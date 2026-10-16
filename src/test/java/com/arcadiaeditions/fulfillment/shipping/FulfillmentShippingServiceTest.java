package com.arcadiaeditions.fulfillment.shipping;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import com.arcadiaeditions.fulfillment.shipping.config.*;
import com.arcadiaeditions.fulfillment.shipping.domain.*;
import com.arcadiaeditions.fulfillment.shipping.domain.events.*;
import com.arcadiaeditions.fulfillment.shipping.dtos.*;
import com.arcadiaeditions.fulfillment.shipping.inmemory.*;
import com.arcadiaeditions.fulfillment.shipping.mappers.*;
import java.time.*;
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

    private ScheduleFulfillmentInput input(String orderId, String postalCode) {
        return new ScheduleFulfillmentInput()
                .setOrderId(orderId)
                .setCustomerId("CUST-004")
                .setShippingAddress(new ShippingAddressInput()
                        .setName("Arcadia Customer")
                        .setLine1("1 Book Lane")
                        .setCity("Madrid")
                        .setPostalCode(postalCode)
                        .setCountry("ES"))
                .addItems(new FulfillmentItemInput().setSku("SKU-002").setQuantity(1));
    }

    @Test
    void scheduleFulfillmentTest() {
        var fulfillment = fulfillmentShippingService.scheduleFulfillment(input("ORD-004", "28004"));

        assertThat(fulfillment.getStatus()).isEqualTo(FulfillmentStatus.SCHEDULED);
        assertThat(fulfillment.getFulfillmentId()).startsWith("FUL-");
        assertThat(fulfillment.getOrderId()).isEqualTo("ORD-004");
        assertThat(fulfillment.getScheduledAt()).isEqualTo(Instant.parse("2026-10-14T10:00:00Z"));
        assertThat(fulfillmentRepository.findByOrderId("ORD-004")).isPresent();
        assertThat(context.eventPublisher().getEvents()).hasSize(1).first().isInstanceOf(FulfillmentScheduled.class);
    }

    @Test
    void scheduleFulfillmentRedeliveryTest() {
        var first = fulfillmentShippingService.scheduleFulfillment(input("ORD-004", "28004"));
        context.eventPublisher().getEvents().clear();
        long count = fulfillmentRepository.count();

        var second = fulfillmentShippingService.scheduleFulfillment(input("ORD-004", "28004"));

        assertThat(second.getFulfillmentId()).isEqualTo(first.getFulfillmentId());
        assertThat(fulfillmentRepository.count()).isEqualTo(count);
        assertThat(context.eventPublisher().getEvents()).isEmpty();
    }

    @Test
    void scheduleFulfillmentExistingFixtureRedeliveryTest() {
        var existing = fulfillmentShippingService.scheduleFulfillment(input("ORD-001", "28001"));

        assertThat(existing.getOrderId()).isEqualTo("ORD-001");
        assertThat(context.eventPublisher().getEvents()).isEmpty();
    }

    @Test
    void scheduleFulfillmentFailedTest() {
        var fulfillment = fulfillmentShippingService.scheduleFulfillment(input("ORD-005", "00000"));

        assertThat(fulfillment.getStatus()).isEqualTo(FulfillmentStatus.FAILED);
        assertThat(fulfillment.getFailedAt()).isEqualTo(Instant.parse("2026-10-14T10:00:00Z"));
        assertThat(fulfillment.getFailureReason()).isEqualTo("carrier_unavailable");
        assertThat(fulfillment.getScheduledAt()).isNull();
        var saved = fulfillmentRepository.findByOrderId("ORD-005");
        assertThat(saved).isPresent();
        assertThat(saved.get().getStatus()).isEqualTo(FulfillmentStatus.FAILED);
        assertThat(context.eventPublisher().getEvents())
                .hasSize(1)
                .first()
                .isInstanceOfSatisfying(FulfillmentFailed.class, event -> {
                    assertThat(event.getReason()).isEqualTo("carrier_unavailable");
                    assertThat(event.getOrderId()).isEqualTo("ORD-005");
                    assertThat(event.getFailedAt()).isEqualTo(Instant.parse("2026-10-14T10:00:00Z"));
                });
        assertThat(context.eventPublisher().getEvents()).noneMatch(FulfillmentScheduled.class::isInstance);
    }

    @Test
    void scheduleFulfillmentFailedRedeliveryTest() {
        var first = fulfillmentShippingService.scheduleFulfillment(input("ORD-005", "00000"));
        context.eventPublisher().getEvents().clear();
        long count = fulfillmentRepository.count();

        var second = fulfillmentShippingService.scheduleFulfillment(input("ORD-005", "00000"));

        assertThat(second.getFulfillmentId()).isEqualTo(first.getFulfillmentId());
        assertThat(second.getStatus()).isEqualTo(FulfillmentStatus.FAILED);
        assertThat(fulfillmentRepository.count()).isEqualTo(count);
        assertThat(context.eventPublisher().getEvents()).isEmpty();
    }

    @Test
    void markReadyToShipTest() { // TODO: implement this test
    }
}
