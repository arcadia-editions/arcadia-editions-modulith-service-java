package com.arcadiaeditions.orders.checkout.core.domain;

import com.arcadiaeditions.orders.checkout.core.domain.events.StockUnavailable;
import com.arcadiaeditions.orders.checkout.core.domain.events.StockUnavailableItem;
import java.util.List;

/**
 * startOrderCheckout answered by StockUnavailableResult: no order was created, StockUnavailable was emitted.
 * The service does not roll back on it, so the StockUnavailable publication commits.
 */
public class StockUnavailableException extends RuntimeException {

    private final transient StockUnavailable event;

    public StockUnavailableException(StockUnavailable event) {
        super("Stock unavailable for order " + event.getOrderId() + ": "
                + event.getUnavailableItems().stream().map(StockUnavailableItem::getSku).toList());
        this.event = event;
    }

    public String getOrderId() {
        return event.getOrderId();
    }

    public List<StockUnavailableItem> getUnavailableItems() {
        return event.getUnavailableItems();
    }
}
