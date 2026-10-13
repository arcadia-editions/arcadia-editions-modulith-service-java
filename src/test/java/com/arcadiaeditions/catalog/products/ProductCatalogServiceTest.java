package com.arcadiaeditions.catalog.products;

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
        Long id = 1L;
        UpdateProductInput input = null; // TODO
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
        // assertTrue(productRepository.containsKey(id));
        var product = productCatalogService.updateProduct(id, input);
        assertNotNull(product);
        assertTrue(productRepository.containsEntity(product));
    }

    @Test
    void publishProductTest() { // TODO: implement this test
    }

    @Test
    void retireProductTest() { // TODO: implement this test
    }
}
