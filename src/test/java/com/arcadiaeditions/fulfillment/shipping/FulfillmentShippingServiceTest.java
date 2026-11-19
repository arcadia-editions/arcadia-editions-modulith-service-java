package com.arcadiaeditions.fulfillment.shipping;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import com.arcadiaeditions.config.TestClock;
import com.arcadiaeditions.fulfillment.shipping.config.*;
import com.arcadiaeditions.fulfillment.shipping.domain.*;
import com.arcadiaeditions.fulfillment.shipping.domain.events.*;
import com.arcadiaeditions.fulfillment.shipping.dtos.*;
import com.arcadiaeditions.fulfillment.shipping.inmemory.*;
import com.arcadiaeditions.fulfillment.shipping.mappers.*;
import java.time.*;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Acceptance Test for FulfillmentShippingService.
 */
class FulfillmentShippingServiceTest {

    private final Logger log = LoggerFactory.getLogger(getClass());

    ServicesInMemoryConfig context = new ServicesInMemoryConfig();
    FulfillmentShippingServiceImpl fulfillmentShippingService = context.fulfillmentShippingService();

    FulfillmentRepositoryInMemory fulfillmentRepository = context.fulfillmentRepository();

    @BeforeEach
    void setUp() {
        context.reloadTestData();
    }

    /** OrderConfirmed of data.sql ORD-004 (postal 28004, SKU-002x1 + SKU-003x1), which has no Fulfillment yet. */
    private ScheduleFulfillmentInput ord004Input() {
        return new ScheduleFulfillmentInput()
                .setOrderId("ORD-004")
                .setCustomerId("CUST-004")
                .setShippingAddress(new ShippingAddressInput()
                        .setName("Arcadia Customer")
                        .setLine1("1 Book Lane")
                        .setCity("Madrid")
                        .setPostalCode("28004")
                        .setCountry("ES"))
                .setItems(List.of(
                        new FulfillmentItemInput().setSku("SKU-002").setQuantity(1),
                        new FulfillmentItemInput().setSku("SKU-003").setQuantity(1)));
    }

    /** OrderConfirmed redelivered for data.sql ORD-001, whose Fulfillment FUL-001 is already SCHEDULED. */
    private ScheduleFulfillmentInput ord001Input() {
        return new ScheduleFulfillmentInput()
                .setOrderId("ORD-001")
                .setCustomerId("CUST-001")
                .setShippingAddress(new ShippingAddressInput()
                        .setName("Arcadia Customer")
                        .setLine1("1 Book Lane")
                        .setCity("Madrid")
                        .setPostalCode("28001")
                        .setCountry("ES"))
                .setItems(List.of(new FulfillmentItemInput().setSku("SKU-001").setQuantity(1)));
    }

    private long fulfillmentsOf(String orderId) {
        return fulfillmentRepository.findAll().stream()
                .filter(f -> orderId.equals(f.getOrderId()))
                .count();
    }

    @Test
    void scheduleFulfillmentTest() {
        // OrderConfirmed for ORD-004: the stand-in decision schedules it -> Fulfillment SCHEDULED, FulfillmentScheduled
        var fulfillment = fulfillmentShippingService.scheduleFulfillment(ord004Input());

        assertThat(fulfillment.getId()).isNotNull();
        assertThat(fulfillment.getFulfillmentId()).startsWith("FUL-");
        assertThat(fulfillment.getOrderId()).isEqualTo("ORD-004");
        assertThat(fulfillment.getCustomerId()).isEqualTo("CUST-004");
        assertThat(fulfillment.getStatus()).isEqualTo(FulfillmentStatus.SCHEDULED);
        assertThat(fulfillment.getScheduledAt()).isEqualTo(TestClock.NOW);
        assertThat(fulfillment.getFailedAt()).isNull();
        assertThat(fulfillment.getFailureReason()).isNull();
        assertThat(fulfillment.getShippingAddress().getPostalCode()).isEqualTo("28004");
        assertThat(fulfillment.getShippingAddress().getCountry()).isEqualTo("ES");
        assertThat(fulfillment.getItems()).hasSize(2);
        assertThat(fulfillment.getItems().get(0).getSku()).isEqualTo("SKU-002");
        assertThat(fulfillment.getItems().get(1).getSku()).isEqualTo("SKU-003");
        assertThat(fulfillmentsOf("ORD-004")).isEqualTo(1);

        var events = context.eventPublisher().getEvents();
        assertThat(events).hasSize(1);
        assertThat(events.get(0)).isInstanceOf(FulfillmentScheduled.class);
        var event = (FulfillmentScheduled) events.get(0);
        assertThat(event.getFulfillmentId()).isEqualTo(fulfillment.getFulfillmentId());
        assertThat(event.getOrderId()).isEqualTo("ORD-004");
        assertThat(event.getCustomerId()).isEqualTo("CUST-004");
        assertThat(event.getScheduledAt()).isEqualTo(TestClock.NOW);
    }

