package com.arcadiaeditions.catalog.inventory.config;

import com.arcadiaeditions.catalog.inventory.domain.*;
import com.arcadiaeditions.catalog.inventory.domain.events.*;
import com.arcadiaeditions.catalog.inventory.events.*;
import com.arcadiaeditions.catalog.inventory.service.*;
import com.arcadiaeditions.catalog.inventory.service.impl.*;
import com.arcadiaeditions.config.TestDataLoader;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
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
    protected final InventoryServiceImpl inventoryService = new InventoryServiceImpl(
            stockReservationRepository(),
            inventoryPositionRepository(),
            Clock.fixed(Instant.parse("2026-10-14T10:00:00Z"), ZoneOffset.UTC),
            eventPublisher);
    protected final InventoryBackofficeServiceImpl inventoryBackofficeService =
            new InventoryBackofficeServiceImpl(inventoryPositionRepository(), eventPublisher);

    @Bean
    public <T extends InventoryService> T inventoryService() {
        return (T) inventoryService;
    }

    @Bean
    public <T extends InventoryBackofficeService> T inventoryBackofficeService() {
        return (T) inventoryBackofficeService;
    }

    static List<StockReservation> _stockReservations;
    static List<InventoryPosition> _inventoryPositions;

    public void reloadTestData() {

        var testDataLoader = new TestDataLoader(List.of(StockReservation.class, InventoryPosition.class));
        var stockReservations = _stockReservations != null
                ? _stockReservations
                : testDataLoader.loadCollectionTestDataAsObjects(StockReservation.class);
        stockReservationRepository().deleteAll();
        stockReservationRepository().saveAll(stockReservations);
        var inventoryPositions = _inventoryPositions != null
                ? _inventoryPositions
                : testDataLoader.loadCollectionTestDataAsObjects(InventoryPosition.class);
        inventoryPositionRepository().deleteAll();
        inventoryPositionRepository().saveAll(inventoryPositions);
        eventPublisher.getEvents().clear();
    }

    @Bean
    public InMemoryEventPublisher eventPublisher() {
        return eventPublisher;
    }
}
