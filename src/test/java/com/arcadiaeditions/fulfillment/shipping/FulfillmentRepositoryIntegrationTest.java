package com.arcadiaeditions.fulfillment.shipping;

import com.arcadiaeditions.common.BaseRepositoryIntegrationTest;
import com.arcadiaeditions.config.TestClock;
import com.arcadiaeditions.fulfillment.shipping.domain.*;
import jakarta.persistence.EntityManager;
import java.time.*;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class FulfillmentRepositoryIntegrationTest extends BaseRepositoryIntegrationTest {

    @Autowired
    EntityManager entityManager;

    @Autowired
    FulfillmentRepository fulfillmentRepository;

    @Test
    void findAllTest() {
        var results = fulfillmentRepository.findAll();
        Assertions.assertFalse(results.isEmpty());
    }

    @Test
    void findByIdTest() {
        var id = 1L;
        var fulfillment = fulfillmentRepository.findById(id).orElseThrow();
        Assertions.assertNotNull(fulfillment.getId());
        Assertions.assertNotNull(fulfillment.getVersion());
    }

    @Test
    void saveTest() {
        Fulfillment fulfillment = new Fulfillment();
        fulfillment.setFulfillmentId("");
        fulfillment.setOrderId("");
        fulfillment.setCustomerId("");
        fulfillment.setStatus(FulfillmentStatus.values()[0]);
        fulfillment.setShippingAddress(new ShippingAddress());
        fulfillment.setItems(List.of(new FulfillmentItem()));
        fulfillment.setCarrierCode("");
        fulfillment.setTrackingNumber("");
        fulfillment.setScheduledAt(Instant.now());
        fulfillment.setReadyAt(Instant.now());
        fulfillment.setFailedAt(Instant.now());
        fulfillment.setFailureReason("");

        // Persist aggregate root
        var created = fulfillmentRepository.save(fulfillment);

        // reloading to get relationships persisted by id
        entityManager.flush();
        entityManager.refresh(created);
        Assertions.assertNotNull(created.getId());
        Assertions.assertNotNull(created.getVersion());
    }

    @Test
    void updateTest() {
        var id = 1L;
        var items = List.of(new FulfillmentItem());
        var address = new ShippingAddress();
        var now = TestClock.NOW;
        var fulfillment = fulfillmentRepository.findById(id).orElseThrow();
        fulfillment.setFulfillmentId("");
        fulfillment.setOrderId("");
        fulfillment.setCustomerId("");
        fulfillment.setStatus(FulfillmentStatus.values()[0]);
        fulfillment.setShippingAddress(address);
        fulfillment.setItems(items);
        fulfillment.setCarrierCode("");
        fulfillment.setTrackingNumber("");
        fulfillment.setScheduledAt(now);
        fulfillment.setReadyAt(now);
        fulfillment.setFailedAt(now);
        fulfillment.setFailureReason("");

        fulfillment = fulfillmentRepository.save(fulfillment);
        entityManager.flush();
        entityManager.clear();
        fulfillment = fulfillmentRepository.findById(id).orElseThrow();
        Assertions.assertEquals("", fulfillment.getFulfillmentId());
        Assertions.assertEquals("", fulfillment.getOrderId());
        Assertions.assertEquals("", fulfillment.getCustomerId());
        Assertions.assertEquals(FulfillmentStatus.values()[0], fulfillment.getStatus());
        Assertions.assertNotNull(fulfillment.getShippingAddress());
        Assertions.assertEquals(items.size(), fulfillment.getItems().size());
        Assertions.assertEquals("", fulfillment.getCarrierCode());
        Assertions.assertEquals("", fulfillment.getTrackingNumber());
        Assertions.assertEquals(now, fulfillment.getScheduledAt());
        Assertions.assertEquals(now, fulfillment.getReadyAt());
        Assertions.assertEquals(now, fulfillment.getFailedAt());
        Assertions.assertEquals("", fulfillment.getFailureReason());
    }

    @Test
    void deleteTest() {
        var id = 1L;
        fulfillmentRepository.deleteById(id);
        var notFound = fulfillmentRepository.findById(id);
        Assertions.assertFalse(notFound.isPresent());
    }
}
