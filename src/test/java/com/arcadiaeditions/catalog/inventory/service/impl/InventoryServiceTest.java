package com.arcadiaeditions.catalog.inventory.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import com.arcadiaeditions.catalog.inventory.config.*;
import com.arcadiaeditions.catalog.inventory.domain.*;
import com.arcadiaeditions.catalog.inventory.domain.events.*;
import com.arcadiaeditions.catalog.inventory.repository.jpa.*;
import com.arcadiaeditions.catalog.inventory.repository.jpa.inmemory.*;
import com.arcadiaeditions.catalog.inventory.service.*;
import com.arcadiaeditions.catalog.inventory.service.dtos.*;
import com.arcadiaeditions.catalog.inventory.service.impl.mappers.*;
import java.time.*;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Acceptance Test for InventoryService.
 */
class InventoryServiceTest {

    private final Logger log = LoggerFactory.getLogger(getClass());

    ServicesInMemoryConfig context = new ServicesInMemoryConfig();
    InventoryServiceImpl inventoryService = context.inventoryService();

    StockReservationRepositoryInMemory stockReservationRepository = context.stockReservationRepository();

    @BeforeEach
    void setUp() {
        context.reloadTestData();
    }

    @Test
    void reserveStockTest() {
        var input = new ReserveStockInput()
                .setOrderId("ORD-NEW")
                .addItems(new ReserveStockItemInput().setSku("SKU-001").setQuantity(2))
                .addItems(new ReserveStockItemInput().setSku("SKU-002").setQuantity(3))
                .addItems(new ReserveStockItemInput().setSku("SKU-001").setQuantity(1));

        var result = inventoryService.reserveStock(input);

        assertThat(result.getOutcome()).isEqualTo(ReserveStockOutcome.RESERVED);
        assertThat(result.getUnavailable()).isNull();
        var now = Instant.parse("2026-10-14T10:00:00Z");
        assertThat(result.getReserved().getOrderId()).isEqualTo("ORD-NEW");
        assertThat(result.getReserved().getExpiresAt()).isEqualTo(now.plus(Duration.ofMinutes(10)));
        assertThat(result.getReserved().getReservations())
                .extracting(StockReservedLine::getSku, StockReservedLine::getQuantity)
                .containsExactlyInAnyOrder(tuple("SKU-001", 3), tuple("SKU-002", 3));

        var reservations =
                stockReservationRepository.findByOrderIdAndStatusIn("ORD-NEW", List.of(ReservationStatus.RESERVED));
        assertThat(reservations).hasSize(2);
        assertThat(reservations).allSatisfy(r -> {
            assertThat(r.getReservationId()).startsWith("RES-");
            assertThat(r.getStatus()).isEqualTo(ReservationStatus.RESERVED);
            assertThat(r.getReservedAt()).isEqualTo(now);
            assertThat(r.getExpiresAt()).isEqualTo(now.plus(Duration.ofMinutes(10)));
        });
        assertThat(position("SKU-001"))
                .extracting(
                        InventoryPosition::getAvailableQuantity,
                        InventoryPosition::getReservedQuantity,
                        InventoryPosition::getSoldQuantity)
                .containsExactly(6, 3, 1);
        assertThat(position("SKU-002"))
                .extracting(
                        InventoryPosition::getAvailableQuantity,
                        InventoryPosition::getReservedQuantity,
                        InventoryPosition::getSoldQuantity)
                .containsExactly(17, 10, 6);
        assertThat(context.eventPublisher().getEvents()).isEmpty();
    }

    @Test
    void reserveStockUnavailableIsAllOrNothingTest() {
        long reservationsBefore = stockReservationRepository.count();
        var input = new ReserveStockInput()
                .setOrderId("ORD-SHORT")
                .addItems(new ReserveStockItemInput().setSku("SKU-001").setQuantity(2))
                .addItems(new ReserveStockItemInput().setSku("SKU-003").setQuantity(5));

        var result = inventoryService.reserveStock(input);

        assertThat(result.getOutcome()).isEqualTo(ReserveStockOutcome.UNAVAILABLE);
        assertThat(result.getReserved()).isNull();
        assertThat(result.getUnavailable().getUnavailableItems())
                .extracting(
                        StockUnavailableLine::getSku,
                        StockUnavailableLine::getRequestedQuantity,
                        StockUnavailableLine::getAvailableQuantity)
                .containsExactly(tuple("SKU-003", 5, 2));
        assertThat(stockReservationRepository.count()).isEqualTo(reservationsBefore);
        assertThat(stockReservationRepository.findByOrderIdAndStatusIn(
                        "ORD-SHORT", List.of(ReservationStatus.RESERVED)))
                .isEmpty();
        assertThat(position("SKU-001"))
                .extracting(
                        InventoryPosition::getAvailableQuantity,
                        InventoryPosition::getReservedQuantity,
                        InventoryPosition::getSoldQuantity)
                .containsExactly(9, 0, 1);
        assertThat(position("SKU-003"))
                .extracting(
                        InventoryPosition::getAvailableQuantity,
                        InventoryPosition::getReservedQuantity,
                        InventoryPosition::getSoldQuantity)
                .containsExactly(2, 1, 1);
        assertThat(context.eventPublisher().getEvents()).isEmpty();
    }

