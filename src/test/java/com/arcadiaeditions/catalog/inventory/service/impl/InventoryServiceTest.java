package com.arcadiaeditions.catalog.inventory.service.impl;

import static org.mockito.Mockito.*;

import com.arcadiaeditions.catalog.inventory.config.*;
import com.arcadiaeditions.catalog.inventory.domain.*;
import com.arcadiaeditions.catalog.inventory.repository.jpa.*;
import com.arcadiaeditions.catalog.inventory.repository.jpa.inmemory.*;
import com.arcadiaeditions.catalog.inventory.service.*;
import com.arcadiaeditions.catalog.inventory.service.dtos.*;
import com.arcadiaeditions.catalog.inventory.service.impl.mappers.*;
import java.time.*;
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
                .setItems(java.util.List.of(
                        new ReserveStockItemInput().setSku("SKU-002").setQuantity(2),
                        new ReserveStockItemInput().setSku("SKU-003").setQuantity(1)));

        var result = inventoryService.reserveStock(input);

        var now = context.clock.instant();
        org.junit.jupiter.api.Assertions.assertEquals(ReserveStockOutcome.RESERVED, result.getOutcome());
        org.junit.jupiter.api.Assertions.assertNull(result.getUnavailable());
        var reserved = result.getReserved();
        org.junit.jupiter.api.Assertions.assertEquals("ORD-NEW", reserved.getOrderId());
        org.junit.jupiter.api.Assertions.assertEquals(now.plus(Duration.ofMinutes(10)), reserved.getExpiresAt());
        org.junit.jupiter.api.Assertions.assertEquals(2, reserved.getReservations().size());
        reserved.getReservations().forEach(line -> {
            org.junit.jupiter.api.Assertions.assertTrue(line.getReservationId().startsWith("RES-"));
            org.junit.jupiter.api.Assertions.assertEquals(now, line.getReservedAt());
        });

        var saved = stockReservationRepository.findAll().stream()
                .filter(r -> "ORD-NEW".equals(r.getOrderId()))
                .toList();
        org.junit.jupiter.api.Assertions.assertEquals(2, saved.size());
        saved.forEach(r -> {
            org.junit.jupiter.api.Assertions.assertEquals(ReservationStatus.RESERVED, r.getStatus());
            org.junit.jupiter.api.Assertions.assertEquals(now, r.getReservedAt());
            org.junit.jupiter.api.Assertions.assertEquals(now.plus(Duration.ofMinutes(10)), r.getExpiresAt());
        });

        // SKU-002 20/7 -> 18/9 ; SKU-003 2/1 -> 1/2
        assertPosition("SKU-002", 18, 9);
        assertPosition("SKU-003", 1, 2);
        assertPosition("SKU-001", 9, 0);
    }

    @Test
    void reserveStockUnavailableReservesNothingTest() {
        var input = new ReserveStockInput()
                .setOrderId("ORD-NEW")
                .setItems(java.util.List.of(
                        new ReserveStockItemInput().setSku("SKU-002").setQuantity(2),
                        new ReserveStockItemInput().setSku("SKU-003").setQuantity(3),
                        new ReserveStockItemInput().setSku("SKU-004").setQuantity(1),
                        new ReserveStockItemInput().setSku("SKU-UNKNOWN").setQuantity(1)));
        var before = stockReservationRepository.findAll().size();

        var result = inventoryService.reserveStock(input);

        org.junit.jupiter.api.Assertions.assertEquals(ReserveStockOutcome.UNAVAILABLE, result.getOutcome());
        org.junit.jupiter.api.Assertions.assertNull(result.getReserved());
        var unavailable = result.getUnavailable();
        org.junit.jupiter.api.Assertions.assertEquals("ORD-NEW", unavailable.getOrderId());
        var lines = unavailable.getUnavailableItems().stream()
                .collect(java.util.stream.Collectors.toMap(StockUnavailableLine::getSku, l -> l));
        org.junit.jupiter.api.Assertions.assertEquals(3, lines.size());
        org.junit.jupiter.api.Assertions.assertEquals(3, lines.get("SKU-003").getRequestedQuantity());
        org.junit.jupiter.api.Assertions.assertEquals(2, lines.get("SKU-003").getAvailableQuantity());
        org.junit.jupiter.api.Assertions.assertEquals(1, lines.get("SKU-004").getRequestedQuantity());
        org.junit.jupiter.api.Assertions.assertEquals(0, lines.get("SKU-004").getAvailableQuantity());
        org.junit.jupiter.api.Assertions.assertEquals(0, lines.get("SKU-UNKNOWN").getAvailableQuantity());

        org.junit.jupiter.api.Assertions.assertEquals(before, stockReservationRepository.findAll().size());
        assertPosition("SKU-002", 20, 7);
        assertPosition("SKU-003", 2, 1);
        assertPosition("SKU-004", 0, 0);
    }

    private void assertPosition(String sku, int available, int reservedQty) {
        var position = context.inventoryPositionRepository().findAll().stream()
                .map(p -> (InventoryPosition) p)
                .filter(p -> sku.equals(p.getSku()))
                .findFirst()
                .orElseThrow();
        org.junit.jupiter.api.Assertions.assertEquals(available, position.getAvailableQuantity());
        org.junit.jupiter.api.Assertions.assertEquals(reservedQty, position.getReservedQuantity());
    }

    @Test
    void releaseStockTest() { // TODO: implement this test
    }

    @Test
    void confirmStockReservationTest() {
        var result = inventoryService.confirmStockReservation(new ConfirmStockReservationInput().setOrderId("ORD-003"));

        var now = context.clock.instant();
        org.junit.jupiter.api.Assertions.assertEquals(1, result.size());
        org.junit.jupiter.api.Assertions.assertEquals(ReservationStatus.CONFIRMED, result.get(0).getStatus());
        var saved = stockReservationRepository.findAll().stream()
                .filter(r -> "ORD-003".equals(r.getOrderId()))
                .toList();
        org.junit.jupiter.api.Assertions.assertEquals(1, saved.size());
        org.junit.jupiter.api.Assertions.assertEquals(ReservationStatus.CONFIRMED, saved.get(0).getStatus());
        org.junit.jupiter.api.Assertions.assertEquals(now, saved.get(0).getConfirmedAt());

        // SKU-002 20/7/6 -> 20/6/7
        var position = context.inventoryPositionRepository().findAll().stream()
                .map(p -> (InventoryPosition) p)
                .filter(p -> "SKU-002".equals(p.getSku()))
                .findFirst()
                .orElseThrow();
        org.junit.jupiter.api.Assertions.assertEquals(20, position.getAvailableQuantity());
        org.junit.jupiter.api.Assertions.assertEquals(6, position.getReservedQuantity());
        org.junit.jupiter.api.Assertions.assertEquals(7, position.getSoldQuantity());

        var events = context.eventPublisher().getEvents();
        org.junit.jupiter.api.Assertions.assertEquals(1, events.size());
        var event = (com.arcadiaeditions.catalog.inventory.domain.events.StockReservationConfirmed) events.get(0);
        org.junit.jupiter.api.Assertions.assertEquals("ORD-003", event.getOrderId());
        org.junit.jupiter.api.Assertions.assertEquals(now, event.getConfirmedAt());
        org.junit.jupiter.api.Assertions.assertEquals(1, event.getItems().size());
        org.junit.jupiter.api.Assertions.assertEquals("RES-004", event.getItems().get(0).getReservationId());
        org.junit.jupiter.api.Assertions.assertEquals("SKU-002", event.getItems().get(0).getSku());
        org.junit.jupiter.api.Assertions.assertEquals(1, event.getItems().get(0).getQuantity());
    }

    @Test
    void confirmStockReservationRedeliveryEmitsNothingTest() {
        var input = new ConfirmStockReservationInput().setOrderId("ORD-003");
        inventoryService.confirmStockReservation(input);
        context.eventPublisher().getEvents().clear();

        var result = inventoryService.confirmStockReservation(input);

        org.junit.jupiter.api.Assertions.assertTrue(result.isEmpty());
        org.junit.jupiter.api.Assertions.assertTrue(context.eventPublisher().getEvents().isEmpty());
        assertPosition("SKU-002", 20, 6);
    }
}
