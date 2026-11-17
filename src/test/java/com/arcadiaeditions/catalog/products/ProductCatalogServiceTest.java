package com.arcadiaeditions.catalog.products;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

import com.arcadiaeditions.catalog.products.config.*;
import com.arcadiaeditions.catalog.products.domain.*;
import com.arcadiaeditions.catalog.products.dtos.*;
import com.arcadiaeditions.catalog.products.inmemory.*;
import com.arcadiaeditions.catalog.products.mappers.*;
import java.time.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Acceptance Test for ProductCatalogService.
 */
class ProductCatalogServiceTest {

    private final Logger log = LoggerFactory.getLogger(getClass());

    ServicesInMemoryConfig context = new ServicesInMemoryConfig();
    ProductCatalogServiceImpl productCatalogService = context.productCatalogService();

    ProductRepositoryInMemory productRepository = context.productRepository();

    @BeforeEach
    void setUp() {
        context.reloadTestData();
    }

    @Test
    void createProductTest() {
        CreateProductInput input = null; // TODO
        // TODO fill input data
        // input.setSku("");
        // input.setName("");
        // input.setDescription("");
        // input.setPrice(BigDecimal.valueOf(0));
        // input.setCurrency("");
        // input.setEditionSize(0);
        // input.setTrackingMode(InventoryTrackingMode.values()[0]);
        // input.setLaunchAt(Instant.now());
        // input.setStatus(ProductStatus.values()[0]);
        var product = productCatalogService.createProduct(input);
        assertNotNull(product.getId());
        assertTrue(productRepository.containsEntity(product));
    }

    @Test
    void updateProductTest() {
        Long id = 1L; // fixtures: Product/1.json, SKU-001, PUBLISHED
        assertTrue(productRepository.containsKey(id));
        var input = new UpdateProductInput()
                .setName("Arcadia First Edition (revised)")
                .setDescription("Revised numbered first edition")
                .setPrice(new java.math.BigDecimal("59.99"))
                .setCurrency("EUR")
                .setLaunchAt(Instant.parse("2026-12-01T10:00:00Z"));
        var product = productCatalogService.updateProduct(id, input);
        assertNotNull(product);
        assertEquals("SKU-001", product.getSku());
        assertEquals("Arcadia First Edition (revised)", product.getName());
        assertEquals("Revised numbered first edition", product.getDescription());
        assertEquals(0, new java.math.BigDecimal("59.99").compareTo(product.getPrice()));
        assertEquals(Instant.parse("2026-12-01T10:00:00Z"), product.getLaunchAt());
        assertEquals(ProductStatus.DRAFT, product.getStatus());
        assertTrue(productRepository.containsEntity(product));
    }

    @Test
    void publishProductTest() { // TODO: implement this test
    }

    @Test
    void retireProductTest() { // TODO: implement this test
    }
}
