package com.arcadiaeditions.payments.processing.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import com.arcadiaeditions.payments.processing.adapter.driven.jpa.inmemory.*;
import com.arcadiaeditions.payments.processing.adapter.driven.provider.StandInPaymentProvider;
import com.arcadiaeditions.payments.processing.application.port.driven.provider.PaymentProvider;
import com.arcadiaeditions.payments.processing.application.port.driven.jpa.*;
import com.arcadiaeditions.payments.processing.application.port.driving.*;
import com.arcadiaeditions.payments.processing.application.port.driving.dto.*;
import com.arcadiaeditions.payments.processing.application.service.mapper.*;
import com.arcadiaeditions.payments.processing.config.*;
import com.arcadiaeditions.payments.processing.domain.*;
import com.arcadiaeditions.config.TestClock;
import com.arcadiaeditions.payments.processing.domain.event.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Acceptance Test for PaymentsProcessingService.
 */
class PaymentsProcessingServiceTest {

    private final Logger log = LoggerFactory.getLogger(getClass());

    ServicesInMemoryConfig context = new ServicesInMemoryConfig();
    PaymentsProcessingServiceImpl paymentsProcessingService = context.paymentsProcessingService();

    PaymentRepositoryInMemory paymentRepository = context.paymentRepository();

    @BeforeEach
    void setUp() {
        context.reloadTestData();
    }

    private AuthorizePaymentInput authorizeInput(
            String orderId, String customerId, String paymentMethodId, String amount) {
        return new AuthorizePaymentInput()
                .setOrderId(orderId)
                .setCustomerId(customerId)
                .setCustomerEmail(customerId.toLowerCase() + "@example.com")
                .setAmount(new BigDecimal(amount))
                .setCurrency("EUR")
                .setPaymentMethodId(paymentMethodId);
    }

    private long paymentsOf(String orderId) {
        return paymentRepository.findAll().stream()
                .filter(p -> orderId.equals(p.getOrderId()))
                .count();
    }

    @Test
    void authorizePaymentTest() {
        // OrderCreated for an order with no Payment yet: the Payment is created and authorized (stand-in pm_ok)
        var input = authorizeInput("ORD-900", "CUST-900", "pm_ok", "59.90");

        var payment = paymentsProcessingService.authorizePayment(input);

        assertThat(payment.getId()).isNotNull();
        assertThat(payment.getOrderId()).isEqualTo("ORD-900");
        assertThat(payment.getCustomerId()).isEqualTo("CUST-900");
        assertThat(payment.getCustomerEmail()).isEqualTo("cust-900@example.com");
        assertThat(payment.getPaymentMethodId()).isEqualTo("pm_ok");
        assertThat(payment.getAmount()).isEqualByComparingTo("59.90");
        assertThat(payment.getCurrency()).isEqualTo("EUR");
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.AUTHORIZED);
        assertThat(payment.getAttemptCount()).isEqualTo(1);
        assertThat(payment.getProviderReference()).isEqualTo("STANDIN-ORD-900");
        assertThat(payment.getAuthorizedAt()).isEqualTo(TestClock.NOW);
        assertThat(payment.getDeclinedAt()).isNull();
        assertThat(payment.getFailedAt()).isNull();
        assertThat(payment.getFailureReason()).isNull();
        assertThat(paymentsOf("ORD-900")).isEqualTo(1);

