package com.arcadiaeditions.payments.processing.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.arcadiaeditions.config.FlowProperties;
import com.arcadiaeditions.payments.processing.adapter.out.event.InMemoryEventPublisher;
import com.arcadiaeditions.payments.processing.adapter.out.jpa.inmemory.*;
import com.arcadiaeditions.payments.processing.adapter.out.provider.StandInPaymentProvider;
import com.arcadiaeditions.payments.processing.application.port.in.*;
import com.arcadiaeditions.payments.processing.application.port.in.dto.*;
import com.arcadiaeditions.payments.processing.application.port.out.jpa.*;
import com.arcadiaeditions.payments.processing.application.port.out.provider.PaymentProvider;
import com.arcadiaeditions.payments.processing.application.service.mapper.*;
import com.arcadiaeditions.payments.processing.config.*;
import com.arcadiaeditions.payments.processing.domain.*;
import com.arcadiaeditions.payments.processing.domain.event.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Acceptance Test for PaymentsProcessingService.
 */
class PaymentsProcessingServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-14T10:00:00Z");

    private final Logger log = LoggerFactory.getLogger(getClass());

    ServicesInMemoryConfig context = new ServicesInMemoryConfig();
    PaymentsProcessingServiceImpl paymentsProcessingService = context.paymentsProcessingService();

    PaymentRepositoryInMemory paymentRepository = context.paymentRepository();
    InMemoryEventPublisher eventPublisher = context.eventPublisher();

    @BeforeEach
    void setUp() {
        context.reloadTestData();
    }

    /** What the OrderCreated listener mapping produces: orderId plus the payment data. */
    private AuthorizePaymentInput orderCreatedInput(String orderId, String paymentMethodId) {
        return new AuthorizePaymentInput()
                .setOrderId(orderId)
                .setCustomerId("CUST-NEW")
                .setCustomerEmail("new@example.com")
                .setAmount(new BigDecimal("42.00"))
                .setCurrency("EUR")
                .setPaymentMethodId(paymentMethodId);
    }

    /** What the PaymentRetried listener mapping produces: orderId and customerId, no payment data (Q-02). */
    private AuthorizePaymentInput paymentRetriedInput(String orderId) {
        return new AuthorizePaymentInput().setOrderId(orderId);
    }

    private Payment payment(String orderId) {
        return paymentRepository.findByOrderId(orderId).orElseThrow();
    }

    @Test
    void authorizePaymentTest() {
        var result = paymentsProcessingService.authorizePayment(orderCreatedInput("ORD-NEW", "pm_ok"));

        assertThat(result.getStatus()).isEqualTo(PaymentStatus.AUTHORIZED);
        var saved = payment("ORD-NEW");
        assertThat(saved.getAttemptCount()).isEqualTo(1);
        assertThat(saved.getStatus()).isEqualTo(PaymentStatus.AUTHORIZED);
        assertThat(saved.getProviderReference()).isEqualTo("STANDIN-ORD-NEW");
        assertThat(saved.getAuthorizedAt()).isEqualTo(NOW);
        assertThat(saved.getPaymentMethodId()).isEqualTo("pm_ok");
        assertThat(eventPublisher.getEvents()).hasSize(1).first().isInstanceOf(PaymentAuthorized.class);
        var event = (PaymentAuthorized) eventPublisher.getEvents().get(0);
        assertThat(event.getOrderId()).isEqualTo("ORD-NEW");
        assertThat(event.getProviderReference()).isEqualTo("STANDIN-ORD-NEW");
    }

    @Test
    void authorizePaymentRedeliveryTest() {
        paymentsProcessingService.authorizePayment(orderCreatedInput("ORD-NEW", "pm_ok"));
        eventPublisher.getEvents().clear();
        var count = paymentRepository.count();

        paymentsProcessingService.authorizePayment(orderCreatedInput("ORD-NEW", "pm_ok"));

        assertThat(paymentRepository.count()).isEqualTo(count);
        assertThat(eventPublisher.getEvents()).isEmpty();
        assertThat(payment("ORD-NEW").getAttemptCount()).isEqualTo(1);
    }

    @Test
    void authorizePaymentOrderCreatedRedeliveryOnExistingPaymentOfAnyStatusTest() {
        // ORD-008 is FAILED (attempt 1): an OrderCreated redelivery must not start a retry
        for (var orderId : new String[] {"ORD-001", "ORD-002", "ORD-003", "ORD-008", "ORD-009", "ORD-010", "ORD-011"}) {
            var before = payment(orderId);
            var status = before.getStatus();
            var attempts = before.getAttemptCount();

            paymentsProcessingService.authorizePayment(orderCreatedInput(orderId, "pm_ok"));

            assertThat(payment(orderId).getStatus()).as(orderId).isEqualTo(status);
            assertThat(payment(orderId).getAttemptCount()).as(orderId).isEqualTo(attempts);
        }
        assertThat(eventPublisher.getEvents()).isEmpty();
    }

    @Test
    void authorizePaymentDeclinedTest() {
        paymentsProcessingService.authorizePayment(orderCreatedInput("ORD-NEW", "pm_decline"));

        var saved = payment("ORD-NEW");
        assertThat(saved.getStatus()).isEqualTo(PaymentStatus.DECLINED);
        assertThat(saved.getDeclinedAt()).isEqualTo(NOW);
        assertThat(saved.getFailureReason()).isNotBlank();
        assertThat(eventPublisher.getEvents()).hasSize(1).first().isInstanceOf(PaymentDeclined.class);
        var event = (PaymentDeclined) eventPublisher.getEvents().get(0);
        assertThat(event.getReason()).isNotBlank().isEqualTo(saved.getFailureReason());
        assertThat(event.getDeclinedAt()).isEqualTo(NOW);
    }

    @Test
    void retryPaymentOnDeclinedPaymentIsNoOpTest() {
        paymentsProcessingService.authorizePayment(orderCreatedInput("ORD-NEW", "pm_decline"));
        eventPublisher.getEvents().clear();

        paymentsProcessingService.retryPayment(new RetryPaymentInput().setOrderId("ORD-NEW"));

        assertThat(payment("ORD-NEW").getStatus()).isEqualTo(PaymentStatus.DECLINED);
        assertThat(payment("ORD-NEW").getAttemptCount()).isEqualTo(1);
        assertThat(eventPublisher.getEvents()).isEmpty();
    }

    @Test
    void retryPaymentAtLimitExhaustsRetriesTest() {
        paymentsProcessingService.retryPayment(new RetryPaymentInput().setOrderId("ORD-009"));

        var saved = payment("ORD-009");
        assertThat(saved.getStatus()).isEqualTo(PaymentStatus.RETRY_EXHAUSTED);
        assertThat(saved.getAttemptCount()).isEqualTo(3);
        assertThat(eventPublisher.getEvents()).hasSize(1).first().isInstanceOf(PaymentRetryExhausted.class);
        var event = (PaymentRetryExhausted) eventPublisher.getEvents().get(0);
        assertThat(event.getOrderId()).isEqualTo("ORD-009");
        assertThat(event.getAttemptCount()).isEqualTo(3);
        assertThat(event.getReason()).isNotBlank().isEqualTo(saved.getFailureReason());
        assertThat(event.getExhaustedAt()).isEqualTo(NOW);
    }

    @Test
    void retryPaymentAfterExhaustionIsNoOpTest() {
        paymentsProcessingService.retryPayment(new RetryPaymentInput().setOrderId("ORD-009"));
        eventPublisher.getEvents().clear();

        paymentsProcessingService.retryPayment(new RetryPaymentInput().setOrderId("ORD-009"));

        assertThat(payment("ORD-009").getStatus()).isEqualTo(PaymentStatus.RETRY_EXHAUSTED);
        assertThat(eventPublisher.getEvents()).isEmpty();
    }

    @Test
    void authorizePaymentFailedThenRetryThenAuthorizedTest() {
        paymentsProcessingService.authorizePayment(orderCreatedInput("ORD-NEW", "pm_fail_once"));

        var failed = payment("ORD-NEW");
        assertThat(failed.getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(failed.getAttemptCount()).isEqualTo(1);
        assertThat(failed.getFailedAt()).isEqualTo(NOW);
        assertThat(failed.getFailureReason()).isNotBlank();
        assertThat(eventPublisher.getEvents()).hasSize(1).first().isInstanceOf(PaymentFailed.class);
        assertThat(((PaymentFailed) eventPublisher.getEvents().get(0)).getReason())
                .isEqualTo(failed.getFailureReason());
        eventPublisher.getEvents().clear();

        // PaymentFailed -> retryPayment: counter + 1, stays FAILED (committed model, API 085b930), one PaymentRetried
        paymentsProcessingService.retryPayment(new RetryPaymentInput().setOrderId("ORD-NEW"));

        var retried = payment("ORD-NEW");
        assertThat(retried.getAttemptCount()).isEqualTo(2);
        assertThat(retried.getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(eventPublisher.getEvents()).hasSize(1).first().isInstanceOf(PaymentRetried.class);
        assertThat(((PaymentRetried) eventPublisher.getEvents().get(0)).getRetriedAt())
                .isEqualTo(NOW);
        eventPublisher.getEvents().clear();

        // PaymentRetried -> authorizePayment with only the orderId: uses the stored data and attempt 2
        paymentsProcessingService.authorizePayment(paymentRetriedInput("ORD-NEW"));

        var authorized = payment("ORD-NEW");
        assertThat(authorized.getStatus()).isEqualTo(PaymentStatus.AUTHORIZED);
        assertThat(authorized.getAttemptCount()).isEqualTo(2);
        assertThat(authorized.getPaymentMethodId()).isEqualTo("pm_fail_once");
        assertThat(authorized.getAmount()).isEqualByComparingTo("42.00");
        assertThat(authorized.getProviderReference()).isEqualTo("STANDIN-ORD-NEW");
        assertThat(eventPublisher.getEvents()).hasSize(1).first().isInstanceOf(PaymentAuthorized.class);
    }

    @Test
    void authorizePaymentRetriedOnNonFailedPaymentIsNoOpTest() {
        paymentsProcessingService.authorizePayment(paymentRetriedInput("ORD-003"));

        assertThat(payment("ORD-003").getStatus()).isEqualTo(PaymentStatus.AUTHORIZED);
        assertThat(eventPublisher.getEvents()).isEmpty();
    }

    @Test
    void authorizePaymentRetriedForUnknownOrderTest() {
        assertThatThrownBy(() -> paymentsProcessingService.authorizePayment(paymentRetriedInput("ORD-NOPE")))
                .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void retryPaymentTest() {
        paymentsProcessingService.retryPayment(new RetryPaymentInput().setOrderId("ORD-008"));

        var saved = payment("ORD-008");
        assertThat(saved.getAttemptCount()).isEqualTo(2);
        assertThat(saved.getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(eventPublisher.getEvents()).hasSize(1).first().isInstanceOf(PaymentRetried.class);
    }

    @Test
    void retryPaymentOnNonFailedPaymentIsNoOpTest() {
        paymentsProcessingService.retryPayment(new RetryPaymentInput().setOrderId("ORD-003"));

        assertThat(payment("ORD-003").getStatus()).isEqualTo(PaymentStatus.AUTHORIZED);
        assertThat(payment("ORD-003").getAttemptCount()).isEqualTo(1);
        assertThat(eventPublisher.getEvents()).isEmpty();
    }

    @Test
    void retryPaymentUnknownOrderTest() {
        assertThatThrownBy(() -> paymentsProcessingService.retryPayment(new RetryPaymentInput().setOrderId("ORD-NOPE")))
                .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void capturePaymentTest() {
        paymentsProcessingService.capturePayment(new CapturePaymentInput().setOrderId("ORD-004"));

        var saved = payment("ORD-004");
        assertThat(saved.getStatus()).isEqualTo(PaymentStatus.CAPTURED);
        assertThat(saved.getCapturedAt()).isEqualTo(NOW);
        assertThat(eventPublisher.getEvents()).hasSize(1).first().isInstanceOf(PaymentCaptured.class);
    }

    @Test
    void capturePaymentRedeliveryTest() {
        paymentsProcessingService.capturePayment(new CapturePaymentInput().setOrderId("ORD-001"));
        paymentsProcessingService.capturePayment(new CapturePaymentInput().setOrderId("ORD-012"));

        assertThat(payment("ORD-001").getStatus()).isEqualTo(PaymentStatus.CAPTURED);
        assertThat(payment("ORD-012").getStatus()).isEqualTo(PaymentStatus.CAPTURE_FAILED);
        assertThat(eventPublisher.getEvents()).isEmpty();
    }

    @Test
    void capturePaymentInvalidStateTest() {
        assertThatThrownBy(
                        () -> paymentsProcessingService.capturePayment(new CapturePaymentInput().setOrderId("ORD-002")))
                .isInstanceOf(PaymentTransitions.InvalidStateTransitionException.class);
        assertThat(eventPublisher.getEvents()).isEmpty();
    }

    @Test
    void capturePaymentUnknownOrderTest() {
        assertThatThrownBy(() ->
                        paymentsProcessingService.capturePayment(new CapturePaymentInput().setOrderId("ORD-NOPE")))
                .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void capturePaymentCaptureFailedTest() {
        // ORD-006 is AUTHORIZED with a pm_capture_fail reference (STANDIN-CAPFAIL-ORD-006)
        paymentsProcessingService.capturePayment(new CapturePaymentInput().setOrderId("ORD-006"));

        var saved = payment("ORD-006");
        assertThat(saved.getStatus()).isEqualTo(PaymentStatus.CAPTURE_FAILED);
        assertThat(saved.getFailureReason()).isEqualTo("capture_rejected");
        assertThat(saved.getCapturedAt()).isNull();
        assertThat(eventPublisher.getEvents()).hasSize(1).first().isInstanceOf(PaymentCaptureFailed.class);
        var event = (PaymentCaptureFailed) eventPublisher.getEvents().get(0);
        assertThat(event.getOrderId()).isEqualTo("ORD-006");
        assertThat(event.getReason()).isEqualTo("capture_rejected");
    }

    /** Service wired to a provider that records voidAuthorization calls. */
    private PaymentsProcessingServiceImpl serviceWithRecordingProvider(List<String> voidedReferences) {
        var standIn = new StandInPaymentProvider();
        PaymentProvider recording = new PaymentProvider() {
            public AuthorizeResult authorize(
                    String orderId, int attempt, BigDecimal amount, String currency, String paymentMethodId) {
                return standIn.authorize(orderId, attempt, amount, currency, paymentMethodId);
            }

            public CaptureResult capture(String providerReference, BigDecimal amount, String currency) {
                return standIn.capture(providerReference, amount, currency);
            }

            public VoidResult voidAuthorization(String providerReference) {
                voidedReferences.add(providerReference);
                return standIn.voidAuthorization(providerReference);
            }
        };
        return new PaymentsProcessingServiceImpl(
                paymentRepository,
                eventPublisher,
                recording,
                new FlowProperties(new FlowProperties.Payments(3, Duration.ZERO)),
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private VoidPaymentInput voidInput(String orderId, String reason) {
        return new VoidPaymentInput().setOrderId(orderId).setReason(reason);
    }

    @Test
    void voidPaymentFromAuthorizedAndCaptureFailedCallsProviderTest() {
        var voided = new ArrayList<String>();
        var service = serviceWithRecordingProvider(voided);

        for (var orderId : new String[] {"ORD-003", "ORD-012"}) {
            var providerReference = payment(orderId).getProviderReference();
            eventPublisher.getEvents().clear();

            service.voidPayment(voidInput(orderId, "FULFILLMENT_FAILED"));

            var saved = payment(orderId);
            assertThat(saved.getStatus()).as(orderId).isEqualTo(PaymentStatus.VOIDED);
            assertThat(saved.getVoidedAt()).as(orderId).isEqualTo(NOW);
            assertThat(saved.getFailureReason()).as(orderId).isEqualTo("FULFILLMENT_FAILED");
            assertThat(voided).as(orderId).contains(providerReference);
            assertThat(eventPublisher.getEvents())
                    .as(orderId)
                    .hasSize(1)
                    .first()
                    .isInstanceOf(PaymentVoided.class);
            var event = (PaymentVoided) eventPublisher.getEvents().get(0);
            assertThat(event.getOrderId()).isEqualTo(orderId);
            assertThat(event.getReason()).isEqualTo("FULFILLMENT_FAILED");
            assertThat(event.getVoidedAt()).isEqualTo(NOW);
        }
        assertThat(voided).hasSize(2);
    }

    @Test
    void voidPaymentNeverAuthorizedSkipsProviderTest() {
        var voided = new ArrayList<String>();
        var service = serviceWithRecordingProvider(voided);

        for (var orderId : new String[] {"ORD-002", "ORD-008"}) {
            eventPublisher.getEvents().clear();

            service.voidPayment(voidInput(orderId, "ORDER_CANCELLED"));

            assertThat(payment(orderId).getStatus()).as(orderId).isEqualTo(PaymentStatus.VOIDED);
            assertThat(payment(orderId).getVoidedAt()).as(orderId).isEqualTo(NOW);
            assertThat(eventPublisher.getEvents())
                    .as(orderId)
                    .hasSize(1)
                    .first()
                    .isInstanceOf(PaymentVoided.class);
        }
        assertThat(voided).isEmpty();
    }

    @Test
    void voidPaymentOnVoidedPaymentIsNoOpTest() {
        var voided = new ArrayList<String>();
        var service = serviceWithRecordingProvider(voided);

        service.voidPayment(voidInput("ORD-013", "FULFILLMENT_FAILED"));

        assertThat(payment("ORD-013").getStatus()).isEqualTo(PaymentStatus.VOIDED);
        assertThat(voided).isEmpty();
        assertThat(eventPublisher.getEvents()).isEmpty();
    }

    @Test
    void voidPaymentOnCapturedPaymentIsRejectedTest() {
        assertThatThrownBy(() -> paymentsProcessingService.voidPayment(voidInput("ORD-001", "X")))
                .isInstanceOf(PaymentTransitions.InvalidStateTransitionException.class);
        assertThat(payment("ORD-001").getStatus()).isEqualTo(PaymentStatus.CAPTURED);
        assertThat(eventPublisher.getEvents()).isEmpty();
    }

    @Test
    void voidPaymentUnknownOrderTest() {
        assertThatThrownBy(() -> paymentsProcessingService.voidPayment(voidInput("ORD-NOPE", "X")))
                .isInstanceOf(NoSuchElementException.class);
    }
}
