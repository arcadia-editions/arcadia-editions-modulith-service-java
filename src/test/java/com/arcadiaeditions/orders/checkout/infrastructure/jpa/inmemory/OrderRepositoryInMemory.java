package com.arcadiaeditions.orders.checkout.infrastructure.jpa.inmemory;

import com.arcadiaeditions.orders.checkout.core.domain.*;
import com.arcadiaeditions.orders.checkout.core.outbound.jpa.OrderRepository;
import java.math.*;
import java.time.*;
import java.util.*;

public class OrderRepositoryInMemory extends InMemoryJpaRepository<Order> implements OrderRepository {

    @Override
    public Optional<Order> findByOrderId(String orderId) {
        return Optional.ofNullable(findByUniqueField("orderId", orderId));
    }
}
