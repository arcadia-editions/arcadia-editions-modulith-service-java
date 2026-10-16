package com.arcadiaeditions.catalog.inventory.repository.jpa;

import com.arcadiaeditions.catalog.inventory.domain.*;
import com.arcadiaeditions.common.BaseRepositoryIntegrationTest;
import jakarta.persistence.EntityManager;
import java.time.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class StockReservationRepositoryIntegrationTest extends BaseRepositoryIntegrationTest {

    @Autowired
    EntityManager entityManager;

    @Autowired
    StockReservationRepository stockReservationRepository;

    @Test
    void findAllTest() {
        var results = stockReservationRepository.findAll();
        Assertions.assertFalse(results.isEmpty());
    }

    @Test
    void findByIdTest() {
        var id = 1L;
        var stockReservation = stockReservationRepository.findById(id).orElseThrow();
        Assertions.assertNotNull(stockReservation.getId());
        Assertions.assertNotNull(stockReservation.getVersion());
    }

    @Test
    void saveTest() {
        StockReservation stockReservation = new StockReservation();
        stockReservation.setReservationId("");
        stockReservation.setOrderId("");
        stockReservation.setSku("");
        stockReservation.setQuantity(0);
        stockReservation.setStatus(ReservationStatus.values()[0]);
        stockReservation.setReservedAt(Instant.now());
        stockReservation.setExpiresAt(Instant.now());
        stockReservation.setConfirmedAt(Instant.now());
        stockReservation.setReleasedAt(Instant.now());
        stockReservation.setReleaseReason(StockReleaseReason.values()[0]);

        // Persist aggregate root
        var created = stockReservationRepository.save(stockReservation);

        // reloading to get relationships persisted by id
        entityManager.flush();
        entityManager.refresh(created);
        Assertions.assertNotNull(created.getId());
        Assertions.assertNotNull(created.getVersion());
    }

    @Test
    void updateTest() {
        var id = 1L;
        var now = Instant.now();
        var stockReservation = stockReservationRepository.findById(id).orElseThrow();
        stockReservation.setReservationId("");
        stockReservation.setOrderId("");
        stockReservation.setSku("");
        stockReservation.setQuantity(0);
        stockReservation.setStatus(ReservationStatus.values()[0]);
        stockReservation.setReservedAt(now);
        stockReservation.setExpiresAt(now);
        stockReservation.setConfirmedAt(now);
        stockReservation.setReleasedAt(now);
        stockReservation.setReleaseReason(StockReleaseReason.values()[0]);

        stockReservation = stockReservationRepository.save(stockReservation);
        Assertions.assertEquals("", stockReservation.getReservationId());
        Assertions.assertEquals("", stockReservation.getOrderId());
        Assertions.assertEquals("", stockReservation.getSku());
        Assertions.assertEquals(0, stockReservation.getQuantity());
        Assertions.assertEquals(ReservationStatus.values()[0], stockReservation.getStatus());
        Assertions.assertEquals(now, stockReservation.getReservedAt());
        Assertions.assertEquals(now, stockReservation.getExpiresAt());
        Assertions.assertEquals(now, stockReservation.getConfirmedAt());
        Assertions.assertEquals(now, stockReservation.getReleasedAt());
        Assertions.assertEquals(StockReleaseReason.values()[0], stockReservation.getReleaseReason());
    }

    @Test
    void deleteTest() {
        var id = 1L;
        stockReservationRepository.deleteById(id);
        var notFound = stockReservationRepository.findById(id);
        Assertions.assertFalse(notFound.isPresent());
    }
}
