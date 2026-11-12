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
    void confirmStockReservationTest() { // TODO: implement this test
    }
}