        var events = context.eventPublisher().getEvents();
        assertThat(events).hasSize(1);
        assertThat(events.get(0)).isInstanceOf(PaymentAuthorized.class);
        var event = (PaymentAuthorized) events.get(0);
        assertThat(event.getOrderId()).isEqualTo("ORD-900");
        assertThat(event.getCustomerId()).isEqualTo("CUST-900");
        assertThat(event.getAmount()).isEqualByComparingTo("59.90");
        assertThat(event.getCurrency()).isEqualTo("EUR");
        assertThat(event.getProviderReference()).isEqualTo("STANDIN-ORD-900");
        assertThat(event.getAuthorizedAt()).isEqualTo(TestClock.NOW);
    }

    @Test
    void authorizePaymentExistingAuthorizationRequestedTest() {
        // ORD-002: Payment AUTHORIZATION_REQUESTED (a1) already committed, a processor call in flight: a redelivered
        // OrderCreated is a no-op (ZDL: creation only), no second Payment, no second processor call, no event
        var calls = new java.util.concurrent.atomic.AtomicInteger();
        var service = serviceWith(new StandInPaymentProvider() {
            @Override
            public AuthorizationResult authorize(AuthorizationRequest request) {
                calls.incrementAndGet();
                return super.authorize(request);
            }
        }, Duration.ofSeconds(30));

        var payment = service.authorizePayment(authorizeInput("ORD-002", "CUST-002", "pm_ok", "159.30"));

        assertThat(payment.getId()).isEqualTo(51L);
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.AUTHORIZATION_REQUESTED);
        assertThat(payment.getAttemptCount()).isEqualTo(1);
        assertThat(payment.getProviderReference()).isNull();
        assertThat(payment.getAuthorizedAt()).isNull();
        assertThat(paymentsOf("ORD-002")).isEqualTo(1);
        assertThat(paymentRepository.findById(51L).orElseThrow().getStatus())
                .isEqualTo(PaymentStatus.AUTHORIZATION_REQUESTED);
        assertThat(calls.get()).isZero();
        assertThat(context.eventPublisher().getEvents()).isEmpty();
    }

    /** A service on the context's repository, events, clock and transactions, with this provider and timeout. */
    private PaymentsProcessingServiceImpl serviceWith(PaymentProvider provider, Duration processorTimeout) {
        var flowProperties = new com.arcadiaeditions.config.FlowProperties();
        flowProperties.getPayments().setProcessorTimeout(processorTimeout);
        return serviceWith(paymentRepository, provider, flowProperties);
    }

    /** A service on this repository, the context's events, clock and transactions, with this provider and settings. */
    private PaymentsProcessingServiceImpl serviceWith(
            PaymentRepository repository,
            PaymentProvider provider,
            com.arcadiaeditions.config.FlowProperties flowProperties) {
        var service = new PaymentsProcessingServiceImpl(
                repository,
                context.eventPublisher(),
                provider,
                context.clock,
                context.transactionManager(),
                flowProperties);
        servicesToShutdown.add(service);
        return service;
    }

    private final java.util.List<PaymentsProcessingServiceImpl> servicesToShutdown = new java.util.ArrayList<>();

    @org.junit.jupiter.api.AfterEach
    void shutdownServices() {
        servicesToShutdown.forEach(PaymentsProcessingServiceImpl::shutdownProcessorCalls);
    }

    @Test
    void authorizePaymentCommitsAuthorizationRequestedBeforeProviderCallTest() {
        // OrderCreated: AUTHORIZATION_REQUESTED (attemptCount 1) is committed in its own transaction before the
        // processor is called, so a call in flight counts as started (getPaymentStatus)
        var seenStatus = new java.util.concurrent.atomic.AtomicReference<PaymentStatus>();
        var seenAttempts = new java.util.concurrent.atomic.AtomicReference<Integer>();
        var commitsBeforeCall = new java.util.concurrent.atomic.AtomicInteger(-1);
        var service = serviceWith(new StandInPaymentProvider() {
            @Override
            public AuthorizationResult authorize(AuthorizationRequest request) {
                var stored = paymentRepository.findByOrderId(request.orderId()).orElseThrow();
                seenStatus.set(stored.getStatus());
                seenAttempts.set(stored.getAttemptCount());
                commitsBeforeCall.set(context.transactionManager().commits());
                return super.authorize(request);
            }
        }, Duration.ofSeconds(30));

        var payment = service.authorizePayment(authorizeInput("ORD-905", "CUST-905", "pm_ok", "59.90"));

        assertThat(seenStatus.get()).isEqualTo(PaymentStatus.AUTHORIZATION_REQUESTED);
        assertThat(seenAttempts.get()).isEqualTo(1);
        assertThat(commitsBeforeCall.get()).isEqualTo(1);
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.AUTHORIZED);
        assertThat(payment.getAttemptCount()).isEqualTo(1);
        assertThat(paymentsOf("ORD-905")).isEqualTo(1);
        var events = context.eventPublisher().getEvents();
        assertThat(events).hasSize(1);
        assertThat(events.get(0)).isInstanceOf(PaymentAuthorized.class);
    }

    @Test
    void authorizePaymentProcessorTimeoutTest() {
        // a processor that does not answer within the timeout (a short one stands for the 30 s of the ZFL):
        // FAILED processor_timeout, exactly one PaymentFailed
        var service = serviceWith(new StandInPaymentProvider() {
            @Override
            public AuthorizationResult authorize(AuthorizationRequest request) {
                try {
                    Thread.sleep(Duration.ofSeconds(10));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return super.authorize(request);
            }
        }, Duration.ofMillis(200));

        var started = System.nanoTime();
        var payment = service.authorizePayment(authorizeInput("ORD-906", "CUST-906", "pm_ok", "59.90"));

        assertThat(Duration.ofNanos(System.nanoTime() - started)).isLessThan(Duration.ofSeconds(5));
        assertFailedWith(payment, "ORD-906", "CUST-906", PaymentProvider.PROCESSOR_TIMEOUT);
    }

    @Test
    void authorizePaymentProcessorErrorTest() {
        // a processor call that throws (P-10 A): FAILED processor_error, exactly one PaymentFailed, not left
        // AUTHORIZATION_REQUESTED for good
        var service = serviceWith(new StandInPaymentProvider() {
            @Override
            public AuthorizationResult authorize(AuthorizationRequest request) {
                throw new IllegalStateException("processor exploded");
            }
        }, Duration.ofSeconds(30));

        var payment = service.authorizePayment(authorizeInput("ORD-907", "CUST-907", "pm_ok", "59.90"));

        assertFailedWith(payment, "ORD-907", "CUST-907", PaymentProvider.PROCESSOR_ERROR);
    }

    @Test
    void shutdownProcessorCallsInterruptsCallsInFlightTest() throws Exception {
        // @PreDestroy: the processor-call threads stop with the application
        var callStarted = new java.util.concurrent.CountDownLatch(1);
        var interrupted = new java.util.concurrent.CountDownLatch(1);
        var service = serviceWith(new StandInPaymentProvider() {
            @Override
            public AuthorizationResult authorize(AuthorizationRequest request) {
                callStarted.countDown();
                try {
                    Thread.sleep(Duration.ofSeconds(20));
                } catch (InterruptedException e) {
                    interrupted.countDown();
                    Thread.currentThread().interrupt();
                }
                return super.authorize(request);
            }
        }, Duration.ofSeconds(30));
        var caller = Thread.ofVirtual()
                .start(() -> service.authorizePayment(authorizeInput("ORD-908", "CUST-908", "pm_ok", "59.90")));
        assertThat(callStarted.await(5, java.util.concurrent.TimeUnit.SECONDS)).isTrue();

        service.shutdownProcessorCalls();

        assertThat(interrupted.await(5, java.util.concurrent.TimeUnit.SECONDS)).isTrue();
        caller.join(Duration.ofSeconds(5));
    }

    private void assertFailedWith(Payment payment, String orderId, String customerId, String reason) {
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(payment.getAttemptCount()).isEqualTo(1);
        assertThat(payment.getFailureReason()).isEqualTo(reason);
        assertThat(payment.getFailedAt()).isEqualTo(TestClock.NOW);
        assertThat(payment.getAuthorizedAt()).isNull();
        assertThat(paymentsOf(orderId)).isEqualTo(1);
        assertThat(paymentRepository.findByOrderId(orderId).orElseThrow().getStatus())
                .isEqualTo(PaymentStatus.FAILED);

        var events = context.eventPublisher().getEvents();
        assertThat(events).hasSize(1);
        assertThat(events.get(0)).isInstanceOf(PaymentFailed.class);
        var event = (PaymentFailed) events.get(0);
        assertThat(event.getOrderId()).isEqualTo(orderId);
        assertThat(event.getCustomerId()).isEqualTo(customerId);
        assertThat(event.getAttemptCount()).isEqualTo(1);
        assertThat(event.getReason()).isEqualTo(reason);
        assertThat(event.getFailedAt()).isEqualTo(TestClock.NOW);
    }

    @Test
    void authorizePaymentRedeliveryAlreadyAuthorizedTest() {
        // ORD-003: Payment already AUTHORIZED; OrderCreated redelivered changes nothing and emits nothing
        var input = authorizeInput("ORD-003", "CUST-003", "pm_ok", "59.90");

        var payment = paymentsProcessingService.authorizePayment(input);

        assertThat(payment.getId()).isEqualTo(101L);
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.AUTHORIZED);
        assertThat(payment.getAttemptCount()).isEqualTo(1);
        assertThat(payment.getProviderReference()).isEqualTo("STANDIN-ORD-003");
        assertThat(payment.getAuthorizedAt()).isEqualTo(Instant.parse("2026-10-14T09:50:08Z"));
        assertThat(paymentsOf("ORD-003")).isEqualTo(1);
        assertThat(context.eventPublisher().getEvents()).isEmpty();
    }

    @Test
    void authorizePaymentTwiceSameOrderTest() {
        // a second delivery of the same OrderCreated after the first authorized it: no second Payment, no second event
        var input = authorizeInput("ORD-901", "CUST-901", "pm_ok", "10.00");
        paymentsProcessingService.authorizePayment(input);

        paymentsProcessingService.authorizePayment(input);

        assertThat(paymentsOf("ORD-901")).isEqualTo(1);
        assertThat(context.eventPublisher().getEvents()).hasSize(1);
    }

    @Test
    void authorizePaymentDeclinedTest() {
        // pm_decline: processor rejects the card: DECLINED, no retry, exactly one PaymentDeclined
        var payment = paymentsProcessingService.authorizePayment(authorizeInput("ORD-902", "CUST-902", "pm_decline", "59.90"));

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.DECLINED);
        assertThat(payment.getDeclinedAt()).isEqualTo(TestClock.NOW);
        assertThat(payment.getFailureReason()).isEqualTo("card_declined");
        assertThat(payment.getAttemptCount()).isEqualTo(1);
        assertThat(payment.getAuthorizedAt()).isNull();
        assertThat(payment.getFailedAt()).isNull();
        assertThat(paymentsOf("ORD-902")).isEqualTo(1);
        assertThat(paymentRepository.findByOrderId("ORD-902").orElseThrow().getStatus())
                .isEqualTo(PaymentStatus.DECLINED);

        var events = context.eventPublisher().getEvents();
        assertThat(events).hasSize(1);
        assertThat(events.get(0)).isInstanceOf(PaymentDeclined.class);
        var event = (PaymentDeclined) events.get(0);
        assertThat(event.getOrderId()).isEqualTo("ORD-902");
        assertThat(event.getCustomerId()).isEqualTo("CUST-902");
        assertThat(event.getAmount()).isEqualByComparingTo("59.90");
        assertThat(event.getCurrency()).isEqualTo("EUR");
        assertThat(event.getReason()).isEqualTo("card_declined");
        assertThat(event.getDeclinedAt()).isEqualTo(TestClock.NOW);
    }

    @Test
    void authorizePaymentRedeliveryAlreadyDeclinedTest() {
        // ORD-010: already DECLINED; redelivery changes nothing, no retry, nothing emitted
        var payment = paymentsProcessingService.authorizePayment(authorizeInput("ORD-010", "CUST-010", "pm_decline", "59.90"));

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.DECLINED);
        assertThat(payment.getAttemptCount()).isEqualTo(1);
        assertThat(payment.getDeclinedAt()).isEqualTo(Instant.parse("2026-10-14T09:50:15Z"));
        assertThat(paymentsOf("ORD-010")).isEqualTo(1);
        assertThat(context.eventPublisher().getEvents()).isEmpty();
    }

    @Test
    void authorizePaymentFailedTest() {
        // pm_fail_once on attempt 1: technical failure: FAILED, attemptCount 1, exactly one PaymentFailed
        assertFailedFirstAttempt("ORD-903", "CUST-903", "pm_fail_once");
    }

    @Test
    void authorizePaymentFailedAlwaysTest() {
        assertFailedFirstAttempt("ORD-904", "CUST-904", "pm_fail_always");
    }

    private void assertFailedFirstAttempt(String orderId, String customerId, String pm) {
        var payment = paymentsProcessingService.authorizePayment(authorizeInput(orderId, customerId, pm, "59.90"));

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(payment.getAttemptCount()).isEqualTo(1);
        assertThat(payment.getFailureReason()).isEqualTo("processor_unavailable");
        assertThat(payment.getFailedAt()).isEqualTo(TestClock.NOW);
        assertThat(payment.getAuthorizedAt()).isNull();
        assertThat(payment.getDeclinedAt()).isNull();
        assertThat(paymentsOf(orderId)).isEqualTo(1);
        assertThat(paymentRepository.findByOrderId(orderId).orElseThrow().getStatus())
                .isEqualTo(PaymentStatus.FAILED);

        var events = context.eventPublisher().getEvents();
        assertThat(events).hasSize(1);
        assertThat(events.get(0)).isInstanceOf(PaymentFailed.class);
        var event = (PaymentFailed) events.get(0);
        assertThat(event.getOrderId()).isEqualTo(orderId);
        assertThat(event.getCustomerId()).isEqualTo(customerId);
        assertThat(event.getAttemptCount()).isEqualTo(1);
        assertThat(event.getReason()).isEqualTo("processor_unavailable");
        assertThat(event.getFailedAt()).isEqualTo(TestClock.NOW);
    }

    @Test
    void authorizePaymentRedeliveryAlreadyFailedTest() {
        // ORD-008: already FAILED (a1); redelivery of OrderCreated changes nothing, emits nothing (retry is T-24)
        var payment = paymentsProcessingService.authorizePayment(authorizeInput("ORD-008", "CUST-008", "pm_fail_once", "59.90"));

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(payment.getAttemptCount()).isEqualTo(1);
        assertThat(payment.getFailedAt()).isEqualTo(Instant.parse("2026-10-14T09:50:13Z"));
        assertThat(paymentsOf("ORD-008")).isEqualTo(1);
        assertThat(context.eventPublisher().getEvents()).isEmpty();
    }

    @Test
    void retryPaymentTest() {
        // ORD-008: FAILED a1 below the limit: stays FAILED, attemptCount unchanged (1), exactly one PaymentRetried
        var payment = paymentsProcessingService.retryPayment(new RetryPaymentInput().setOrderId("ORD-008").setAttemptCount(1)).orElseThrow();

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(payment.getAttemptCount()).isEqualTo(1);
        assertThat(paymentsOf("ORD-008")).isEqualTo(1);
        assertThat(paymentRepository.findByOrderId("ORD-008").orElseThrow().getAttemptCount())
                .isEqualTo(1);

        var events = context.eventPublisher().getEvents();
        assertThat(events).hasSize(1);
        assertThat(events.get(0)).isInstanceOf(PaymentRetried.class);
        var event = (PaymentRetried) events.get(0);
        assertThat(event.getOrderId()).isEqualTo("ORD-008");
        assertThat(event.getCustomerId()).isEqualTo("CUST-008");
        assertThat(event.getAttemptCount()).isEqualTo(1);
        assertThat(event.getRetriedAt()).isEqualTo(TestClock.NOW);
    }

    @Test
    void retryPaymentExhaustedTest() {
        // ORD-009: FAILED at attempt 3 (the limit): RETRY_EXHAUSTED, exactly one PaymentRetryExhausted
        var payment = paymentsProcessingService.retryPayment(new RetryPaymentInput().setOrderId("ORD-009").setAttemptCount(3)).orElseThrow();

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.RETRY_EXHAUSTED);
        assertThat(payment.getAttemptCount()).isEqualTo(3);
        assertThat(paymentsOf("ORD-009")).isEqualTo(1);
        assertThat(paymentRepository.findByOrderId("ORD-009").orElseThrow().getStatus())
                .isEqualTo(PaymentStatus.RETRY_EXHAUSTED);

        var events = context.eventPublisher().getEvents();
        assertThat(events).hasSize(1);
        assertThat(events.get(0)).isInstanceOf(PaymentRetryExhausted.class);
        var event = (PaymentRetryExhausted) events.get(0);
        assertThat(event.getOrderId()).isEqualTo("ORD-009");
        assertThat(event.getCustomerId()).isEqualTo("CUST-009");
        assertThat(event.getAttemptCount()).isEqualTo(3);
        assertThat(event.getReason()).isEqualTo("processor_unavailable");
        assertThat(event.getExhaustedAt()).isEqualTo(TestClock.NOW);
    }

    @Test
    void retryPaymentNotFailedTest() {
        // ORD-003 is AUTHORIZED: a redelivered PaymentFailed is a no-op, payment unchanged, no event
        var payment = paymentsProcessingService.retryPayment(new RetryPaymentInput().setOrderId("ORD-003").setAttemptCount(1)).orElseThrow();

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.AUTHORIZED);
        assertThat(paymentRepository.findByOrderId("ORD-003").orElseThrow().getStatus())
                .isEqualTo(PaymentStatus.AUTHORIZED);
        assertThat(context.eventPublisher().getEvents()).isEmpty();
    }

    @Test
    void retryPaymentNoPaymentTest() {
        // P-12 A: no Payment for the order: Optional.empty(), nothing created, no event
        assertThat(paymentsProcessingService.retryPayment(new RetryPaymentInput().setOrderId("ORD-905").setAttemptCount(1)))
                .isEmpty();
        assertThat(paymentsOf("ORD-905")).isZero();
        assertThat(context.eventPublisher().getEvents()).isEmpty();
    }

    @Test
    void retryPaymentStaleAttemptTest() {
        // ORD-008 is at attempt 1: a PaymentFailed of another attempt is a no-op, unchanged, no event
        var payment = paymentsProcessingService.retryPayment(new RetryPaymentInput().setOrderId("ORD-008").setAttemptCount(2)).orElseThrow();

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(payment.getAttemptCount()).isEqualTo(1);
        assertThat(paymentRepository.findByOrderId("ORD-008").orElseThrow().getAttemptCount()).isEqualTo(1);
        assertThat(context.eventPublisher().getEvents()).isEmpty();
    }

    @Test
    void retryPaymentStaleAttemptAtLimitTest() {
        // ORD-009 is at attempt 3: a stale PaymentFailed (attempt 2) must not exhaust it
        var payment = paymentsProcessingService.retryPayment(new RetryPaymentInput().setOrderId("ORD-009").setAttemptCount(2)).orElseThrow();

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(context.eventPublisher().getEvents()).isEmpty();
    }

    @Test
    void retryPaymentRetryExhaustedRedeliveryTest() {
        // ORD-011 is RETRY_EXHAUSTED: redelivery is a no-op, no second PaymentRetryExhausted
        var payment = paymentsProcessingService.retryPayment(new RetryPaymentInput().setOrderId("ORD-011").setAttemptCount(3)).orElseThrow();

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.RETRY_EXHAUSTED);
        assertThat(context.eventPublisher().getEvents()).isEmpty();
    }

    @Test
    void authorizePaymentIsCreationOnlyOnFailedPaymentTest() {
        // ORD-008 FAILED a1: authorizePayment never reuses it (T-54, ZDL 119-128); retries are reauthorizePayment
        var payment = paymentsProcessingService.authorizePayment(new AuthorizePaymentInput().setOrderId("ORD-008"));

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(payment.getAttemptCount()).isEqualTo(1);
        assertThat(paymentsOf("ORD-008")).isEqualTo(1);
        assertThat(paymentRepository.findByOrderId("ORD-008").orElseThrow().getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(context.eventPublisher().getEvents()).isEmpty();
    }

    @Test
    void capturePaymentTest() {
        // ORD-004: Payment AUTHORIZED -> CAPTURED through the provider, PaymentCaptured with customerEmail
        var payment = paymentsProcessingService.capturePayment(new CapturePaymentInput().setOrderId("ORD-004"));

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.CAPTURED);
        assertThat(payment.getCapturedAt()).isEqualTo(TestClock.NOW);
        assertThat(payment.getProviderReference()).isEqualTo("STANDIN-ORD-004");
        assertThat(paymentsOf("ORD-004")).isEqualTo(1);
        assertThat(paymentRepository.findByOrderId("ORD-004").orElseThrow().getStatus())
                .isEqualTo(PaymentStatus.CAPTURED);

        var events = context.eventPublisher().getEvents();
        assertThat(events).hasSize(1);
        assertThat(events.get(0)).isInstanceOf(PaymentCaptured.class);
        var event = (PaymentCaptured) events.get(0);
        assertThat(event.getOrderId()).isEqualTo("ORD-004");
        assertThat(event.getCustomerId()).isEqualTo("CUST-004");
        assertThat(event.getCustomerEmail()).isEqualTo("customer004@example.com");
        assertThat(event.getAmount()).isEqualByComparingTo("99.40");
        assertThat(event.getCurrency()).isEqualTo("EUR");
        assertThat(event.getProviderReference()).isEqualTo("STANDIN-ORD-004");
        assertThat(event.getCapturedAt()).isEqualTo(TestClock.NOW);
    }

    @Test
    void capturePaymentAlreadyCapturedTest() {
        // ORD-001: Payment already CAPTURED; redelivery is a no-op, unchanged, provider not called, nothing emitted
        var before = paymentRepository.findByOrderId("ORD-001").orElseThrow().getCapturedAt();

        var payment = paymentsProcessingService.capturePayment(new CapturePaymentInput().setOrderId("ORD-001"));

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.CAPTURED);
        assertThat(payment.getCapturedAt()).isEqualTo(before);
        assertThat(context.eventPublisher().getEvents()).isEmpty();
    }

    @Test
    void capturePaymentRedeliveryAlreadyCaptureFailedTest() {
        // ORD-012: CAPTURE_FAILED; redelivered FulfillmentScheduled is a no-op, unchanged, nothing emitted
        var before = paymentRepository.findByOrderId("ORD-012").orElseThrow();
        var failedAt = before.getFailedAt();

        var payment = paymentsProcessingService.capturePayment(new CapturePaymentInput().setOrderId("ORD-012"));

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.CAPTURE_FAILED);
        assertThat(payment.getFailedAt()).isEqualTo(failedAt);
        assertThat(payment.getFailureReason()).isEqualTo("capture_rejected");
        assertThat(paymentsOf("ORD-012")).isEqualTo(1);
        assertThat(context.eventPublisher().getEvents()).isEmpty();
    }

    @Test
    void capturePaymentRedeliveryAlreadyVoidedTest() {
        // ORD-016: VOIDED; redelivered FulfillmentScheduled is a no-op, unchanged, nothing emitted
        var voidedAt = paymentRepository.findByOrderId("ORD-016").orElseThrow().getVoidedAt();

        var payment = paymentsProcessingService.capturePayment(new CapturePaymentInput().setOrderId("ORD-016"));

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.VOIDED);
        assertThat(payment.getVoidedAt()).isEqualTo(voidedAt);
        assertThat(paymentsOf("ORD-016")).isEqualTo(1);
        assertThat(context.eventPublisher().getEvents()).isEmpty();
    }

    @Test
    void capturePaymentOtherStatesStillThrowTest() {
        // ORD-002 AUTHORIZATION_REQUESTED, ORD-008 FAILED, ORD-010 DECLINED, ORD-011 RETRY_EXHAUSTED
        for (var orderId : java.util.List.of("ORD-002", "ORD-008", "ORD-010", "ORD-011")) {
            org.assertj.core.api.Assertions.assertThatThrownBy(() ->
                            paymentsProcessingService.capturePayment(new CapturePaymentInput().setOrderId(orderId)))
                    .as(orderId)
                    .isInstanceOf(RuntimeException.class);
        }
        assertThat(context.eventPublisher().getEvents()).isEmpty();
    }

    @Test
    void capturePaymentNotFoundTest() {
        org.assertj.core.api.Assertions.assertThatThrownBy(
                        () -> paymentsProcessingService.capturePayment(new CapturePaymentInput().setOrderId("ORD-999")))
                .isInstanceOf(java.util.NoSuchElementException.class);
        assertThat(context.eventPublisher().getEvents()).isEmpty();
    }

    @Test
    void voidPaymentTest() {
        // ORD-007: fulfillment failed, Payment still AUTHORIZED -> VOIDED through the provider, one PaymentVoided
        var payment = paymentsProcessingService.voidPayment(
                new VoidPaymentInput().setOrderId("ORD-007").setReason("carrier_unavailable")).orElseThrow();

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.VOIDED);
        assertThat(payment.getVoidedAt()).isEqualTo(TestClock.NOW);
        assertThat(payment.getFailureReason()).isEqualTo("carrier_unavailable");
        assertThat(payment.getProviderReference()).isEqualTo("STANDIN-ORD-007");
        assertThat(paymentsOf("ORD-007")).isEqualTo(1);
        assertThat(paymentRepository.findByOrderId("ORD-007").orElseThrow().getStatus())
                .isEqualTo(PaymentStatus.VOIDED);

        var events = context.eventPublisher().getEvents();
        assertThat(events).hasSize(1);
        assertThat(events.get(0)).isInstanceOf(PaymentVoided.class);
        var event = (PaymentVoided) events.get(0);
        assertThat(event.getOrderId()).isEqualTo("ORD-007");
        assertThat(event.getCustomerId()).isEqualTo("CUST-007");
        assertThat(event.getProviderReference()).isEqualTo("STANDIN-ORD-007");
        assertThat(event.getReason()).isEqualTo("carrier_unavailable");
        assertThat(event.getVoidedAt()).isEqualTo(TestClock.NOW);
    }

    @Test
    void capturePaymentFailedTest() {
        // ORD-006: pm_capture_fail -> CAPTURE_FAILED with failedAt and failureReason, exactly one PaymentCaptureFailed
        var payment = paymentsProcessingService.capturePayment(new CapturePaymentInput().setOrderId("ORD-006"));

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.CAPTURE_FAILED);
        assertThat(payment.getFailedAt()).isEqualTo(TestClock.NOW);
        assertThat(payment.getFailureReason()).isEqualTo("capture_rejected");
        assertThat(payment.getCapturedAt()).isNull();
        assertThat(paymentRepository.findByOrderId("ORD-006").orElseThrow().getStatus())
                .isEqualTo(PaymentStatus.CAPTURE_FAILED);

        var events = context.eventPublisher().getEvents();
        assertThat(events).hasSize(1);
        assertThat(events.get(0)).isInstanceOf(PaymentCaptureFailed.class);
        var event = (PaymentCaptureFailed) events.get(0);
        assertThat(event.getOrderId()).isEqualTo("ORD-006");
        assertThat(event.getCustomerId()).isEqualTo("CUST-006");
        assertThat(event.getReason()).isEqualTo("capture_rejected");
        assertThat(event.getFailedAt()).isEqualTo(TestClock.NOW);
    }

    @Test
    void voidPaymentFromCaptureFailedTest() {
        // ORD-012: CAPTURE_FAILED, void pending -> VOIDED, one PaymentVoided
        var payment = paymentsProcessingService.voidPayment(
                new VoidPaymentInput().setOrderId("ORD-012").setReason("capture_rejected")).orElseThrow();

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.VOIDED);
        assertThat(payment.getVoidedAt()).isEqualTo(TestClock.NOW);
        var events = context.eventPublisher().getEvents();
        assertThat(events).hasSize(1);
        assertThat(events.get(0)).isInstanceOf(PaymentVoided.class);
        var event = (PaymentVoided) events.get(0);
        assertThat(event.getOrderId()).isEqualTo("ORD-012");
        assertThat(event.getReason()).isEqualTo("capture_rejected");
        assertThat(event.getVoidedAt()).isEqualTo(TestClock.NOW);
    }

    @Test
    void voidPaymentFromAuthorizationRequestedTest() {
        var payment = paymentsProcessingService.voidPayment(
                new VoidPaymentInput().setOrderId("ORD-002").setReason("x")).orElseThrow();

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.VOIDED);
        assertThat(context.eventPublisher().getEvents()).hasSize(1);
        assertThat(context.eventPublisher().getEvents().get(0)).isInstanceOf(PaymentVoided.class);
    }

    @Test
    void voidPaymentFromFailedTest() {
        var payment = paymentsProcessingService.voidPayment(
                new VoidPaymentInput().setOrderId("ORD-008").setReason("x")).orElseThrow();

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.VOIDED);
        assertThat(context.eventPublisher().getEvents()).hasSize(1);
        assertThat(context.eventPublisher().getEvents().get(0)).isInstanceOf(PaymentVoided.class);
    }

    @Test
    void voidPaymentAlreadyVoidedTest() {
        // ORD-016: already VOIDED; redelivery is a no-op, nothing changes, nothing emitted
        var payment = paymentsProcessingService.voidPayment(
                new VoidPaymentInput().setOrderId("ORD-016").setReason("carrier_unavailable")).orElseThrow();

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.VOIDED);
        assertThat(payment.getVoidedAt()).isEqualTo(Instant.parse("2026-10-14T09:50:56Z"));
        assertThat(paymentsOf("ORD-016")).isEqualTo(1);
        assertThat(context.eventPublisher().getEvents()).isEmpty();
    }

    @Test
    void voidPaymentTwiceTest() {
        var input = new VoidPaymentInput().setOrderId("ORD-007").setReason("carrier_unavailable");
        paymentsProcessingService.voidPayment(input);

        paymentsProcessingService.voidPayment(input);

        assertThat(context.eventPublisher().getEvents()).hasSize(1);
    }

    @Test
    void voidPaymentNotFoundTest() {
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> paymentsProcessingService.voidPayment(
                        new VoidPaymentInput().setOrderId("ORD-999").setReason("x")))
                .isInstanceOf(java.util.NoSuchElementException.class);
        assertThat(context.eventPublisher().getEvents()).isEmpty();
    }

    @Test
    void voidPaymentInvalidStateTest() {
        // ORD-010 is DECLINED: no void from there
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> paymentsProcessingService.voidPayment(
                        new VoidPaymentInput().setOrderId("ORD-010").setReason("x")))
                .isInstanceOf(PaymentTransitions.InvalidStateTransitionException.class);
        assertThat(context.eventPublisher().getEvents()).isEmpty();
    }

    // --- reauthorizePayment (T-53): ZDL 130-136, P-11 A, P-12 A ---

    private static ReauthorizePaymentInput reauthorizeInput(String orderId, int attemptCount) {
        return new ReauthorizePaymentInput().setOrderId(orderId).setAttemptCount(attemptCount);
    }

    /** Flow settings with this backoff (a short one stands for the 5 s / 15 s of the ZFL) and processor timeout. */
    private static com.arcadiaeditions.config.FlowProperties flowWith(Duration processorTimeout, Duration... backoff) {
        var flowProperties = new com.arcadiaeditions.config.FlowProperties();
        flowProperties.getPayments().setProcessorTimeout(processorTimeout);
        flowProperties.getPayments().setRetryBackoff(java.util.List.of(backoff));
        return flowProperties;
    }

    /** The stand-in, counting its authorization calls. */
    private static class CountingProvider extends StandInPaymentProvider {
        final java.util.concurrent.atomic.AtomicInteger calls = new java.util.concurrent.atomic.AtomicInteger();

        @Override
        public AuthorizationResult authorize(AuthorizationRequest request) {
            calls.incrementAndGet();
            return super.authorize(request);
        }
    }

    private PaymentsProcessingServiceImpl reauthorizeService(PaymentProvider provider) {
        return serviceWith(paymentRepository, provider, flowWith(Duration.ofSeconds(30), Duration.ZERO, Duration.ZERO));
    }

    @Test
    void reauthorizePaymentTest() {
        // ORD-008: FAILED a1, PaymentRetried a1: AUTHORIZATION_REQUESTED a2 committed before the processor call,
        // pm_fail_once succeeds on attempt 2: AUTHORIZED, exactly one PaymentAuthorized
        var seenStatus = new java.util.concurrent.atomic.AtomicReference<PaymentStatus>();
        var seenAttempts = new java.util.concurrent.atomic.AtomicReference<Integer>();
        var requestAttempt = new java.util.concurrent.atomic.AtomicInteger();
        var commitsBeforeCall = new java.util.concurrent.atomic.AtomicInteger(-1);
        var service = reauthorizeService(new StandInPaymentProvider() {
            @Override
            public AuthorizationResult authorize(AuthorizationRequest request) {
                var stored = paymentRepository.findByOrderId(request.orderId()).orElseThrow();
                seenStatus.set(stored.getStatus());
                seenAttempts.set(stored.getAttemptCount());
                requestAttempt.set(request.attempt());
                commitsBeforeCall.set(context.transactionManager().commits());
                return super.authorize(request);
            }
        });

        var payment = service.reauthorizePayment(reauthorizeInput("ORD-008", 1)).orElseThrow();

        assertThat(seenStatus.get()).isEqualTo(PaymentStatus.AUTHORIZATION_REQUESTED);
        assertThat(seenAttempts.get()).isEqualTo(2);
        assertThat(requestAttempt.get()).isEqualTo(2);
        assertThat(commitsBeforeCall.get()).isPositive();
        assertThat(payment.getId()).isEqualTo(351L);
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.AUTHORIZED);
        assertThat(payment.getAttemptCount()).isEqualTo(2);
        assertThat(payment.getProviderReference()).isEqualTo("STANDIN-ORD-008");
        assertThat(payment.getAuthorizedAt()).isEqualTo(TestClock.NOW);
        assertThat(paymentsOf("ORD-008")).isEqualTo(1);
        assertThat(paymentRepository.findByOrderId("ORD-008").orElseThrow().getStatus())
                .isEqualTo(PaymentStatus.AUTHORIZED);

        var events = context.eventPublisher().getEvents();
        assertThat(events).hasSize(1);
        assertThat(events.get(0)).isInstanceOf(PaymentAuthorized.class);
        var event = (PaymentAuthorized) events.get(0);
        assertThat(event.getOrderId()).isEqualTo("ORD-008");
        assertThat(event.getCustomerId()).isEqualTo("CUST-008");
        assertThat(event.getAmount()).isEqualByComparingTo("59.90");
        assertThat(event.getCurrency()).isEqualTo("EUR");
        assertThat(event.getProviderReference()).isEqualTo("STANDIN-ORD-008");
        assertThat(event.getAuthorizedAt()).isEqualTo(TestClock.NOW);
    }

    @Test
    void reauthorizePaymentFailedAgainTest() {
        // ORD-008 a1, the processor fails again: FAILED a2, exactly one PaymentFailed with attemptCount 2
        var service = reauthorizeService(new StandInPaymentProvider() {
            @Override
            public AuthorizationResult authorize(AuthorizationRequest request) {
                return AuthorizationResult.failed(PROCESSOR_UNAVAILABLE);
            }
        });

        var payment = service.reauthorizePayment(reauthorizeInput("ORD-008", 1)).orElseThrow();

        assertReauthorizeFailedWith(payment, PaymentProvider.PROCESSOR_UNAVAILABLE);
    }

    @Test
    void reauthorizePaymentProcessorErrorTest() {
        // a processor call that throws (P-10 A): FAILED processor_error a2, one PaymentFailed, not left
        // AUTHORIZATION_REQUESTED
        var service = reauthorizeService(new StandInPaymentProvider() {
            @Override
            public AuthorizationResult authorize(AuthorizationRequest request) {
                throw new IllegalStateException("processor exploded");
            }
        });

        var payment = service.reauthorizePayment(reauthorizeInput("ORD-008", 1)).orElseThrow();

        assertReauthorizeFailedWith(payment, PaymentProvider.PROCESSOR_ERROR);
    }

    @Test
    void reauthorizePaymentProcessorTimeoutTest() {
        // a processor that does not answer within the timeout (a short one stands for the 30 s): FAILED processor_timeout
        var service = serviceWith(
                paymentRepository,
                new StandInPaymentProvider() {
                    @Override
                    public AuthorizationResult authorize(AuthorizationRequest request) {
                        try {
                            Thread.sleep(Duration.ofSeconds(10));
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                        return super.authorize(request);
                    }
                },
                flowWith(Duration.ofMillis(200), Duration.ZERO, Duration.ZERO));

        var started = System.nanoTime();
        var payment = service.reauthorizePayment(reauthorizeInput("ORD-008", 1)).orElseThrow();

        assertThat(Duration.ofNanos(System.nanoTime() - started)).isLessThan(Duration.ofSeconds(5));
        assertReauthorizeFailedWith(payment, PaymentProvider.PROCESSOR_TIMEOUT);
    }

    private void assertReauthorizeFailedWith(Payment payment, String reason) {
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(payment.getAttemptCount()).isEqualTo(2);
        assertThat(payment.getFailureReason()).isEqualTo(reason);
        assertThat(payment.getFailedAt()).isEqualTo(TestClock.NOW);
        assertThat(payment.getAuthorizedAt()).isNull();
        assertThat(paymentsOf("ORD-008")).isEqualTo(1);
        var stored = paymentRepository.findByOrderId("ORD-008").orElseThrow();
        assertThat(stored.getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(stored.getAttemptCount()).isEqualTo(2);

        var events = context.eventPublisher().getEvents();
        assertThat(events).hasSize(1);
        assertThat(events.get(0)).isInstanceOf(PaymentFailed.class);
        var event = (PaymentFailed) events.get(0);
        assertThat(event.getOrderId()).isEqualTo("ORD-008");
        assertThat(event.getCustomerId()).isEqualTo("CUST-008");
        assertThat(event.getAttemptCount()).isEqualTo(2);
        assertThat(event.getReason()).isEqualTo(reason);
        assertThat(event.getFailedAt()).isEqualTo(TestClock.NOW);
    }

    @Test
    void reauthorizePaymentDeclinedTest() {
        // the processor declines attempt 2: DECLINED a2, exactly one PaymentDeclined
        var service = reauthorizeService(new StandInPaymentProvider() {
            @Override
            public AuthorizationResult authorize(AuthorizationRequest request) {
                return AuthorizationResult.declined(CARD_DECLINED);
            }
        });

        var payment = service.reauthorizePayment(reauthorizeInput("ORD-008", 1)).orElseThrow();

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.DECLINED);
        assertThat(payment.getAttemptCount()).isEqualTo(2);
        assertThat(payment.getDeclinedAt()).isEqualTo(TestClock.NOW);
        assertThat(payment.getFailureReason()).isEqualTo("card_declined");
        assertThat(paymentRepository.findByOrderId("ORD-008").orElseThrow().getStatus())
                .isEqualTo(PaymentStatus.DECLINED);
        var events = context.eventPublisher().getEvents();
        assertThat(events).hasSize(1);
        assertThat(events.get(0)).isInstanceOf(PaymentDeclined.class);
        var event = (PaymentDeclined) events.get(0);
        assertThat(event.getOrderId()).isEqualTo("ORD-008");
        assertThat(event.getReason()).isEqualTo("card_declined");
        assertThat(event.getDeclinedAt()).isEqualTo(TestClock.NOW);
    }

    @Test
    void reauthorizePaymentNoPaymentTest() {
        // P-12 A: no Payment for the order: Optional.empty(), nothing created, no processor call, no event
        var provider = new CountingProvider();
        var service = reauthorizeService(provider);

        assertThat(service.reauthorizePayment(reauthorizeInput("ORD-905", 1))).isEmpty();

        assertThat(paymentsOf("ORD-905")).isZero();
        assertThat(provider.calls.get()).isZero();
        assertThat(context.eventPublisher().getEvents()).isEmpty();
    }

    @Test
    void reauthorizePaymentStaleAttemptTest() {
        // ORD-008 at a1 gets PaymentRetried a2, ORD-009 at a3 gets a2 (stale): no-op, unchanged, no call, no event,
        // and no backoff waited
        var provider = new CountingProvider();
        var service = serviceWith(
                paymentRepository,
                provider,
                flowWith(Duration.ofSeconds(30), Duration.ofSeconds(10), Duration.ofSeconds(10)));

        var started = System.nanoTime();
        var ord008 = service.reauthorizePayment(reauthorizeInput("ORD-008", 2)).orElseThrow();
        var ord009 = service.reauthorizePayment(reauthorizeInput("ORD-009", 2)).orElseThrow();

        assertThat(Duration.ofNanos(System.nanoTime() - started)).isLessThan(Duration.ofSeconds(5));
        assertThat(ord008.getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(ord008.getAttemptCount()).isEqualTo(1);
        assertThat(ord009.getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(ord009.getAttemptCount()).isEqualTo(3);
        assertThat(paymentRepository.findByOrderId("ORD-008").orElseThrow().getAttemptCount()).isEqualTo(1);
        assertThat(paymentRepository.findByOrderId("ORD-009").orElseThrow().getAttemptCount()).isEqualTo(3);
        assertThat(provider.calls.get()).isZero();
        assertThat(context.eventPublisher().getEvents()).isEmpty();
    }

    @Test
    void reauthorizePaymentOtherStatesNoOpTest() {
        // ORD-002 AUTHORIZATION_REQUESTED, ORD-003 AUTHORIZED, ORD-010 DECLINED, ORD-011 RETRY_EXHAUSTED,
        // ORD-001 CAPTURED, ORD-016 VOIDED: a redelivered PaymentRetried is a no-op, no call, no event
        var provider = new CountingProvider();
        var service = reauthorizeService(provider);
        for (var orderId : java.util.List.of("ORD-002", "ORD-003", "ORD-010", "ORD-011", "ORD-001", "ORD-016")) {
            var before = paymentRepository.findByOrderId(orderId).orElseThrow();
            var status = before.getStatus();
            var attempts = before.getAttemptCount();

            var payment = service.reauthorizePayment(reauthorizeInput(orderId, attempts)).orElseThrow();

            assertThat(payment.getStatus()).as(orderId).isEqualTo(status);
            assertThat(payment.getAttemptCount()).as(orderId).isEqualTo(attempts);
            assertThat(paymentRepository.findByOrderId(orderId).orElseThrow().getStatus())
                    .as(orderId)
                    .isEqualTo(status);
        }
        assertThat(provider.calls.get()).isZero();
        assertThat(context.eventPublisher().getEvents()).isEmpty();
    }

    @Test
    void reauthorizePaymentTwiceTest() {
        // the same PaymentRetried a1 delivered twice: the second finds AUTHORIZED a2, no second call, no second event
        var provider = new CountingProvider();
        var service = reauthorizeService(provider);
        var input = reauthorizeInput("ORD-008", 1);

        service.reauthorizePayment(input);
        var payment = service.reauthorizePayment(input).orElseThrow();

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.AUTHORIZED);
        assertThat(payment.getAttemptCount()).isEqualTo(2);
        assertThat(provider.calls.get()).isEqualTo(1);
        assertThat(context.eventPublisher().getEvents()).hasSize(1);
    }

    @Test
    void reauthorizePaymentWaitsBackoffBeforeAttemptTwoTest() {
        // backoff before attempt 2 (5 s in the ZFL; 300 ms here), waited before the processor call
        var elapsedAtCall = new java.util.concurrent.atomic.AtomicLong(-1);
        var started = System.nanoTime();
        var service = serviceWith(
                paymentRepository,
                new StandInPaymentProvider() {
                    @Override
                    public AuthorizationResult authorize(AuthorizationRequest request) {
                        elapsedAtCall.set(System.nanoTime() - started);
                        return super.authorize(request);
                    }
                },
                flowWith(Duration.ofSeconds(30), Duration.ofMillis(300), Duration.ofSeconds(10)));

        var payment = service.reauthorizePayment(reauthorizeInput("ORD-008", 1)).orElseThrow();

        assertThat(Duration.ofNanos(elapsedAtCall.get())).isGreaterThanOrEqualTo(Duration.ofMillis(300));
        assertThat(Duration.ofNanos(System.nanoTime() - started)).isLessThan(Duration.ofSeconds(5));
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.AUTHORIZED);
    }

    @Test
    void reauthorizePaymentWaitsBackoffBeforeAttemptThreeTest() {
        // backoff before attempt 3 (15 s in the ZFL; 600 ms here), not the one of attempt 2
        paymentRepository.findByOrderId("ORD-008").orElseThrow().setAttemptCount(2);
        var service = serviceWith(
                paymentRepository,
                new StandInPaymentProvider(),
                flowWith(Duration.ofSeconds(30), Duration.ofMillis(10), Duration.ofMillis(600)));

        var started = System.nanoTime();
        var payment = service.reauthorizePayment(reauthorizeInput("ORD-008", 2)).orElseThrow();

        assertThat(Duration.ofNanos(System.nanoTime() - started)).isGreaterThanOrEqualTo(Duration.ofMillis(600));
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.AUTHORIZED);
        assertThat(payment.getAttemptCount()).isEqualTo(3);
    }

    @Test
    void flowPropertiesDefaultBackoffTest() {
        // ZFL header: 5 s before attempt 2, 15 s before attempt 3
        assertThat(new com.arcadiaeditions.config.FlowProperties().getPayments().getRetryBackoff())
                .containsExactly(Duration.ofSeconds(5), Duration.ofSeconds(15));
    }

    @Test
    void reauthorizePaymentAttemptsTwoAndThreeWithVersionedRepositoryTest() {
        // JPA @Version semantics: a Payment loaded before the REQUIRES_NEW block and saved over the one that block
        // committed fails with an optimistic-lock error; attempts 2 and 3 must not
        var repository = new VersionedPaymentRepository();
        repository.saveAll(paymentRepository.findAll());
        var service = serviceWith(
                repository,
                new StandInPaymentProvider() {
                    @Override
                    public AuthorizationResult authorize(AuthorizationRequest request) {
                        return AuthorizationResult.failed(PROCESSOR_UNAVAILABLE);
                    }
                },
                flowWith(Duration.ofSeconds(30), Duration.ZERO, Duration.ZERO));
        var versionBefore = repository.findByOrderId("ORD-008").orElseThrow().getVersion();

        var attempt2 = service.reauthorizePayment(reauthorizeInput("ORD-008", 1)).orElseThrow();
        service.retryPayment(new RetryPaymentInput().setOrderId("ORD-008").setAttemptCount(2));
        var attempt3 = service.reauthorizePayment(reauthorizeInput("ORD-008", 2)).orElseThrow();

        assertThat(attempt2.getAttemptCount()).isEqualTo(2);
        assertThat(attempt3.getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(attempt3.getAttemptCount()).isEqualTo(3);
        var stored = repository.findByOrderId("ORD-008").orElseThrow();
        assertThat(stored.getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(stored.getAttemptCount()).isEqualTo(3);
        assertThat(stored.getVersion()).isGreaterThan(versionBefore);
        var failed = context.eventPublisher().getEvents().stream()
                .filter(PaymentFailed.class::isInstance)
                .map(e -> ((PaymentFailed) e).getAttemptCount())
                .toList();
        assertThat(failed).containsExactly(2, 3);
    }

    @Test
    void versionedRepositoryRejectsStaleSaveTest() {
        // the test double itself: saving a copy loaded before another save fails, as a JPA merge does
        var repository = new VersionedPaymentRepository();
        repository.saveAll(paymentRepository.findAll());
        var stale = repository.findByOrderId("ORD-008").orElseThrow();
        repository.save(repository.findByOrderId("ORD-008").orElseThrow().setAttemptCount(2));

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> repository.save(stale))
                .isInstanceOf(org.springframework.orm.ObjectOptimisticLockingFailureException.class);
    }

    /**
     * In-memory repository with JPA @Version semantics: reads return detached copies, a save of a copy whose version
     * is not the stored one fails, and every save increments the version.
     */
    static class VersionedPaymentRepository extends PaymentRepositoryInMemory {

        @Override
        public Optional<Payment> findByOrderId(String orderId) {
            return super.findByOrderId(orderId).map(VersionedPaymentRepository::copyOf);
        }

        @Override
        public Optional<Payment> findById(Long id) {
            return super.findById(id).map(VersionedPaymentRepository::copyOf);
        }

        @Override
        @SuppressWarnings("unchecked")
        public <S extends Payment> S save(S entity) {
            var stored = entity.getId() == null ? null : getEntities().get(entity.getId());
            var copy = copyOf(entity);
            if (stored == null) {
                copy.setVersion(entity.getVersion() == null ? 0 : entity.getVersion());
            } else {
                if (!java.util.Objects.equals(stored.getVersion(), entity.getVersion())) {
                    throw new org.springframework.orm.ObjectOptimisticLockingFailureException(
                            Payment.class, entity.getId());
                }
                copy.setVersion(stored.getVersion() + 1);
            }
            super.save(copy);
            return (S) copyOf(copy);
        }

        static Payment copyOf(Payment source) {
            var copy = new Payment();
            for (var field : org.apache.commons.lang3.reflect.FieldUtils.getAllFieldsList(Payment.class)) {
                if (java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
                    continue;
                }
                try {
                    org.apache.commons.lang3.reflect.FieldUtils.writeField(
                            field, copy, org.apache.commons.lang3.reflect.FieldUtils.readField(field, source, true), true);
                } catch (IllegalAccessException e) {
                    throw new IllegalStateException(e);
                }
            }
            return copy;
        }
    }
}
