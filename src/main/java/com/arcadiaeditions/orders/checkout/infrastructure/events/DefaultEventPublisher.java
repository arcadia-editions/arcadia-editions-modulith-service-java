package com.arcadiaeditions.orders.checkout.infrastructure.events;

import com.arcadiaeditions.orders.checkout.core.domain.*;
import com.arcadiaeditions.orders.checkout.core.domain.events.*;
import com.arcadiaeditions.orders.checkout.core.outbound.events.EventPublisher;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
@org.jmolecules.architecture.hexagonal.SecondaryAdapter
public class DefaultEventPublisher implements EventPublisher {

    private final ApplicationEventPublisher applicationEventPublisher;

    public DefaultEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
        this.applicationEventPublisher = applicationEventPublisher;
    }

    public void onOrderConfirmedItem(OrderConfirmedItem event) {
        applicationEventPublisher.publishEvent(event);
    }

    public void onOrderConfirmedShippingAddress(OrderConfirmedShippingAddress event) {
        applicationEventPublisher.publishEvent(event);
    }

    public void onStockUnavailableItem(StockUnavailableItem event) {
        applicationEventPublisher.publishEvent(event);
    }

    public void onStockUnavailable(StockUnavailable event) {
        applicationEventPublisher.publishEvent(event);
    }

    public void onOrderConfirmed(OrderConfirmed event) {
        applicationEventPublisher.publishEvent(event);
    }

    public void onOrderCancelled(OrderCancelled event) {
        applicationEventPublisher.publishEvent(event);
    }

    public void onOrderCreated(OrderCreated event) {
        applicationEventPublisher.publishEvent(event);
    }
}
