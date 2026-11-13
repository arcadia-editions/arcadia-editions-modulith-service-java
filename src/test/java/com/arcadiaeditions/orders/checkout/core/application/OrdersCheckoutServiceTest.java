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
    void confirmOrderTest() {
        var order = ordersCheckoutService.confirmOrder(new ConfirmOrderInput().setOrderId("ORD-003"));

        org.junit.jupiter.api.Assertions.assertEquals(OrderStatus.CONFIRMED, order.getStatus());
        org.junit.jupiter.api.Assertions.assertEquals(context.clock.instant(), order.getConfirmedAt());

        var reservations = context.inventoryContext.stockReservationRepository().findByOrderId("ORD-003");
        org.junit.jupiter.api.Assertions.assertFalse(reservations.isEmpty());
        reservations.forEach(r -> org.junit.jupiter.api.Assertions.assertEquals(
                com.arcadiaeditions.catalog.inventory.domain.ReservationStatus.CONFIRMED, r.getStatus()));

        var events = context.eventPublisher().getEvents();
        org.junit.jupiter.api.Assertions.assertEquals(1, events.size());
        var confirmed = (com.arcadiaeditions.orders.checkout.core.domain.events.OrderConfirmed) events.get(0);
        org.junit.jupiter.api.Assertions.assertEquals("ORD-003", confirmed.getOrderId());
        org.junit.jupiter.api.Assertions.assertEquals("CUST-003", confirmed.getCustomerId());
        org.junit.jupiter.api.Assertions.assertEquals(context.clock.instant(), confirmed.getConfirmedAt());
        org.junit.jupiter.api.Assertions.assertEquals(1, confirmed.getItems().size());
        org.junit.jupiter.api.Assertions.assertEquals("SKU-002", confirmed.getItems().get(0).getSku());
        org.junit.jupiter.api.Assertions.assertEquals(1, confirmed.getItems().get(0).getQuantity());
        org.junit.jupiter.api.Assertions.assertEquals("1 Book Lane", confirmed.getShippingAddress().getLine1());
        org.junit.jupiter.api.Assertions.assertEquals("28003", confirmed.getShippingAddress().getPostalCode());
    }

    @Test
    void confirmOrderAlreadyConfirmedRedeliveryTest() {
        // ORD-001: already CONFIRMED; redelivery is a no-op, unchanged, nothing emitted
        var before = orderRepository.findByOrderId("ORD-001").orElseThrow().getConfirmedAt();

        var order = ordersCheckoutService.confirmOrder(new ConfirmOrderInput().setOrderId("ORD-001"));

        org.junit.jupiter.api.Assertions.assertEquals(OrderStatus.CONFIRMED, order.getStatus());
        org.junit.jupiter.api.Assertions.assertEquals(before, order.getConfirmedAt());
        org.junit.jupiter.api.Assertions.assertTrue(context.eventPublisher().getEvents().isEmpty());
    }

    @Test
    void confirmOrderFromCancelledIsInvalidTest() {
        org.junit.jupiter.api.Assertions.assertThrows(
                OrderTransitions.InvalidStateTransitionException.class,
                () -> ordersCheckoutService.confirmOrder(new ConfirmOrderInput().setOrderId("ORD-013")));
        org.junit.jupiter.api.Assertions.assertTrue(context.eventPublisher().getEvents().isEmpty());
    }

    @Test
    void confirmOrderNotFoundTest() {
        org.junit.jupiter.api.Assertions.assertThrows(
                java.util.NoSuchElementException.class,
                () -> ordersCheckoutService.confirmOrder(new ConfirmOrderInput().setOrderId("ORD-999")));
    }

    @Test
    void cancelOrderFromCreatedTest() { // ORD-018
        var order = ordersCheckoutService.cancelOrder(
                new CancelOrderInput().setOrderId("ORD-018").setReason("PAYMENT_DECLINED"));

        assertCancelled(order, "ORD-018", "CUST-018", "PAYMENT_DECLINED");
    }

    @Test
    void cancelOrderFromConfirmedTest() { // ORD-017
        var order = ordersCheckoutService.cancelOrder(
                new CancelOrderInput().setOrderId("ORD-017").setReason("PAYMENT_VOIDED"));

        assertCancelled(order, "ORD-017", "CUST-017", "PAYMENT_VOIDED");
    }

    private void assertCancelled(Order order, String orderId, String customerId, String reason) {
        org.junit.jupiter.api.Assertions.assertEquals(OrderStatus.CANCELLED, order.getStatus());
        org.junit.jupiter.api.Assertions.assertEquals(reason, order.getCancellationReason());
        org.junit.jupiter.api.Assertions.assertEquals(context.clock.instant(), order.getCancelledAt());
        var saved = orderRepository.findByOrderId(orderId).orElseThrow();
        org.junit.jupiter.api.Assertions.assertEquals(OrderStatus.CANCELLED, saved.getStatus());

        var events = context.eventPublisher().getEvents();
        org.junit.jupiter.api.Assertions.assertEquals(1, events.size());
        var cancelled = (com.arcadiaeditions.orders.checkout.core.domain.events.OrderCancelled) events.get(0);
        org.junit.jupiter.api.Assertions.assertEquals(orderId, cancelled.getOrderId());
        org.junit.jupiter.api.Assertions.assertEquals(customerId, cancelled.getCustomerId());
        org.junit.jupiter.api.Assertions.assertEquals(customerId.toLowerCase().replace("cust-", "customer") + "@example.com", cancelled.getCustomerEmail());
        org.junit.jupiter.api.Assertions.assertEquals(reason, cancelled.getReason());
        org.junit.jupiter.api.Assertions.assertEquals(context.clock.instant(), cancelled.getCancelledAt());
    }

    @Test
    void cancelOrderAlreadyCancelledIsNoOpTest() {
        var input = new CancelOrderInput().setOrderId("ORD-018").setReason("PAYMENT_DECLINED");
        ordersCheckoutService.cancelOrder(input);
        context.eventPublisher().getEvents().clear();

        var order = ordersCheckoutService.cancelOrder(input);

        org.junit.jupiter.api.Assertions.assertEquals(OrderStatus.CANCELLED, order.getStatus());
        org.junit.jupiter.api.Assertions.assertTrue(context.eventPublisher().getEvents().isEmpty());
    }

    @Test
    void cancelOrderNotFoundTest() {
        org.junit.jupiter.api.Assertions.assertThrows(
                java.util.NoSuchElementException.class,
                () -> ordersCheckoutService.cancelOrder(
                        new CancelOrderInput().setOrderId("ORD-999").setReason("PAYMENT_DECLINED")));
    }

    @Test
    void getOrderTest() {
        Long id = 1L; // fixtures: Order/1.json, ORD-001
        var order = ordersCheckoutService.getOrder(id);
        assertTrue(order.isPresent());
        org.junit.jupiter.api.Assertions.assertEquals("ORD-001", order.get().getOrderId());
        org.junit.jupiter.api.Assertions.assertEquals(OrderStatus.CONFIRMED, order.get().getStatus());
    }

    @Test
    void getOrderNotFoundTest() {
        assertTrue(ordersCheckoutService.getOrder(999999L).isEmpty());
    }
}
