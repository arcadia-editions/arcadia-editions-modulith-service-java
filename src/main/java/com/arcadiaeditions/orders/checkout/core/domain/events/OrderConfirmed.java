package com.arcadiaeditions.orders.checkout.core.domain.events;

import com.arcadiaeditions.orders.checkout.core.domain.*;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.math.*;
import java.time.*;
import java.util.*;

/**
 *
 */
@org.springframework.modulith.NamedInterface("events.OrderConfirmed")
@org.jmolecules.event.annotation.DomainEvent
public class OrderConfirmed implements Serializable {

    @java.io.Serial
    private static final long serialVersionUID = 1L;

    private String orderId;

    private Integer version;

    private String customerId;

    private Instant confirmedAt;

    private List<OrderConfirmedItem> items;

    private OrderConfirmedShippingAddress shippingAddress;

    public OrderConfirmed addItems(OrderConfirmedItem items) {
        this.items.add(items);
        return this;
    }

    public String getOrderId() {
        return orderId;
    }

    public OrderConfirmed setOrderId(String orderId) {
        this.orderId = orderId;
        return this;
    }

    public Integer getVersion() {
        return version;
    }

    public OrderConfirmed setVersion(Integer version) {
        this.version = version;
        return this;
    }

    public String getCustomerId() {
        return customerId;
    }

    public OrderConfirmed setCustomerId(String customerId) {
        this.customerId = customerId;
        return this;
    }

    public Instant getConfirmedAt() {
        return confirmedAt;
    }

    public OrderConfirmed setConfirmedAt(Instant confirmedAt) {
        this.confirmedAt = confirmedAt;
        return this;
    }

    public List<OrderConfirmedItem> getItems() {
        return items;
    }

    public OrderConfirmed setItems(List<OrderConfirmedItem> items) {
        this.items = items;
        return this;
    }

    public OrderConfirmedShippingAddress getShippingAddress() {
        return shippingAddress;
    }

    public OrderConfirmed setShippingAddress(OrderConfirmedShippingAddress shippingAddress) {
        this.shippingAddress = shippingAddress;
        return this;
    }
}
