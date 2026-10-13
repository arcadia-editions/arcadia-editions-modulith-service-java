package com.arcadiaeditions.orders.checkout.core.outbound.events;

import com.arcadiaeditions.orders.checkout.core.domain.*;
import com.arcadiaeditions.orders.checkout.core.domain.events.*;

@org.jmolecules.architecture.hexagonal.SecondaryPort
public interface EventPublisher {

    void onOrderConfirmedItem(OrderConfirmedItem event);

    void onOrderConfirmedShippingAddress(OrderConfirmedShippingAddress event);

    void onStockUnavailableItem(StockUnavailableItem event);

    void onStockUnavailable(StockUnavailable event);

    void onOrderConfirmed(OrderConfirmed event);

    void onOrderCancelled(OrderCancelled event);

    void onOrderCreated(OrderCreated event);
}
