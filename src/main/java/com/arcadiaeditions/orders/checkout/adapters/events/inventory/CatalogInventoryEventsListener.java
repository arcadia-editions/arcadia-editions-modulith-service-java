package com.arcadiaeditions.orders.checkout.adapters.events.inventory;

import com.arcadiaeditions.orders.checkout.adapters.events.inventory.mappers.CatalogInventoryEventsListenerMapper;
import com.arcadiaeditions.orders.checkout.core.domain.events.*;
import com.arcadiaeditions.orders.checkout.core.inbound.*;
import com.arcadiaeditions.orders.checkout.core.inbound.dtos.*;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

/**
 * Internal domain event listeners for CatalogInventoryZdl.
 *
 * This class is generated once and can be customized safely.
 */
@Component
@org.jmolecules.architecture.hexagonal.PrimaryAdapter
public class CatalogInventoryEventsListener {
    private final OrdersCheckoutService ordersCheckoutService;
    private final CatalogInventoryEventsListenerMapper mapper = CatalogInventoryEventsListenerMapper.INSTANCE;

    public CatalogInventoryEventsListener(OrdersCheckoutService ordersCheckoutService) {
        this.ordersCheckoutService = ordersCheckoutService;
    }

    @ApplicationModuleListener
    public void onStockReleased(com.arcadiaeditions.catalog.inventory.domain.events.StockReleased event) {
        ordersCheckoutService.cancelOrder(mapper.asCancelOrderInput(event));
    }
}
