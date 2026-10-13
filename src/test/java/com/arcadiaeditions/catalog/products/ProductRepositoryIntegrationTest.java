package com.arcadiaeditions.catalog.products;

import com.arcadiaeditions.catalog.products.domain.*;
import com.arcadiaeditions.common.BaseRepositoryIntegrationTest;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class ProductRepositoryIntegrationTest extends BaseRepositoryIntegrationTest {

    @Autowired
    EntityManager entityManager;

    @Autowired
    ProductRepository productRepository;

    @Test
    void findAllTest() {
        var results = productRepository.findAll();
        Assertions.assertFalse(results.isEmpty());
    }

    @Test
    void findByIdTest() {
        var id = 1L;
        var product = productRepository.findById(id).orElseThrow();
        Assertions.assertNotNull(product.getId());
        Assertions.assertNotNull(product.getVersion());
    }

    @Test
    void saveTest() {
        Product product = new Product();
        product.setSku("");
        product.setName("");
        product.setDescription("");
        product.setPrice(BigDecimal.valueOf(0));
        product.setCurrency("");
        product.setEditionSize(0);
        product.setTrackingMode(InventoryTrackingMode.values()[0]);
        product.setLaunchAt(Instant.now());
        product.setStatus(ProductStatus.values()[0]);

        // Persist aggregate root
        var created = productRepository.save(product);

        // reloading to get relationships persisted by id
        entityManager.flush();
        entityManager.refresh(created);
        Assertions.assertNotNull(created.getId());
        Assertions.assertNotNull(created.getVersion());
    }

    @Test
    void updateTest() {
        var id = 1L;
        var product = productRepository.findById(id).orElseThrow();
        product.setSku("");
        product.setName("");
        product.setDescription("");
        product.setPrice(BigDecimal.valueOf(0));
        product.setCurrency("");
        product.setEditionSize(0);
        product.setTrackingMode(InventoryTrackingMode.values()[0]);
        product.setLaunchAt(Instant.now());
        product.setStatus(ProductStatus.values()[0]);

        product = productRepository.save(product);
        Assertions.assertEquals("", product.getSku());
        Assertions.assertEquals("", product.getName());
        Assertions.assertEquals("", product.getDescription());
        Assertions.assertEquals(BigDecimal.valueOf(0), product.getPrice());
        Assertions.assertEquals("", product.getCurrency());
        Assertions.assertEquals(0, product.getEditionSize());
        Assertions.assertEquals(InventoryTrackingMode.values()[0], product.getTrackingMode());
        Assertions.assertEquals(Instant.now(), product.getLaunchAt());
        Assertions.assertEquals(ProductStatus.values()[0], product.getStatus());
    }

    @Test
    void deleteTest() {
        var id = 1L;
        productRepository.deleteById(id);
        var notFound = productRepository.findById(id);
        Assertions.assertFalse(notFound.isPresent());
    }
}
