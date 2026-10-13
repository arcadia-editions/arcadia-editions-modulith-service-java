package com.arcadiaeditions.catalog.products.config;

import com.arcadiaeditions.catalog.products.*;
import com.arcadiaeditions.catalog.products.inmemory.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

// @Configuration
public class RepositoriesInMemoryConfig {

    protected final ProductRepository productRepository = new ProductRepositoryInMemory();

    @Bean
    @Primary
    public <T extends ProductRepository> T productRepository() {
        return (T) productRepository;
    }
}
