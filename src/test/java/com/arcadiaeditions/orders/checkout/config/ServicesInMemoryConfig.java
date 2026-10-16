package com.arcadiaeditions.orders.checkout.config;

import com.arcadiaeditions.config.TestDataLoader;
import com.arcadiaeditions.orders.checkout.core.application.*;
import com.arcadiaeditions.orders.checkout.core.domain.*;
import com.arcadiaeditions.orders.checkout.core.domain.events.*;
import com.arcadiaeditions.orders.checkout.core.inbound.*;
import com.arcadiaeditions.orders.checkout.infrastructure.events.*;
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
    private final com.arcadiaeditions.catalog.inventory.config.ServicesInMemoryConfig inventoryContext =
            new com.arcadiaeditions.catalog.inventory.config.ServicesInMemoryConfig();
    protected final OrdersCheckoutServiceImpl ordersCheckoutService = new OrdersCheckoutServiceImpl(
            orderRepository(),
            eventPublisher,
            inventoryContext.<com.arcadiaeditions.catalog.inventory.service.InventoryService>inventoryService(),
            Clock.fixed(Instant.parse("2026-10-14T10:00:00Z"), ZoneOffset.UTC));

    public com.arcadiaeditions.catalog.inventory.config.ServicesInMemoryConfig getInventoryContext() {
        return inventoryContext;
    }

    @Bean
    public <T extends OrdersCheckoutService> T ordersCheckoutService() {
        return (T) ordersCheckoutService;
    }

    static List<Order> _orders;

    public void reloadTestData() {

        var testDataLoader = new TestDataLoader(List.of(Order.class, OrderItem.class, ShippingAddress.class));
        var orders = _orders != null ? _orders : testDataLoader.loadCollectionTestDataAsObjects(Order.class);
        orderRepository().deleteAll();
        orderRepository().saveAll(orders);
        inventoryContext.reloadTestData();
        eventPublisher.getEvents().clear();
    }

    @Bean
    public InMemoryEventPublisher eventPublisher() {
        return eventPublisher;
    }
}
