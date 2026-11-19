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
            org.springframework.transaction.PlatformTransactionManager transactionManager,
            com.arcadiaeditions.config.FlowProperties flowProperties) {
        this.flowProperties = flowProperties;
        this.paymentRepository = paymentRepository;
        this.eventPublisher = eventPublisher;
        this.paymentProvider = paymentProvider;
        this.clock = clock;
        this.newTransaction = new org.springframework.transaction.support.TransactionTemplate(transactionManager);
        this.newTransaction.setPropagationBehavior(
                org.springframework.transaction.TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    /** Commits AUTHORIZATION_REQUESTED apart from the caller's transaction, before the processor call. */
    private final org.springframework.transaction.support.TransactionTemplate newTransaction;

    /** Runs each processor call, so the caller can stop waiting after the processor timeout. */
    private final java.util.concurrent.ExecutorService processorCalls =
            java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor();

    /** Stops the processor-call threads with the application. */
    @jakarta.annotation.PreDestroy
    public void shutdownProcessorCalls() {
        processorCalls.shutdownNow();
    }

    /**
     * One processor call, abandoned after the processor timeout (ZFL header: 30 s): it then answers FAILED
     * processor_timeout; a call that throws answers FAILED processor_error (P-10 A).
     */
    private PaymentProvider.AuthorizationResult callProcessor(
            java.util.concurrent.Callable<PaymentProvider.AuthorizationResult> call) {
        var timeout = flowProperties.getPayments().getProcessorTimeout();
        var future = processorCalls.submit(call);
        try {
            return future.get(timeout.toMillis(), java.util.concurrent.TimeUnit.MILLISECONDS);
        } catch (java.util.concurrent.TimeoutException e) {
            future.cancel(true);
            log.warn("payment processor did not answer within {}", timeout);
            return PaymentProvider.AuthorizationResult.failed(PaymentProvider.PROCESSOR_TIMEOUT);
        } catch (java.util.concurrent.ExecutionException e) {
            // a processor that throws is a technical failure too: FAILED + PaymentFailed, not a rollback
            log.warn("payment processor call failed", e.getCause() != null ? e.getCause() : e);
            return PaymentProvider.AuthorizationResult.failed(PaymentProvider.PROCESSOR_ERROR);
        } catch (InterruptedException e) {
            future.cancel(true);
            Thread.currentThread().interrupt();
            throw new IllegalStateException("interrupted waiting for the payment processor", e);
        }
    }

    public Payment authorizePayment(AuthorizePaymentInput input) {

        log.debug("Request authorizePayment: {}", input);

        // AUTHORIZATION_REQUESTED is committed in its own transaction before the processor call, so a call in flight
        // counts as started (getPaymentStatus); the outcome is saved afterwards, in the caller's transaction
        var noOp = new boolean[] {false};
        var payment = newTransaction.execute(status -> {
            // one Payment per order: get-or-create by orderId
            var existing = paymentRepository.findByOrderId(input.getOrderId());
            if (existing.isPresent()) {
                // redelivery: a Payment exists (a call in flight or resolved); nothing changes, nothing is emitted
                log.debug(
                        "authorizePayment: payment of order {} already {}",
                        input.getOrderId(),
                        existing.get().getStatus());
                noOp[0] = true;
                return existing.get();
            }
            var requested = paymentsProcessingServiceMapper.update(new Payment(), input);
            requested.setAttemptCount(1);
            requested.setStatus(PaymentStatus.AUTHORIZATION_REQUESTED);
            return paymentRepository.save(requested);
        });
        if (noOp[0]) {
            return payment;
        }

        return authorizeRequested(payment);
    }

    /**
     * The processor call of a Payment committed AUTHORIZATION_REQUESTED, and its outcome saved (in the caller's
     * transaction) with its event: AUTHORIZED, DECLINED or FAILED.
     */
    private Payment authorizeRequested(Payment payment) {
        var request = new PaymentProvider.AuthorizationRequest(
                payment.getOrderId(),
                payment.getPaymentMethodId(),
                payment.getAmount(),
                payment.getCurrency(),
                payment.getAttemptCount());
        var result = callProcessor(() -> paymentProvider.authorize(request));

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
                // technical failure: eligible for retry
                payment.setStatus(PaymentStatus.FAILED);
                payment.setFailedAt(Instant.now(clock));
                payment.setFailureReason(result.reason());
                payment = paymentRepository.save(payment);
                eventPublisher.onPaymentFailed(eventsMapper.asPaymentFailed(payment));
            }
        }
        return payment;
    }

    public Optional<Payment> retryPayment(RetryPaymentInput input) {

        log.debug("Request retryPayment: {}", input);

        var found = paymentRepository.findByOrderId(input.getOrderId());
        if (found.isEmpty()) {
            // P-12 A: no Payment for the order; nothing to retry, no event
            log.debug("retryPayment: no payment for order {}", input.getOrderId());
            return Optional.empty();
        }
        var payment = found.get();
        if (payment.getStatus() != PaymentStatus.FAILED
                || !Objects.equals(input.getAttemptCount(), payment.getAttemptCount())) {
            // redelivery or stale PaymentFailed: the payment moved on or failed another attempt; no event
            log.debug(
                    "retryPayment no-op, payment {} at attempt {}, input attempt {}: {}",
                    payment.getStatus(),
                    payment.getAttemptCount(),
                    input.getAttemptCount(),
                    input.getOrderId());
            return Optional.of(payment);
        }
        PaymentTransitions.ensureCanRetryPayment(payment);

        if (payment.getAttemptCount() >= flowProperties.getPayments().getMaxAttempts()) {
            // retry-exhausted: attempt limit reached, the order moves to cancellation
            payment.setStatus(PaymentStatus.RETRY_EXHAUSTED);
            payment = paymentRepository.save(payment);
            eventPublisher.onPaymentRetryExhausted(eventsMapper
                    .asPaymentRetryExhausted(payment)
                    .setReason(payment.getFailureReason())
                    .setExhaustedAt(Instant.now(clock)));
            return Optional.of(payment);
        }
        // retried: stays FAILED; attemptCount is incremented by reauthorizePayment, not here
        payment = paymentRepository.save(payment);
        eventPublisher.onPaymentRetried(
                eventsMapper.asPaymentRetried(payment).setRetriedAt(Instant.now(clock)));
        return Optional.of(payment);
    }

    public Payment capturePayment(CapturePaymentInput input) {

        log.debug("Request capturePayment: {}", input);

        // the payment is resolved by orderId (Payment.orderId is unique)
        var payment = paymentRepository.findByOrderId(input.getOrderId()).orElseThrow();
        if (payment.getStatus() == PaymentStatus.CAPTURED
                || payment.getStatus() == PaymentStatus.CAPTURE_FAILED
                || payment.getStatus() == PaymentStatus.VOIDED) {
            // redelivery: already captured, capture failed or voided; no provider call, no event
            log.debug("capturePayment redelivery, payment already {}: {}", payment.getStatus(), input.getOrderId());
            return payment;
        }
        PaymentTransitions.ensureCanCapturePayment(payment);

        var result = paymentProvider.capture(new PaymentProvider.CaptureRequest(
                payment.getOrderId(),
                payment.getPaymentMethodId(),
                payment.getProviderReference(),
                payment.getAmount(),
                payment.getCurrency()));

        if (!result.captured()) {
            // capture failed: the authorization is to be voided (PaymentCaptureFailed listener)
            payment.setStatus(PaymentStatus.CAPTURE_FAILED);
            payment.setFailedAt(Instant.now(clock));
            payment.setFailureReason(result.reason());
            payment = paymentRepository.save(payment);
            eventPublisher.onPaymentCaptureFailed(
                    eventsMapper.asPaymentCaptureFailed(payment).setReason(payment.getFailureReason()));
            return payment;
        }
        payment.setStatus(PaymentStatus.CAPTURED);
        payment.setCapturedAt(Instant.now(clock));
        payment = paymentRepository.save(payment);
        eventPublisher.onPaymentCaptured(eventsMapper.asPaymentCaptured(payment));
        return payment;
    }

    // the ZDL makes voidPayment return Payment? (0.10.x): the body below is the baseline behavior
    public Optional<Payment> voidPayment(VoidPaymentInput input) {
        return Optional.ofNullable(voidPaymentBaseline(input));
    }

    private Payment voidPaymentBaseline(VoidPaymentInput input) {

        log.debug("Request voidPayment: {}", input);

        // the payment is resolved by orderId (Payment.orderId is unique)
        var payment = paymentRepository.findByOrderId(input.getOrderId()).orElseThrow();
        if (payment.getStatus() == PaymentStatus.VOIDED) {
            // redelivery: already voided; nothing changes, nothing is emitted
            log.debug("voidPayment: payment of order {} already VOIDED", input.getOrderId());
            return payment;
        }
        PaymentTransitions.ensureCanVoidPayment(payment);
        var result = paymentProvider.voidAuthorization(new PaymentProvider.VoidRequest(
                payment.getOrderId(), payment.getProviderReference(), input.getReason()));
        if (!result.voided()) {
            throw new IllegalStateException("provider did not void the authorization: " + result.reason());
        }
        payment.setStatus(PaymentStatus.VOIDED);
        payment.setVoidedAt(Instant.now(clock));
        payment.setFailureReason(input.getReason());
        payment = paymentRepository.save(payment);
        eventPublisher.onPaymentVoided(eventsMapper.asPaymentVoided(payment).setReason(input.getReason()));
        return payment;
    }

    public Optional<Payment> reauthorizePayment(ReauthorizePaymentInput input) {

        log.debug("Request reauthorizePayment: {}", input);

        // every read and the AUTHORIZATION_REQUESTED save run in transactions of their own, so the caller's
        // persistence context never holds a copy older than the one committed before the processor call (JPA @Version)
        var found = newTransaction.execute(status -> paymentRepository.findByOrderId(input.getOrderId()));
        if (found == null || found.isEmpty()) {
            // P-12 A: no Payment for the order; nothing to reauthorize, no event
            log.debug("reauthorizePayment: no payment for order {}", input.getOrderId());
            return Optional.empty();
        }
        if (!canReauthorize(found.get(), input)) {
            return found;
        }

        // ZFL header: backoff before the next attempt (5 s before attempt 2, 15 s before attempt 3)
        waitBackoffBefore(input.getAttemptCount() + 1);

        // P-11 A: FAILED -> AUTHORIZATION_REQUESTED with attemptCount + 1, committed before the processor call;
        // checked again after the wait, against the Payment loaded in this same block
        var noOp = new boolean[] {false};
        var payment = newTransaction.execute(status -> {
            var current = paymentRepository.findByOrderId(input.getOrderId()).orElseThrow();
            if (!canReauthorize(current, input)) {
                noOp[0] = true;
                return current;
            }
            current.setAttemptCount(current.getAttemptCount() + 1);
            current.setStatus(PaymentStatus.AUTHORIZATION_REQUESTED);
            return paymentRepository.save(current);
        });
        if (noOp[0]) {
            return Optional.of(payment);
        }
        return Optional.of(authorizeRequested(payment));
    }

    /**
     * Guarded here, not with PaymentTransitions.ensureCanReauthorizePayment (it checks AUTHORIZATION_REQUESTED, the
     * from of the last step): only FAILED at input.attemptCount, below the attempt limit; anything else is a
     * redelivered or stale PaymentRetried, a no-op with no event.
     */
    private boolean canReauthorize(Payment payment, ReauthorizePaymentInput input) {
        if (payment.getStatus() == PaymentStatus.FAILED
                && Objects.equals(input.getAttemptCount(), payment.getAttemptCount())
                && payment.getAttemptCount() < flowProperties.getPayments().getMaxAttempts()) {
            return true;
        }
        log.debug(
                "reauthorizePayment no-op, payment {} at attempt {}, input attempt {}: {}",
                payment.getStatus(),
                payment.getAttemptCount(),
                input.getAttemptCount(),
                input.getOrderId());
        return false;
    }

    /** Waits the backoff before this attempt (arcadia.payments.retry-backoff: element 0 before attempt 2). */
    private void waitBackoffBefore(int attempt) {
        var backoff = flowProperties.getPayments().getRetryBackoff();
        if (attempt < 2 || backoff == null || backoff.isEmpty()) {
            return;
        }
        var delay = backoff.get(Math.min(attempt - 2, backoff.size() - 1));
        if (delay == null || delay.isZero() || delay.isNegative()) {
            return;
        }
        try {
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("interrupted waiting the payment retry backoff", e);
        }
    }

    // baseline/place-order-flow-3 compile stub: implemented by a task of docs/tasks.md
    public GetPaymentStatusResult getPaymentStatus(GetPaymentStatusInput input) {
        throw new UnsupportedOperationException("getPaymentStatus: " + input.getOrderId());
    }
}