    @Test
    void scheduleFulfillmentRedeliveryTest() {
        // OrderConfirmed redelivered for ORD-001: FUL-001 already exists; nothing changes, nothing is emitted
        var fulfillment = fulfillmentShippingService.scheduleFulfillment(ord001Input());

        assertThat(fulfillment.getFulfillmentId()).isEqualTo("FUL-001");
        assertThat(fulfillment.getStatus()).isEqualTo(FulfillmentStatus.SCHEDULED);
        assertThat(fulfillment.getScheduledAt()).isEqualTo(Instant.parse("2026-09-26T13:06:00Z"));
        assertThat(fulfillmentsOf("ORD-001")).isEqualTo(1);
        assertThat(context.eventPublisher().getEvents()).isEmpty();
    }

    private ScheduleFulfillmentInput postal00000Input(String orderId, String customerId) {
        return new ScheduleFulfillmentInput()
                .setOrderId(orderId)
                .setCustomerId(customerId)
                .setShippingAddress(new ShippingAddressInput()
                        .setName("Arcadia Customer")
                        .setLine1("1 Book Lane")
                        .setCity("Madrid")
                        .setPostalCode("00000")
                        .setCountry("ES"))
                .setItems(List.of(new FulfillmentItemInput().setSku("SKU-002").setQuantity(1)));
    }

    @Test
    void scheduleFulfillmentFailedTest() {
        // OrderConfirmed for ORD-005 (postal 00000): carrier_unavailable -> Fulfillment FAILED, FulfillmentFailed only
        var fulfillment = fulfillmentShippingService.scheduleFulfillment(postal00000Input("ORD-005", "CUST-005"));

        assertThat(fulfillment.getId()).isNotNull();
        assertThat(fulfillment.getFulfillmentId()).startsWith("FUL-");
        assertThat(fulfillment.getOrderId()).isEqualTo("ORD-005");
        assertThat(fulfillment.getStatus()).isEqualTo(FulfillmentStatus.FAILED);
        assertThat(fulfillment.getFailedAt()).isEqualTo(TestClock.NOW);
        assertThat(fulfillment.getFailureReason()).isEqualTo("carrier_unavailable");
        assertThat(fulfillment.getScheduledAt()).isNull();
        assertThat(fulfillmentsOf("ORD-005")).isEqualTo(1);

        var events = context.eventPublisher().getEvents();
        assertThat(events).hasSize(1);
        assertThat(events.get(0)).isInstanceOf(FulfillmentFailed.class);
        var event = (FulfillmentFailed) events.get(0);
        assertThat(event.getFulfillmentId()).isEqualTo(fulfillment.getFulfillmentId());
        assertThat(event.getOrderId()).isEqualTo("ORD-005");
        assertThat(event.getCustomerId()).isEqualTo("CUST-005");
        assertThat(event.getReason()).isEqualTo("carrier_unavailable");
        assertThat(event.getFailedAt()).isEqualTo(TestClock.NOW);
    }

    @Test
    void scheduleFulfillmentFailedRedeliveryTest() {
        // OrderConfirmed redelivered for ORD-007: FUL-003 already FAILED; nothing changes, nothing is emitted
        var fulfillment = fulfillmentShippingService.scheduleFulfillment(postal00000Input("ORD-007", "CUST-007"));

        assertThat(fulfillment.getFulfillmentId()).isEqualTo("FUL-003");
        assertThat(fulfillment.getStatus()).isEqualTo(FulfillmentStatus.FAILED);
        assertThat(fulfillment.getFailureReason()).isEqualTo("carrier_unavailable");
        assertThat(fulfillmentsOf("ORD-007")).isEqualTo(1);
        assertThat(context.eventPublisher().getEvents()).isEmpty();
    }

    private RecordPaymentCapturedInput capturedInput(String orderId, Instant at) {
        return new RecordPaymentCapturedInput().setOrderId(orderId).setCapturedAt(at);
    }

