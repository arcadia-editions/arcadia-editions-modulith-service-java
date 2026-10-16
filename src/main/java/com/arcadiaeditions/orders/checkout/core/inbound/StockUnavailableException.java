package com.arcadiaeditions.orders.checkout.core.inbound;

import com.arcadiaeditions.orders.checkout.core.domain.events.StockUnavailable;

/**
 * Thrown by {@link OrdersCheckoutService#startOrderCheckout} when some items cannot be reserved
 * in the requested quantity. Carries the published {@link StockUnavailable} event.
 */
public class StockUnavailableException extends RuntimeException {

    @java.io.Serial
    private static final long serialVersionUID = 1L;

    private final transient StockUnavailable stockUnavailable;

    public StockUnavailableException(StockUnavailable stockUnavailable) {
        super("Some items are not available in the requested quantity");
        this.stockUnavailable = stockUnavailable;
    }

    public StockUnavailable getStockUnavailable() {
        return stockUnavailable;
    }
}
