package com.arcadiaeditions.orders.checkout.infrastructure.jpa;

import com.arcadiaeditions.common.BaseRepositoryIntegrationTest;
import com.arcadiaeditions.orders.checkout.core.domain.*;
import com.arcadiaeditions.orders.checkout.core.outbound.jpa.OrderRepository;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.*;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class OrderRepositoryIntegrationTest extends BaseRepositoryIntegrationTest {

    @Autowired
    EntityManager entityManager;

    @Autowired
    OrderRepository orderRepository;

    @Test
    void findAllTest() {
        var results = orderRepository.findAll();
        Assertions.assertFalse(results.isEmpty());
    }

    @Test
    void findByIdTest() {
        var id = 1L;
        var order = orderRepository.findById(id).orElseThrow();
        Assertions.assertNotNull(order.getId());
        Assertions.assertNotNull(order.getVersion());
    }

    @Test
    void saveTest() {
        Order order = new Order();
        order.setOrderId("");
        order.setStatus(OrderStatus.values()[0]);
        order.setCustomerId("");
        order.setCustomerEmail("");
        order.setPaymentMethodId("");
        order.setItems(List.of(new OrderItem()));
        order.setShippingAddress(new ShippingAddress());
        order.setTotalAmount(BigDecimal.valueOf(0));
        order.setCurrency("");
        order.setCreatedAt(Instant.now());
        order.setConfirmedAt(Instant.now());
        order.setCancelledAt(Instant.now());
        order.setCancellationReason("");

        // Persist aggregate root
        var created = orderRepository.save(order);

        // reloading to get relationships persisted by id
        entityManager.flush();
        entityManager.refresh(created);
        Assertions.assertNotNull(created.getId());
        Assertions.assertNotNull(created.getVersion());
    }

    @Test
    void updateTest() {
        var id = 1L;
        var now = Instant.now();
        var order = orderRepository.findById(id).orElseThrow();
        order.setOrderId("");
        order.setStatus(OrderStatus.values()[0]);
        order.setCustomerId("");
        order.setCustomerEmail("");
        order.setPaymentMethodId("");
        order.setItems(List.of(new OrderItem()));
        order.setShippingAddress(new ShippingAddress());
        order.setTotalAmount(BigDecimal.valueOf(0));
        order.setCurrency("");
        order.setCreatedAt(now);
        order.setConfirmedAt(now);
        order.setCancelledAt(now);
        order.setCancellationReason("");

        order = orderRepository.save(order);
        Assertions.assertEquals("", order.getOrderId());
        Assertions.assertEquals(OrderStatus.values()[0], order.getStatus());
        Assertions.assertEquals("", order.getCustomerId());
        Assertions.assertEquals("", order.getCustomerEmail());
        Assertions.assertEquals("", order.getPaymentMethodId());
        org.assertj.core.api.Assertions.assertThat(order.getItems())
                .usingRecursiveComparison()
                .isEqualTo(List.of(new OrderItem()));
        org.assertj.core.api.Assertions.assertThat(order.getShippingAddress())
                .usingRecursiveComparison()
                .isEqualTo(new ShippingAddress());
        Assertions.assertEquals(BigDecimal.valueOf(0), order.getTotalAmount());
        Assertions.assertEquals("", order.getCurrency());
        Assertions.assertEquals(now, order.getCreatedAt());
        Assertions.assertEquals(now, order.getConfirmedAt());
        Assertions.assertEquals(now, order.getCancelledAt());
        Assertions.assertEquals("", order.getCancellationReason());
    }

    @Test
    void deleteTest() {
        var id = 1L;
        orderRepository.deleteById(id);
        var notFound = orderRepository.findById(id);
        Assertions.assertFalse(notFound.isPresent());
    }
}
