package com.arcadiaeditions.catalog.inventory.config;

import com.arcadiaeditions.catalog.inventory.repository.jpa.*;
import com.arcadiaeditions.catalog.inventory.repository.jpa.inmemory.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

// @Configuration
public class RepositoriesInMemoryConfig {

    protected final StockReservationRepository stockReservationRepository = new StockReservationRepositoryInMemory();

    @Bean
    @Primary
    public <T extends StockReservationRepository> T stockReservationRepository() {
        return (T) stockReservationRepository;
    }

    protected final InventoryPositionRepository inventoryPositionRepository = new InventoryPositionRepositoryInMemory();

    @Bean
    @Primary
    public <T extends InventoryPositionRepository> T inventoryPositionRepository() {
        return (T) inventoryPositionRepository;
    }
}
