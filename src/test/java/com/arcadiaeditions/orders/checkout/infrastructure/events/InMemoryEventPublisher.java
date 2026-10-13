package com.arcadiaeditions.orders.checkout.infrastructure.events;

import com.arcadiaeditions.orders.checkout.core.domain.*;
import com.arcadiaeditions.orders.checkout.core.domain.events.*;
import com.arcadiaeditions.orders.checkout.core.outbound.events.EventPublisher;
import java.util.ArrayList;
import java.util.List;

public class InMemoryEventPublisher implements EventPublisher {

    private final List<Object> events = new ArrayList<>();

    public List<Object> getEvents() {
        return events;
    }

    public void onOrderConfirmedItem(OrderConfirmedItem event) {
        events.add(event);
    }

    public void onOrderConfirmedShippingAddress(OrderConfirmedShippingAddress event) {
        events.add(event);
    }

    public void onStockUnavailableItem(StockUnavailableItem event) {
        events.add(event);
    }

    public void onStockUnavailable(StockUnavailable event) {
        events.add(event);
    }

    public void onOrderConfirmed(OrderConfirmed event) {
        events.add(event);
    }

    public void onOrderCancelled(OrderCancelled event) {
        events.add(event);
    }

    public void onOrderCreated(OrderCreated event) {
        events.add(event);
    }
}
