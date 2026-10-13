package com.arcadiaeditions.catalog.products.config;

import com.arcadiaeditions.catalog.products.*;
import com.arcadiaeditions.catalog.products.domain.*;
import com.arcadiaeditions.catalog.products.domain.events.*;
import com.arcadiaeditions.config.TestDataLoader;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Services InMemory Config. It can be used standalone or with @SpringBootTest.
 */
@Configuration
@Profile("in-memory")
public class ServicesInMemoryConfig extends RepositoriesInMemoryConfig {

    private InMemoryEventPublisher eventPublisher = new InMemoryEventPublisher();
    protected final ProductCatalogServiceImpl productCatalogService =
            new ProductCatalogServiceImpl(productRepository(), eventPublisher);

    @Bean
    public <T extends ProductCatalogService> T productCatalogService() {
        return (T) productCatalogService;
    }

    static List<Product> _products;

    public void reloadTestData() {

        var testDataLoader = new TestDataLoader(List.of(Product.class));
        var products = _products != null ? _products : testDataLoader.loadCollectionTestDataAsObjects(Product.class);
        productRepository().deleteAll();
        productRepository().saveAll(products);
        eventPublisher.getEvents().clear();
    }

    @Bean
    public InMemoryEventPublisher eventPublisher() {
        return eventPublisher;
    }
}
