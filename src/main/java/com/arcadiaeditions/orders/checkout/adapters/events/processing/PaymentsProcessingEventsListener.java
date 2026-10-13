package com.arcadiaeditions.orders.checkout.adapters.events.processing;

import com.arcadiaeditions.orders.checkout.core.domain.events.*;
import com.arcadiaeditions.orders.checkout.core.inbound.*;
import com.arcadiaeditions.orders.checkout.core.inbound.dtos.*;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

/**
 * Internal domain event listeners for PaymentsProcessingZdl.
 *
 * This class is generated once and can be customized safely.
 */
@Component
@org.jmolecules.architecture.hexagonal.PrimaryAdapter
public class PaymentsProcessingEventsListener {
    private final OrdersCheckoutService ordersCheckoutService;

    public PaymentsProcessingEventsListener(OrdersCheckoutService ordersCheckoutService) {
        this.ordersCheckoutService = ordersCheckoutService;
    }

    @ApplicationModuleListener
    public void onPaymentAuthorized(com.arcadiaeditions.payments.processing.domain.event.PaymentAuthorized event) {
        // TODO CUSTOM_REQUIRED: resolve the Order id for PaymentAuthorized (no event field is assumed to carry it)
        Long id = null;
        ordersCheckoutService.confirmOrder(id);
    }
}
