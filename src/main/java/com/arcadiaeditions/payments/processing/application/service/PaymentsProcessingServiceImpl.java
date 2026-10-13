package com.arcadiaeditions.payments.processing.application.service;

import com.arcadiaeditions.payments.processing.application.port.in.*;
import com.arcadiaeditions.payments.processing.application.port.in.dto.*;
import com.arcadiaeditions.payments.processing.application.port.out.event.*;
import com.arcadiaeditions.payments.processing.application.port.out.jpa.*;
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

    /**
     * Constructor.
     */
    public PaymentsProcessingServiceImpl(PaymentRepository paymentRepository, EventPublisher eventPublisher) {
        this.paymentRepository = paymentRepository;
        this.eventPublisher = eventPublisher;
    }

    public Payment authorizePayment(AuthorizePaymentInput input) {

        log.debug("Request authorizePayment: {}", input);

        var payment = paymentsProcessingServiceMapper.update(new Payment(), input);
        // TODO: implement this method
        // emit events
        var paymentAuthorized = eventsMapper.asPaymentAuthorized(payment);
        eventPublisher.onPaymentAuthorized(paymentAuthorized);
        var paymentDeclined = eventsMapper.asPaymentDeclined(payment);
        eventPublisher.onPaymentDeclined(paymentDeclined);
        var paymentFailed = eventsMapper.asPaymentFailed(payment);
        eventPublisher.onPaymentFailed(paymentFailed);
        return payment;
    }

    public Payment retryPayment(Long id, RetryPaymentInput input) {

        log.debug("Request retryPayment: {} {}", id, input);

        var payment = paymentRepository
                .findById(id)
                .map(existingPayment -> {
                    PaymentTransitions.ensureCanRetryPayment(existingPayment);
                    paymentsProcessingServiceMapper.update(existingPayment, input);
                    existingPayment.setStatus(PaymentStatus.AUTHORIZATION_REQUESTED);
                    return existingPayment;
                })
                .map(paymentRepository::save)
                .orElseThrow();
        // emit events
        var paymentRetried = eventsMapper.asPaymentRetried(payment);
        eventPublisher.onPaymentRetried(paymentRetried);
        var paymentRetryExhausted = eventsMapper.asPaymentRetryExhausted(payment);
        eventPublisher.onPaymentRetryExhausted(paymentRetryExhausted);
        return payment;
    }

    public Payment capturePayment(Long id, CapturePaymentInput input) {

        log.debug("Request capturePayment: {} {}", id, input);

        var payment = paymentRepository
                .findById(id)
                .map(existingPayment -> {
                    PaymentTransitions.ensureCanCapturePayment(existingPayment);
                    paymentsProcessingServiceMapper.update(existingPayment, input);
                    existingPayment.setStatus(PaymentStatus.CAPTURED);
                    return existingPayment;
                })
                .map(paymentRepository::save)
                .orElseThrow();
        // emit events
        var paymentCaptured = eventsMapper.asPaymentCaptured(payment);
        eventPublisher.onPaymentCaptured(paymentCaptured);
        var paymentCaptureFailed = eventsMapper.asPaymentCaptureFailed(payment);
        eventPublisher.onPaymentCaptureFailed(paymentCaptureFailed);
        return payment;
    }

    public Payment voidPayment(Long id, VoidPaymentInput input) {

        log.debug("Request voidPayment: {} {}", id, input);

        var payment = paymentRepository
                .findById(id)
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
