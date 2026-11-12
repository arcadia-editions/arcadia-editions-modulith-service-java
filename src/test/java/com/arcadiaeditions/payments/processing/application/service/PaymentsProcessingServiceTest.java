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
    void retryPaymentTest() { // TODO: implement this test
    }

    @Test
    void capturePaymentTest() { // TODO: implement this test
    }

    @Test
    void voidPaymentTest() { // TODO: implement this test
    }
}
