package com.arcadiaeditions.payments.processing.application.service;

import com.arcadiaeditions.config.FlowProperties;
import com.arcadiaeditions.payments.processing.application.port.in.*;
import com.arcadiaeditions.payments.processing.application.port.in.dto.*;
import com.arcadiaeditions.payments.processing.application.port.out.event.*;
import com.arcadiaeditions.payments.processing.application.port.out.jpa.*;
import com.arcadiaeditions.payments.processing.application.port.out.provider.PaymentProvider;
import com.arcadiaeditions.payments.processing.application.service.mapper.*;
import com.arcadiaeditions.payments.processing.domain.*;
import com.arcadiaeditions.payments.processing.domain.event.*;
import java.math.*;
import java.time.*;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing [Payment].
 */
@Service
@Transactional
@org.springframework.modulith.NamedInterface("PaymentsProcessingService")
@org.jspecify.annotations.NullMarked
@org.jmolecules.architecture.hexagonal.Application
public class PaymentsProcessingServiceImpl implements PaymentsProcessingService {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final PaymentsProcessingServiceMapper paymentsProcessingServiceMapper =
            PaymentsProcessingServiceMapper.INSTANCE;

    private final PaymentRepository paymentRepository;

    private final EventsMapper eventsMapper = EventsMapper.INSTANCE;
    private final EventPublisher eventPublisher;

    private final PaymentProvider paymentProvider;
    private final FlowProperties flowProperties;
    private final Clock clock;

    /**
     * Constructor.
     */
    public PaymentsProcessingServiceImpl(
            PaymentRepository paymentRepository,
            EventPublisher eventPublisher,
            PaymentProvider paymentProvider,
            FlowProperties flowProperties,
            Clock clock) {
        this.paymentRepository = paymentRepository;
        this.eventPublisher = eventPublisher;
        this.paymentProvider = paymentProvider;
        this.flowProperties = flowProperties;
        this.clock = clock;
    }

    public Payment authorizePayment(AuthorizePaymentInput input) {

        log.debug("Request authorizePayment: {}", input);

        // Two triggers share this command (payments-processing ZDL, API 085b930):
        //  - OrderCreated: the input carries the payment data (amount, paymentMethodId, ...).
        //  - PaymentRetried: the input carries only orderId/customerId (Q-02); the stored payment is used.
        // The data-less input is the retry trigger. An OrderCreated redelivery always carries the data, so it can
        // never restart a FAILED payment.
        boolean retryTrigger = input.getAmount() == null && input.getPaymentMethodId() == null;

        var existing = paymentRepository.findByOrderId(input.getOrderId());
        Payment payment;
        if (existing.isEmpty()) {
            if (retryTrigger) {
                throw new NoSuchElementException("Payment not found for retry: " + input.getOrderId());
            }
            payment = paymentsProcessingServiceMapper.update(new Payment(), input);
            payment.setStatus(PaymentStatus.AUTHORIZATION_REQUESTED);
            payment.setAttemptCount(1);
            payment = paymentRepository.save(payment);
        } else {
            payment = existing.get();
            if (retryTrigger && payment.getStatus() == PaymentStatus.FAILED) {
                // retryPayment already incremented attemptCount and left the payment FAILED; start the new attempt
                payment.setStatus(PaymentStatus.AUTHORIZATION_REQUESTED);
                payment = paymentRepository.save(payment);
            } else {
                // redelivery of OrderCreated or PaymentRetried, or any other state: nothing to do
                return payment;
            }
        }

        var result = paymentProvider.authorize(
                payment.getOrderId(),
                payment.getAttemptCount(),
                payment.getAmount(),
                payment.getCurrency(),
                payment.getPaymentMethodId());
        var now = Instant.now(clock);
        switch (result) {
            case PaymentProvider.Authorized authorized -> {
                payment.setStatus(PaymentStatus.AUTHORIZED);
                payment.setProviderReference(authorized.providerReference());
                payment.setAuthorizedAt(now);
                payment = paymentRepository.save(payment);
                eventPublisher.onPaymentAuthorized(eventsMapper.asPaymentAuthorized(payment));
            }
            case PaymentProvider.Declined declined -> {
                payment.setStatus(PaymentStatus.DECLINED);
                payment.setDeclinedAt(now);
                payment.setFailureReason(declined.reason());
                payment = paymentRepository.save(payment);
                eventPublisher.onPaymentDeclined(eventsMapper.asPaymentDeclined(payment));
            }
            case PaymentProvider.Failed failed -> {
                payment.setStatus(PaymentStatus.FAILED);
                payment.setFailedAt(now);
                payment.setFailureReason(failed.reason());
                payment = paymentRepository.save(payment);
                eventPublisher.onPaymentFailed(eventsMapper.asPaymentFailed(payment));
            }
        }
        return payment;
    }

