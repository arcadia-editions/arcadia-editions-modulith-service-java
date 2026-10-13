package com.arcadiaeditions.catalog.inventory.repository.jpa;

import com.arcadiaeditions.catalog.inventory.domain.*;
import com.arcadiaeditions.common.BaseRepositoryIntegrationTest;
import jakarta.persistence.EntityManager;
import java.time.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class InventoryPositionRepositoryIntegrationTest extends BaseRepositoryIntegrationTest {

    @Autowired
    EntityManager entityManager;

    @Autowired
    InventoryPositionRepository inventoryPositionRepository;

    @Test
    void findAllTest() {
        var results = inventoryPositionRepository.findAll();
        Assertions.assertFalse(results.isEmpty());
    }

    @Test
    void findByIdTest() {
        var id = 1L;
        var inventoryPosition = inventoryPositionRepository.findById(id).orElseThrow();
        Assertions.assertNotNull(inventoryPosition.getId());
        Assertions.assertNotNull(inventoryPosition.getVersion());
    }

    @Test
    void saveTest() {
        InventoryPosition inventoryPosition = new InventoryPosition();
        inventoryPosition.setSku("");
        inventoryPosition.setAvailableQuantity(0);
        inventoryPosition.setReservedQuantity(0);
        inventoryPosition.setSoldQuantity(0);

        // Persist aggregate root
        var created = inventoryPositionRepository.save(inventoryPosition);

        // reloading to get relationships persisted by id
        entityManager.flush();
        entityManager.refresh(created);
        Assertions.assertNotNull(created.getId());
        Assertions.assertNotNull(created.getVersion());
    }

    @Test
    void updateTest() {
        var id = 1L;
        var inventoryPosition = inventoryPositionRepository.findById(id).orElseThrow();
        inventoryPosition.setSku("");
        inventoryPosition.setAvailableQuantity(0);
        inventoryPosition.setReservedQuantity(0);
        inventoryPosition.setSoldQuantity(0);

        inventoryPosition = inventoryPositionRepository.save(inventoryPosition);
        Assertions.assertEquals("", inventoryPosition.getSku());
        Assertions.assertEquals(0, inventoryPosition.getAvailableQuantity());
        Assertions.assertEquals(0, inventoryPosition.getReservedQuantity());
        Assertions.assertEquals(0, inventoryPosition.getSoldQuantity());
    }

    @Test
    void deleteTest() {
        var id = 1L;
        inventoryPositionRepository.deleteById(id);
        var notFound = inventoryPositionRepository.findById(id);
        Assertions.assertFalse(notFound.isPresent());
    }
}
