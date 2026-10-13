package com.arcadiaeditions.catalog.inventory.service.impl;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
 * Acceptance Test for InventoryBackofficeService.
 */
class InventoryBackofficeServiceTest {

    private final Logger log = LoggerFactory.getLogger(getClass());

    ServicesInMemoryConfig context = new ServicesInMemoryConfig();
    InventoryBackofficeServiceImpl inventoryBackofficeService = context.inventoryBackofficeService();

    InventoryPositionRepositoryInMemory inventoryPositionRepository = context.inventoryPositionRepository();

    @BeforeEach
    void setUp() {
        context.reloadTestData();
    }

    @Test
    void createInventoryPositionTest() {
        CreateInventoryPositionInput input = null; // TODO
        // TODO fill input data
        // input.setSku("");
        // input.setAvailableQuantity(0);
        // input.setReservedQuantity(0);
        // input.setSoldQuantity(0);
        var inventoryPosition = inventoryBackofficeService.createInventoryPosition(input);
        assertNotNull(inventoryPosition.getId());
        assertTrue(inventoryPositionRepository.containsEntity(inventoryPosition));
    }

    @Test
    void receiveStockTest() { // TODO: implement this test
    }

    @Test
    void adjustStockTest() { // TODO: implement this test
    }
}