    public Payment retryPayment(RetryPaymentInput input) {

        log.debug("Request retryPayment: {}", input);

        var payment = paymentRepository
                .findByOrderId(input.getOrderId())
                .orElseThrow(() -> new NoSuchElementException("Payment not found: " + input.getOrderId()));
        if (payment.getStatus() != PaymentStatus.FAILED) {
            // stale or redelivered PaymentFailed (R-02)
            return payment;
        }
        PaymentTransitions.ensureCanRetryPayment(payment);
        if (payment.getAttemptCount() >= flowProperties.payments().maxAttempts()) {
            payment.setStatus(PaymentStatus.RETRY_EXHAUSTED);
            payment = paymentRepository.save(payment);
            eventPublisher.onPaymentRetryExhausted(eventsMapper.asPaymentRetryExhausted(payment, Instant.now(clock)));
            return payment;
        }
        // committed model (API 085b930): retryPayment keeps the payment FAILED; authorizePayment, triggered by
        // PaymentRetried, moves it to AUTHORIZATION_REQUESTED
        payment.setAttemptCount(payment.getAttemptCount() + 1);
        payment = paymentRepository.save(payment);
        eventPublisher.onPaymentRetried(eventsMapper.asPaymentRetried(payment, Instant.now(clock)));
        return payment;
    }

    public Payment capturePayment(CapturePaymentInput input) {

        log.debug("Request capturePayment: {}", input);

        var payment = paymentRepository
                .findByOrderId(input.getOrderId())
                .orElseThrow(() -> new NoSuchElementException("Payment not found: " + input.getOrderId()));
        if (payment.getStatus() == PaymentStatus.CAPTURED || payment.getStatus() == PaymentStatus.CAPTURE_FAILED) {
            // redelivery
            return payment;
        }
        PaymentTransitions.ensureCanCapturePayment(payment);
        var result =
                paymentProvider.capture(payment.getProviderReference(), payment.getAmount(), payment.getCurrency());
        switch (result) {
            case PaymentProvider.Captured captured -> {
                payment.setStatus(PaymentStatus.CAPTURED);
                payment.setCapturedAt(Instant.now(clock));
                payment = paymentRepository.save(payment);
                eventPublisher.onPaymentCaptured(eventsMapper.asPaymentCaptured(payment));
            }
            case PaymentProvider.CaptureFailed captureFailed -> {
                // no money moved; the authorization stays at the provider until voidPayment releases it
                payment.setStatus(PaymentStatus.CAPTURE_FAILED);
                payment.setFailureReason(captureFailed.reason());
                payment = paymentRepository.save(payment);
                eventPublisher.onPaymentCaptureFailed(eventsMapper.asPaymentCaptureFailed(payment));
            }
        }
        return payment;
    }

    public Payment voidPayment(VoidPaymentInput input) {

        log.debug("Request voidPayment: {}", input);

        var payment = paymentRepository
                .findByOrderId(input.getOrderId())
                .orElseThrow(() -> new NoSuchElementException("Payment not found: " + input.getOrderId()));
        if (payment.getStatus() == PaymentStatus.VOIDED) {
            // redelivery
            return payment;
        }
        PaymentTransitions.ensureCanVoidPayment(payment);
        // only an authorized payment holds something at the provider
        if (payment.getStatus() == PaymentStatus.AUTHORIZED || payment.getStatus() == PaymentStatus.CAPTURE_FAILED) {
            paymentProvider.voidAuthorization(payment.getProviderReference());
        }
        payment.setStatus(PaymentStatus.VOIDED);
        payment.setVoidedAt(Instant.now(clock));
        payment.setFailureReason(input.getReason());
        payment = paymentRepository.save(payment);
        eventPublisher.onPaymentVoided(eventsMapper.asPaymentVoided(payment));
        return payment;
    }
}