    @Test
    void recordPaymentCapturedTest() {
        // PaymentCaptured of ORD-023: FUL-009 SCHEDULED without paymentCapturedAt -> set, no state change, no event
        var capturedAt = Instant.parse("2026-10-14T09:50:48Z");
        var result = fulfillmentShippingService.recordPaymentCaptured(capturedInput("ORD-023", capturedAt));

        assertThat(result).isPresent();
        var fulfillment = result.get();
        assertThat(fulfillment.getFulfillmentId()).isEqualTo("FUL-009");
        assertThat(fulfillment.getPaymentCapturedAt()).isEqualTo(capturedAt);
        assertThat(fulfillment.getStatus()).isEqualTo(FulfillmentStatus.SCHEDULED);
        assertThat(fulfillmentRepository.findByOrderId("ORD-023").orElseThrow().getPaymentCapturedAt())
                .isEqualTo(capturedAt);
        assertThat(context.eventPublisher().getEvents()).isEmpty();
    }

    @Test
    void recordPaymentCapturedRedeliveryTest() {
        // ORD-001: paymentCapturedAt already set; unchanged, no event
        var result = fulfillmentShippingService.recordPaymentCaptured(
                capturedInput("ORD-001", Instant.parse("2026-12-01T00:00:00Z")));

        assertThat(result).isEmpty();
        assertThat(fulfillmentRepository.findByOrderId("ORD-001").orElseThrow().getPaymentCapturedAt())
                .isEqualTo(Instant.parse("2026-09-26T13:06:35Z"));
        assertThat(context.eventPublisher().getEvents()).isEmpty();
    }

    @Test
    void recordPaymentCapturedNoFulfillmentTest() {
        // ORD-004 has no Fulfillment: no-op
        var result = fulfillmentShippingService.recordPaymentCaptured(capturedInput("ORD-004", TestClock.NOW));

        assertThat(result).isEmpty();
        assertThat(fulfillmentsOf("ORD-004")).isZero();
        assertThat(context.eventPublisher().getEvents()).isEmpty();
    }

    @Test
    void recordPaymentCapturedNotScheduledTest() {
        // ORD-007: FUL-003 FAILED: no-op
        var result = fulfillmentShippingService.recordPaymentCaptured(capturedInput("ORD-007", TestClock.NOW));

        assertThat(result).isEmpty();
        assertThat(fulfillmentRepository.findByOrderId("ORD-007").orElseThrow().getPaymentCapturedAt())
                .isNull();
        assertThat(context.eventPublisher().getEvents()).isEmpty();
    }

    @Test
    void markReadyToShipTest() {
        // ORD-001: FUL-001 SCHEDULED with paymentCapturedAt -> READY_TO_SHIP, exactly one FulfillmentReadyToShip
        var fulfillment = fulfillmentShippingService.markReadyToShip(1L);

        assertThat(fulfillment.getStatus()).isEqualTo(FulfillmentStatus.READY_TO_SHIP);
        assertThat(fulfillmentRepository.findById(1L).orElseThrow().getStatus())
                .isEqualTo(FulfillmentStatus.READY_TO_SHIP);
        var events = context.eventPublisher().getEvents();
        assertThat(events).hasSize(1);
        assertThat(events.get(0)).isInstanceOf(FulfillmentReadyToShip.class);
        assertThat(((FulfillmentReadyToShip) events.get(0)).getOrderId()).isEqualTo("ORD-001");
    }

    @Test
    void markReadyToShipRefusedUntilPaymentCapturedTest() {
        // ORD-023 and ORD-006: SCHEDULED without paymentCapturedAt -> refused naming the cause, no change, no event
        for (var id : List.of(401L, 51L)) {
            assertThatThrownBy(() -> fulfillmentShippingService.markReadyToShip(id))
                    .isInstanceOf(PaymentNotCapturedException.class)
                    .hasMessageContaining("payment not captured yet");
            var unchanged = fulfillmentRepository.findById(id).orElseThrow();
            assertThat(unchanged.getStatus()).isEqualTo(FulfillmentStatus.SCHEDULED);
        }
        assertThat(context.eventPublisher().getEvents()).isEmpty();
    }

    @Test
    void markReadyToShipAfterPaymentCapturedTest() {
        // ORD-023: PaymentCaptured recorded first, then the warehouse may mark it
        fulfillmentShippingService.recordPaymentCaptured(capturedInput("ORD-023", TestClock.NOW));

        var fulfillment = fulfillmentShippingService.markReadyToShip(401L);

        assertThat(fulfillment.getStatus()).isEqualTo(FulfillmentStatus.READY_TO_SHIP);
        assertThat(context.eventPublisher().getEvents()).hasSize(1);
    }
}
