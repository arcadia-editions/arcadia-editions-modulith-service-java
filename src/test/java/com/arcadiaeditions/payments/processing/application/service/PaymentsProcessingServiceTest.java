package com.arcadiaeditions.payments.processing.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import com.arcadiaeditions.payments.processing.adapter.driven.jpa.inmemory.*;
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
        // ORD-002: Payment AUTHORIZATION_REQUESTED (a1) already stored: get, not create a second one
        var input = authorizeInput("ORD-002", "CUST-002", "pm_ok", "159.30");

        var payment = paymentsProcessingService.authorizePayment(input);

        assertThat(payment.getId()).isEqualTo(51L);
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.AUTHORIZED);
        assertThat(payment.getAttemptCount()).isEqualTo(1);
        assertThat(payment.getProviderReference()).isEqualTo("STANDIN-ORD-002");
        assertThat(payment.getAuthorizedAt()).isEqualTo(TestClock.NOW);
        assertThat(paymentsOf("ORD-002")).isEqualTo(1);
        assertThat(paymentRepository.findById(51L).orElseThrow().getStatus()).isEqualTo(PaymentStatus.AUTHORIZED);

        var events = context.eventPublisher().getEvents();
        assertThat(events).hasSize(1);
        var event = (PaymentAuthorized) events.get(0);
        assertThat(event.getOrderId()).isEqualTo("ORD-002");
        assertThat(event.getCustomerId()).isEqualTo("CUST-002");
        assertThat(event.getAmount()).isEqualByComparingTo("159.30");
        assertThat(event.getProviderReference()).isEqualTo("STANDIN-ORD-002");
        assertThat(event.getAuthorizedAt()).isEqualTo(TestClock.NOW);
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
        // ORD-008: FAILED a1 below the limit: stays FAILED, attemptCount 2, exactly one PaymentRetried
        var payment = paymentsProcessingService.retryPayment(new RetryPaymentInput().setOrderId("ORD-008"));

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(payment.getAttemptCount()).isEqualTo(2);
        assertThat(paymentsOf("ORD-008")).isEqualTo(1);
        assertThat(paymentRepository.findByOrderId("ORD-008").orElseThrow().getAttemptCount())
                .isEqualTo(2);

        var events = context.eventPublisher().getEvents();
        assertThat(events).hasSize(1);
        assertThat(events.get(0)).isInstanceOf(PaymentRetried.class);
        var event = (PaymentRetried) events.get(0);
        assertThat(event.getOrderId()).isEqualTo("ORD-008");
        assertThat(event.getCustomerId()).isEqualTo("CUST-008");
        assertThat(event.getAttemptCount()).isEqualTo(2);
        assertThat(event.getRetriedAt()).isEqualTo(TestClock.NOW);
    }

    @Test
    void retryPaymentExhaustedTest() {
        // ORD-009: FAILED at attempt 3 (the limit): RETRY_EXHAUSTED, exactly one PaymentRetryExhausted
        var payment = paymentsProcessingService.retryPayment(new RetryPaymentInput().setOrderId("ORD-009"));

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
        // ORD-003 is AUTHORIZED: retry is not allowed from there
        org.assertj.core.api.Assertions.assertThatThrownBy(
                        () -> paymentsProcessingService.retryPayment(new RetryPaymentInput().setOrderId("ORD-003")))
                .isInstanceOf(PaymentTransitions.InvalidStateTransitionException.class);
        assertThat(context.eventPublisher().getEvents()).isEmpty();
    }

    @Test
    void authorizePaymentOnPaymentRetriedTest() {
        // ORD-008 retried (attempt 2): PaymentRetried carries no payment data, the stored Payment is reloaded
        paymentsProcessingService.retryPayment(new RetryPaymentInput().setOrderId("ORD-008"));
        context.eventPublisher().getEvents().clear();

        var payment = paymentsProcessingService.authorizePayment(new AuthorizePaymentInput().setOrderId("ORD-008"));

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.AUTHORIZED);
        assertThat(payment.getAttemptCount()).isEqualTo(2);
        assertThat(payment.getProviderReference()).isEqualTo("STANDIN-ORD-008");
        assertThat(payment.getAuthorizedAt()).isEqualTo(TestClock.NOW);
        assertThat(paymentsOf("ORD-008")).isEqualTo(1);

        var events = context.eventPublisher().getEvents();
        assertThat(events).hasSize(1);
        assertThat(events.get(0)).isInstanceOf(PaymentAuthorized.class);
        var event = (PaymentAuthorized) events.get(0);
        assertThat(event.getOrderId()).isEqualTo("ORD-008");
        assertThat(event.getAmount()).isEqualByComparingTo("59.90");
    }

    @Test
    void authorizePaymentOnPaymentRetriedFailsAgainTest() {
        // pm_fail_always: the second attempt fails again, FAILED with attemptCount 2, one PaymentFailed
        paymentsProcessingService.authorizePayment(authorizeInput("ORD-904", "CUST-904", "pm_fail_always", "10.00"));
        paymentsProcessingService.retryPayment(new RetryPaymentInput().setOrderId("ORD-904"));
        context.eventPublisher().getEvents().clear();

        var payment = paymentsProcessingService.authorizePayment(new AuthorizePaymentInput().setOrderId("ORD-904"));

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(payment.getAttemptCount()).isEqualTo(2);
        var events = context.eventPublisher().getEvents();
        assertThat(events).hasSize(1);
        assertThat(events.get(0)).isInstanceOf(PaymentFailed.class);
        assertThat(((PaymentFailed) events.get(0)).getAttemptCount()).isEqualTo(2);
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
        // ORD-001: Payment already CAPTURED: invalid transition, nothing changes, nothing emitted
        org.assertj.core.api.Assertions.assertThatThrownBy(
                        () -> paymentsProcessingService.capturePayment(new CapturePaymentInput().setOrderId("ORD-001")))
                .isInstanceOf(PaymentTransitions.InvalidStateTransitionException.class);

        assertThat(paymentRepository.findByOrderId("ORD-001").orElseThrow().getStatus())
                .isEqualTo(PaymentStatus.CAPTURED);
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
    void voidPaymentTest() { // TODO: implement this test
    }
}
