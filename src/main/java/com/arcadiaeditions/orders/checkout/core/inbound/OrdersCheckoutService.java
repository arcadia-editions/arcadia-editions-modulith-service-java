package com.arcadiaeditions.orders.checkout.core.inbound;

import com.arcadiaeditions.orders.checkout.core.domain.*;
import com.arcadiaeditions.orders.checkout.core.inbound.dtos.*;
import java.math.*;
import java.time.*;
import java.util.*;

/**
 * Inbound Service Port for managing [Order].
 */
@org.springframework.modulith.NamedInterface("OrdersCheckoutService")
@org.jspecify.annotations.NullMarked
@org.jmolecules.architecture.hexagonal.PrimaryPort
public interface OrdersCheckoutService {

    /**
     *
     *
     * With Events: [[OrderCreated, StockUnavailable]].
     *
     */
    public Order startOrderCheckout(StartOrderCheckoutInput input);
    /**
     *
     *
     * With Events: [OrderConfirmed].
     *
     */
    public Order confirmOrder(ConfirmOrderInput input);
    /**
     *
     *
     * With Events: [OrderCancelled].
     *
     */
    public Order cancelOrder(CancelOrderInput input);
    /**
     *
     *
     */
    public Optional<Order> getOrder(Long id);
}
