package com.arcadiaeditions.orders.checkout.config;

import com.arcadiaeditions.orders.checkout.core.outbound.jpa.*;
import com.arcadiaeditions.orders.checkout.infrastructure.jpa.inmemory.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

// @Configuration
public class RepositoriesInMemoryConfig {

    protected final OrderRepository orderRepository = new OrderRepositoryInMemory();

    @Bean
    @Primary
    public <T extends OrderRepository> T orderRepository() {
        return (T) orderRepository;
    }
}
