package com.arcadiaeditions.payments.processing.application.service;

import com.arcadiaeditions.payments.processing.application.port.driven.event.*;
import com.arcadiaeditions.payments.processing.application.port.driven.jpa.*;
import com.arcadiaeditions.payments.processing.application.port.driven.provider.PaymentProvider;
import com.arcadiaeditions.payments.processing.application.port.driving.*;
import com.arcadiaeditions.payments.processing.application.port.driving.dto.*;
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

    private final Clock clock;

    private final com.arcadiaeditions.config.FlowProperties flowProperties;

    /**
     * Constructor.
     */
    public PaymentsProcessingServiceImpl(
            PaymentRepository paymentRepository,
            EventPublisher eventPublisher,
            PaymentProvider paymentProvider,
            Clock clock,
            com.arcadiaeditions.config.FlowProperties flowProperties) {
        this.flowProperties = flowProperties;
        this.paymentRepository = paymentRepository;
        this.eventPublisher = eventPublisher;
        this.paymentProvider = paymentProvider;
        this.clock = clock;
    }

    public Payment authorizePayment(AuthorizePaymentInput input) {

        log.debug("Request authorizePayment: {}", input);

        // one Payment per order: get-or-create by orderId
        var existing = paymentRepository.findByOrderId(input.getOrderId());
        // PaymentRetried carries no payment data: its input has only the orderId, the stored Payment is reloaded
        boolean retried = input.getPaymentMethodId() == null
                && existing.isPresent()
                && existing.get().getStatus() == PaymentStatus.FAILED;
        if (existing.isPresent() && !retried && existing.get().getStatus() != PaymentStatus.AUTHORIZATION_REQUESTED) {
            // redelivery: the authorization is already resolved; nothing changes, nothing is emitted
            log.debug(
                    "authorizePayment: payment of order {} already {}",
                    input.getOrderId(),
                    existing.get().getStatus());
            return existing.get();
        }
        var payment = existing.orElseGet(() -> {
            var created = paymentsProcessingServiceMapper.update(new Payment(), input);
            created.setStatus(PaymentStatus.AUTHORIZATION_REQUESTED);
            created.setAttemptCount(1);
            return created;
        });

        var result = paymentProvider.authorize(new PaymentProvider.AuthorizationRequest(
                payment.getOrderId(),
                payment.getPaymentMethodId(),
                payment.getAmount(),
                payment.getCurrency(),
                payment.getAttemptCount()));

        switch (result.outcome()) {
            case AUTHORIZED -> {
                payment.setStatus(PaymentStatus.AUTHORIZED);
                payment.setProviderReference(result.providerReference());
                payment.setAuthorizedAt(Instant.now(clock));
                payment = paymentRepository.save(payment);
                eventPublisher.onPaymentAuthorized(eventsMapper.asPaymentAuthorized(payment));
            }
            case DECLINED -> {
                // no retry: the customer needs to act
                payment.setStatus(PaymentStatus.DECLINED);
                payment.setDeclinedAt(Instant.now(clock));
                payment.setFailureReason(result.reason());
                payment = paymentRepository.save(payment);
                eventPublisher.onPaymentDeclined(eventsMapper.asPaymentDeclined(payment));
            }
            case FAILED -> {
                // technical failure: eligible for retry (T-24)
                payment.setStatus(PaymentStatus.FAILED);
                payment.setFailedAt(Instant.now(clock));
                payment.setFailureReason(result.reason());
                payment = paymentRepository.save(payment);
                eventPublisher.onPaymentFailed(eventsMapper.asPaymentFailed(payment));
            }
        }
        return payment;
    }

    public Payment retryPayment(RetryPaymentInput input) {

        log.debug("Request retryPayment: {}", input);

        var payment = paymentRepository.findByOrderId(input.getOrderId()).orElseThrow();
        PaymentTransitions.ensureCanRetryPayment(payment);

        if (payment.getAttemptCount() >= flowProperties.getPayments().getMaxAttempts()) {
            // TODO T-26: retry-exhausted branch: RETRY_EXHAUSTED, emit PaymentRetryExhausted
            throw new UnsupportedOperationException("retryPayment retry-exhausted branch not implemented yet");
        }
        // retried: stays FAILED, one more attempt
        payment.setAttemptCount(payment.getAttemptCount() + 1);
        payment = paymentRepository.save(payment);
        eventPublisher.onPaymentRetried(
                eventsMapper.asPaymentRetried(payment).setRetriedAt(Instant.now(clock)));
        return payment;
    }

    public Payment capturePayment(CapturePaymentInput input) {

        log.debug("Request capturePayment: {}", input);

        // the payment is resolved by orderId (Payment.orderId is unique)
        var payment = paymentRepository.findByOrderId(input.getOrderId()).orElseThrow();
        PaymentTransitions.ensureCanCapturePayment(payment);

        var result = paymentProvider.capture(new PaymentProvider.CaptureRequest(
                payment.getOrderId(),
                payment.getPaymentMethodId(),
                payment.getProviderReference(),
                payment.getAmount(),
                payment.getCurrency()));

        if (!result.captured()) {
            // TODO T-33: capture failure: CAPTURE_FAILED with failedAt and failureReason, emit PaymentCaptureFailed
            throw new UnsupportedOperationException("capturePayment failure branch not implemented yet");
        }
        payment.setStatus(PaymentStatus.CAPTURED);
        payment.setCapturedAt(Instant.now(clock));
        payment = paymentRepository.save(payment);
        eventPublisher.onPaymentCaptured(eventsMapper.asPaymentCaptured(payment));
        return payment;
    }

    public Payment voidPayment(VoidPaymentInput input) {

        log.debug("Request voidPayment: {}", input);

        // TODO CUSTOM_REQUIRED: load the Payment this transition applies to, e.g. by a correlation key in the input
        Optional<Payment> foundPayment = Optional.empty();
        var payment = foundPayment
                .map(existingPayment -> {
                    PaymentTransitions.ensureCanVoidPayment(existingPayment);
                    paymentsProcessingServiceMapper.update(existingPayment, input);
                    existingPayment.setStatus(PaymentStatus.VOIDED);
                    return existingPayment;
                })
                .map(paymentRepository::save)
                .orElseThrow();
        // emit events
        var paymentVoided = eventsMapper.asPaymentVoided(payment);
        eventPublisher.onPaymentVoided(paymentVoided);
        return payment;
    }
}
