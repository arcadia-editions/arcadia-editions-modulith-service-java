package com.arcadiaeditions.orders.checkout.core.application;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

import com.arcadiaeditions.orders.checkout.config.*;
import com.arcadiaeditions.orders.checkout.core.application.mappers.*;
import com.arcadiaeditions.orders.checkout.core.domain.*;
import com.arcadiaeditions.orders.checkout.core.inbound.*;
import com.arcadiaeditions.orders.checkout.core.inbound.dtos.*;
import com.arcadiaeditions.orders.checkout.core.outbound.jpa.*;
import com.arcadiaeditions.orders.checkout.infrastructure.jpa.inmemory.*;
import java.time.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Acceptance Test for OrdersCheckoutService.
 */
class OrdersCheckoutServiceTest {

    private final Logger log = LoggerFactory.getLogger(getClass());

    ServicesInMemoryConfig context = new ServicesInMemoryConfig();
    OrdersCheckoutServiceImpl ordersCheckoutService = context.ordersCheckoutService();

    OrderRepositoryInMemory orderRepository = context.orderRepository();

    @BeforeEach
    void setUp() {
        context.reloadTestData();
    }

    @Test
    void startOrderCheckoutTest() {
        var input = new StartOrderCheckoutInput()
                .setCustomerId("CUST-009")
                .setCustomerEmail("buyer@example.com")
                .setPaymentMethodId("PM-009")
                .setCurrency("EUR")
                .setItems(java.util.List.of(
                        new OrderItemInput().setSku("SKU-001").setQuantity(2).setUnitPrice(new java.math.BigDecimal("49.99")),
                        new OrderItemInput().setSku("SKU-002").setQuantity(1).setUnitPrice(new java.math.BigDecimal("10.00"))))
                .setShippingAddress(new ShippingAddressInput()
                        .setName("Buyer")
                        .setLine1("1 Book Lane")
                        .setCity("Madrid")
                        .setPostalCode("28001")
                        .setCountry("ES"));

        var order = ordersCheckoutService.startOrderCheckout(input);

        org.junit.jupiter.api.Assertions.assertTrue(order.getOrderId().matches("ORD-.+"));
        org.junit.jupiter.api.Assertions.assertEquals(OrderStatus.CREATED, order.getStatus());
        org.junit.jupiter.api.Assertions.assertEquals(
                0, new java.math.BigDecimal("109.98").compareTo(order.getTotalAmount()));
        org.junit.jupiter.api.Assertions.assertEquals(context.clock.instant(), order.getCreatedAt());
        org.junit.jupiter.api.Assertions.assertEquals(2, order.getItems().size());
        org.junit.jupiter.api.Assertions.assertTrue(
                orderRepository.findAll().stream().anyMatch(o -> o.getOrderId().equals(order.getOrderId())));

        var events = context.eventPublisher().getEvents();
        org.junit.jupiter.api.Assertions.assertEquals(1, events.size());
        var created = (com.arcadiaeditions.orders.checkout.core.domain.events.OrderCreated) events.get(0);
        org.junit.jupiter.api.Assertions.assertEquals(order.getOrderId(), created.getOrderId());
        org.junit.jupiter.api.Assertions.assertEquals(
                0, new java.math.BigDecimal("109.98").compareTo(created.getAmount()));
        org.junit.jupiter.api.Assertions.assertEquals("EUR", created.getCurrency());
        org.junit.jupiter.api.Assertions.assertEquals("PM-009", created.getPaymentMethodId());

        var position = context.inventoryContext.inventoryPositionRepository().findBySkuForUpdate("SKU-001").orElseThrow();
        org.junit.jupiter.api.Assertions.assertEquals(7, position.getAvailableQuantity());
    }

    @Test
    void confirmOrderTest() { // TODO: implement this test
    }

    @Test
    void cancelOrderTest() { // TODO: implement this test
    }

    @Test
    void getOrderTest() {
        Long id = null;
        var order = ordersCheckoutService.getOrder(id);
        assertTrue(order.isPresent());
    }
}