    @Test
    void reserveStockUnavailableListsEveryShortLineAndUnknownSkuTest() {
        var input = new ReserveStockInput()
                .setOrderId("ORD-SHORT-2")
                .addItems(new ReserveStockItemInput().setSku("SKU-001").setQuantity(10))
                .addItems(new ReserveStockItemInput().setSku("SKU-002").setQuantity(1))
                .addItems(new ReserveStockItemInput().setSku("SKU-UNKNOWN").setQuantity(4));

        var result = inventoryService.reserveStock(input);

        assertThat(result.getOutcome()).isEqualTo(ReserveStockOutcome.UNAVAILABLE);
        assertThat(result.getUnavailable().getOrderId()).isNotBlank();
        assertThat(result.getUnavailable().getUnavailableItems())
                .extracting(
                        StockUnavailableLine::getSku,
                        StockUnavailableLine::getRequestedQuantity,
                        StockUnavailableLine::getAvailableQuantity)
                .containsExactlyInAnyOrder(tuple("SKU-001", 10, 9), tuple("SKU-UNKNOWN", 4, 0));
        assertThat(stockReservationRepository.findByOrderIdAndStatusIn(
                        "ORD-SHORT-2", List.of(ReservationStatus.RESERVED)))
                .isEmpty();
        assertThat(position("SKU-002"))
                .extracting(
                        InventoryPosition::getAvailableQuantity,
                        InventoryPosition::getReservedQuantity,
                        InventoryPosition::getSoldQuantity)
                .containsExactly(20, 7, 6);
    }

    @Test
    void releaseStockReservedTest() {
        // ORD-002: SKU-002 x2 + SKU-003 x1, both RESERVED
        var before2 = counters("SKU-002");
        var before3 = counters("SKU-003");

        var result = inventoryService.releaseStock(
                new ReleaseStockInput().setOrderId("ORD-002").setReason(StockReleaseReason.PAYMENT_DECLINED));

        var now = Instant.parse("2026-10-14T10:00:00Z");
        assertThat(result).hasSize(2);
        assertThat(result).allSatisfy(r -> {
            assertThat(r.getStatus()).isEqualTo(ReservationStatus.RELEASED);
            assertThat(r.getReleasedAt()).isEqualTo(now);
            assertThat(r.getReleaseReason()).isEqualTo(StockReleaseReason.PAYMENT_DECLINED);
        });
        // reserved -> available
        assertThat(counters("SKU-002")).containsExactly(before2.get(0) + 2, before2.get(1) - 2, before2.get(2));
        assertThat(counters("SKU-003")).containsExactly(before3.get(0) + 1, before3.get(1) - 1, before3.get(2));

        var events = context.eventPublisher().getEvents();
        assertThat(events).hasSize(1);
        var event = (StockReleased) events.get(0);
        assertThat(event.getOrderId()).isEqualTo("ORD-002");
        assertThat(event.getReason()).isEqualTo(StockReleaseReason.PAYMENT_DECLINED);
        assertThat(event.getReleasedAt()).isEqualTo(now);
        assertThat(event.getItems())
                .extracting(StockReleasedItem::getSku, StockReleasedItem::getQuantity)
                .containsExactlyInAnyOrder(tuple("SKU-002", 2), tuple("SKU-003", 1));

        // second call: nothing left to release, no new event
        var again = inventoryService.releaseStock(
                new ReleaseStockInput().setOrderId("ORD-002").setReason(StockReleaseReason.PAYMENT_DECLINED));
        assertThat(again).isEmpty();
        assertThat(context.eventPublisher().getEvents()).hasSize(1);
    }

