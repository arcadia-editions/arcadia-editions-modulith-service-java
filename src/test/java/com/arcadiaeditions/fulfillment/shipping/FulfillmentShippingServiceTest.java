package com.arcadiaeditions.fulfillment.shipping;

import static org.assertj.core.api.Assertions.assertThat;
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

    @Test
    void markReadyToShipTest() { // TODO: implement this test
    }
}
