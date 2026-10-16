package com.arcadiaeditions.orders.checkout.core.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

import com.arcadiaeditions.catalog.inventory.domain.ReservationStatus;
import com.arcadiaeditions.orders.checkout.config.*;
import com.arcadiaeditions.orders.checkout.core.application.mappers.*;
import com.arcadiaeditions.orders.checkout.core.domain.*;
import com.arcadiaeditions.orders.checkout.core.domain.events.OrderCancelled;
import com.arcadiaeditions.orders.checkout.core.domain.events.OrderConfirmed;
import com.arcadiaeditions.orders.checkout.core.domain.events.OrderCreated;
import com.arcadiaeditions.orders.checkout.core.domain.events.StockUnavailable;
import com.arcadiaeditions.orders.checkout.core.inbound.*;
import com.arcadiaeditions.orders.checkout.core.inbound.dtos.*;
import com.arcadiaeditions.orders.checkout.core.outbound.jpa.*;
import com.arcadiaeditions.orders.checkout.infrastructure.jpa.inmemory.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.List;
import java.util.NoSuchElementException;
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

    private static final Instant NOW = Instant.parse("2026-10-14T10:00:00Z");

    @Test
    void startOrderCheckoutTest() {
        var input = new StartOrderCheckoutInput()
                .setCustomerId("CUST-100")
                .setCustomerEmail("buyer@example.com")
                .setPaymentMethodId("pm_ok")
                .setCurrency("EUR")
                .addItems(new OrderItemInput().setSku("SKU-001").setQuantity(2).setUnitPrice(new BigDecimal("10.50")))
                .addItems(new OrderItemInput().setSku("SKU-002").setQuantity(1).setUnitPrice(new BigDecimal("4.00")))
                .setShippingAddress(new ShippingAddressInput()
                        .setName("Ada")
                        .setLine1("Calle 1")
                        .setCity("Madrid")
                        .setPostalCode("28001")
                        .setCountry("ES"));

        var order = ordersCheckoutService.startOrderCheckout(input);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CREATED);
        assertThat(order.getOrderId()).startsWith("ORD-");
        assertThat(order.getTotalAmount()).isEqualByComparingTo("25.00");
        assertThat(order.getCurrency()).isEqualTo("EUR");
        assertThat(order.getCreatedAt()).isEqualTo(NOW);
        assertThat(order.getConfirmedAt()).isNull();
        var saved = orderRepository.findByOrderId(order.getOrderId());
        assertThat(saved).isPresent();
        assertThat(saved.get().getStatus()).isEqualTo(OrderStatus.CREATED);

        var inventory = context.getInventoryContext();
        var reservations = inventory
                .stockReservationRepository()
                .findByOrderIdAndStatusIn(order.getOrderId(), List.of(ReservationStatus.RESERVED));
        assertThat(reservations).hasSize(2);
        var sku1 = inventory.inventoryPositionRepository().findAll().stream()
                .filter(p -> "SKU-001".equals(p.getSku()))
                .findFirst()
                .orElseThrow();
        assertThat(sku1.getAvailableQuantity()).isEqualTo(7);

        var created = context.eventPublisher().getEvents().stream()
                .filter(OrderCreated.class::isInstance)
                .map(OrderCreated.class::cast)
                .toList();
        assertThat(created).hasSize(1);
        assertThat(created.get(0).getOrderId()).isEqualTo(order.getOrderId());
        assertThat(created.get(0).getAmount()).isEqualByComparingTo("25.00");
    }

    @Test
    void startOrderCheckoutStockUnavailableTest() {
        var input = new StartOrderCheckoutInput()
                .setCustomerId("CUST-100")
                .setCustomerEmail("buyer@example.com")
                .setPaymentMethodId("pm_ok")
                .setCurrency("EUR")
                .addItems(new OrderItemInput().setSku("SKU-001").setQuantity(2).setUnitPrice(new BigDecimal("10.50")))
                .addItems(new OrderItemInput().setSku("SKU-003").setQuantity(3).setUnitPrice(new BigDecimal("4.00")))
                .setShippingAddress(new ShippingAddressInput()
                        .setName("Ada")
                        .setLine1("Calle 1")
                        .setCity("Madrid")
                        .setPostalCode("28001")
                        .setCountry("ES"));
        long ordersBefore = orderRepository.findAll().size();

        var thrown = org.junit.jupiter.api.Assertions.assertThrows(
                StockUnavailableException.class, () -> ordersCheckoutService.startOrderCheckout(input));

        var event = thrown.getStockUnavailable();
        assertThat(thrown.getMessage()).isEqualTo("Some items are not available in the requested quantity");
        assertThat(event.getOrderId()).startsWith("ORD-");
        assertThat(event.getCustomerId()).isEqualTo("CUST-100");
        assertThat(event.getCustomerEmail()).isEqualTo("buyer@example.com");
        assertThat(event.getUnavailableItems()).hasSize(1);
        assertThat(event.getUnavailableItems().get(0).getSku()).isEqualTo("SKU-003");
        assertThat(event.getUnavailableItems().get(0).getRequestedQuantity()).isEqualTo(3);
        assertThat(event.getUnavailableItems().get(0).getAvailableQuantity()).isEqualTo(2);
        assertThat(orderRepository.findAll()).hasSize((int) ordersBefore);
        assertThat(orderRepository.findByOrderId(event.getOrderId())).isEmpty();
        var events = context.eventPublisher().getEvents();
        assertThat(events.stream().filter(StockUnavailable.class::isInstance)).hasSize(1);
        assertThat(events.stream().filter(OrderCreated.class::isInstance)).isEmpty();
    }

    @Test
    void confirmOrderTest() {
        var order = ordersCheckoutService.confirmOrder(new ConfirmOrderInput().setOrderId("ORD-003"));

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(order.getConfirmedAt()).isEqualTo(NOW);
        assertThat(orderRepository.findByOrderId("ORD-003").orElseThrow().getStatus())
                .isEqualTo(OrderStatus.CONFIRMED);
        var reservations = context.getInventoryContext()
                .stockReservationRepository()
                .findByOrderIdAndStatusIn("ORD-003", List.of(ReservationStatus.CONFIRMED));
        assertThat(reservations).hasSize(1);
        assertThat(context.eventPublisher().getEvents().stream().filter(OrderConfirmed.class::isInstance))
                .hasSize(1);
    }

    @Test
    void confirmOrderRedeliveryTest() {
        var order = ordersCheckoutService.confirmOrder(new ConfirmOrderInput().setOrderId("ORD-001"));

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(context.eventPublisher().getEvents()).isEmpty();
    }

    @Test
    void confirmOrderNotFoundTest() {
        var input = new ConfirmOrderInput().setOrderId("ORD-UNKNOWN");
        assertThatThrownBy(() -> ordersCheckoutService.confirmOrder(input)).isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void cancelOrderFromCreatedTest() {
        var order = ordersCheckoutService.cancelOrder(
                new CancelOrderInput().setOrderId("ORD-008").setReason("OUT_OF_STOCK"));

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(order.getCancelledAt()).isEqualTo(NOW);
        assertThat(order.getCancellationReason()).isEqualTo("OUT_OF_STOCK");
        assertThat(orderRepository.findByOrderId("ORD-008").orElseThrow().getStatus())
                .isEqualTo(OrderStatus.CANCELLED);
        var cancelled = context.eventPublisher().getEvents().stream()
                .filter(OrderCancelled.class::isInstance)
                .map(OrderCancelled.class::cast)
                .toList();
        assertThat(cancelled).hasSize(1);
        assertThat(cancelled.get(0).getOrderId()).isEqualTo("ORD-008");
        assertThat(cancelled.get(0).getReason()).isEqualTo("OUT_OF_STOCK");
    }

    @Test
    void cancelOrderFromConfirmedTest() {
        var order = ordersCheckoutService.cancelOrder(
                new CancelOrderInput().setOrderId("ORD-004").setReason("EXPIRED"));

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(order.getCancelledAt()).isEqualTo(NOW);
        assertThat(order.getCancellationReason()).isEqualTo("EXPIRED");
        var cancelled = context.eventPublisher().getEvents().stream()
                .filter(OrderCancelled.class::isInstance)
                .map(OrderCancelled.class::cast)
                .toList();
        assertThat(cancelled).hasSize(1);
        assertThat(cancelled.get(0).getReason()).isEqualTo("EXPIRED");
    }

    @Test
    void cancelOrderRedeliveryTest() {
        var order = ordersCheckoutService.cancelOrder(
                new CancelOrderInput().setOrderId("ORD-013").setReason("OUT_OF_STOCK"));

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(context.eventPublisher().getEvents()).isEmpty();
    }

    @Test
    void cancelOrderNotFoundTest() {
        var input = new CancelOrderInput().setOrderId("ORD-UNKNOWN").setReason("OUT_OF_STOCK");
        assertThatThrownBy(() -> ordersCheckoutService.cancelOrder(input)).isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void getOrderTest() {
        Long id = 1L;
        var order = ordersCheckoutService.getOrder(id);
        assertTrue(order.isPresent());
    }
}