    @Test
    void releaseStockConfirmedTest() {
        // ORD-004: SKU-002 x1 + SKU-003 x1, both CONFIRMED
        var before2 = counters("SKU-002");
        var before3 = counters("SKU-003");

        var result = inventoryService.releaseStock(
                new ReleaseStockInput().setOrderId("ORD-004").setReason(StockReleaseReason.PAYMENT_VOIDED));

        assertThat(result).hasSize(2);
        assertThat(result).allSatisfy(r -> {
            assertThat(r.getStatus()).isEqualTo(ReservationStatus.RELEASED);
            assertThat(r.getReleasedAt()).isEqualTo(Instant.parse("2026-10-14T10:00:00Z"));
            assertThat(r.getReleaseReason()).isEqualTo(StockReleaseReason.PAYMENT_VOIDED);
        });
        // sold -> available (Q-09)
        assertThat(counters("SKU-002")).containsExactly(before2.get(0) + 1, before2.get(1), before2.get(2) - 1);
        assertThat(counters("SKU-003")).containsExactly(before3.get(0) + 1, before3.get(1), before3.get(2) - 1);

        var events = context.eventPublisher().getEvents();
        assertThat(events).hasSize(1);
        var event = (StockReleased) events.get(0);
        assertThat(event.getOrderId()).isEqualTo("ORD-004");
        assertThat(event.getReason()).isEqualTo(StockReleaseReason.PAYMENT_VOIDED);
        assertThat(event.getItems())
                .extracting(StockReleasedItem::getSku, StockReleasedItem::getQuantity)
                .containsExactlyInAnyOrder(tuple("SKU-002", 1), tuple("SKU-003", 1));
    }

    @Test
    void releaseStockNothingToReleaseTest() {
        // ORD-013 is already RELEASED; ORD-UNKNOWN has no reservations
        assertThat(inventoryService.releaseStock(
                        new ReleaseStockInput().setOrderId("ORD-013").setReason(StockReleaseReason.PAYMENT_VOIDED)))
                .isEmpty();
        assertThat(inventoryService.releaseStock(new ReleaseStockInput()
                        .setOrderId("ORD-UNKNOWN")
                        .setReason(StockReleaseReason.PAYMENT_DECLINED)))
                .isEmpty();
        assertThat(context.eventPublisher().getEvents()).isEmpty();
    }

    @Test
    void confirmStockReservationTest() {
        // ORD-002: SKU-002 x2 + SKU-003 x1, both RESERVED
        var result = inventoryService.confirmStockReservation(new ConfirmStockReservationInput().setOrderId("ORD-002"));

        assertThat(result).hasSize(2);
        assertThat(result).allSatisfy(r -> {
            assertThat(r.getStatus()).isEqualTo(ReservationStatus.CONFIRMED);
            assertThat(r.getConfirmedAt()).isEqualTo(Instant.parse("2026-10-14T10:00:00Z"));
        });
        assertThat(stockReservationRepository.findByOrderIdAndStatusIn("ORD-002", List.of(ReservationStatus.RESERVED)))
                .isEmpty();
        assertThat(position("SKU-002"))
                .extracting(
                        InventoryPosition::getAvailableQuantity,
                        InventoryPosition::getReservedQuantity,
                        InventoryPosition::getSoldQuantity)
                .containsExactly(20, 5, 8);
        assertThat(position("SKU-003"))
                .extracting(
                        InventoryPosition::getAvailableQuantity,
                        InventoryPosition::getReservedQuantity,
                        InventoryPosition::getSoldQuantity)
                .containsExactly(2, 0, 2);

        var events = context.eventPublisher().getEvents();
        assertThat(events).hasSize(1);
        var event = (StockReservationConfirmed) events.get(0);
        assertThat(event.getOrderId()).isEqualTo("ORD-002");
        assertThat(event.getItems())
                .extracting(StockReservationConfirmedItem::getSku, StockReservationConfirmedItem::getQuantity)
                .containsExactlyInAnyOrder(tuple("SKU-002", 2), tuple("SKU-003", 1));

        // second call: no RESERVED left, no new event
        var again = inventoryService.confirmStockReservation(new ConfirmStockReservationInput().setOrderId("ORD-002"));
        assertThat(again).isEmpty();
        assertThat(context.eventPublisher().getEvents()).hasSize(1);
    }

    @Test
    void confirmStockReservationRedeliveryIsNoOpTest() {
        // ORD-001 is already CONFIRMED, ORD-UNKNOWN has no reservations: nothing RESERVED left
        var before = counters("SKU-001");

        assertThat(inventoryService.confirmStockReservation(new ConfirmStockReservationInput().setOrderId("ORD-001")))
                .isEmpty();
        assertThat(inventoryService.confirmStockReservation(
                        new ConfirmStockReservationInput().setOrderId("ORD-UNKNOWN")))
                .isEmpty();

        assertThat(counters("SKU-001")).isEqualTo(before);
        assertThat(context.eventPublisher().getEvents()).isEmpty();
    }

    private List<Integer> counters(String sku) {
        var p = position(sku);
        return List.of(p.getAvailableQuantity(), p.getReservedQuantity(), p.getSoldQuantity());
    }

    private InventoryPosition position(String sku) {
        return context.inventoryPositionRepository()
                .findAllBySkuInOrderBySku(List.of(sku))
                .get(0);
    }
}
