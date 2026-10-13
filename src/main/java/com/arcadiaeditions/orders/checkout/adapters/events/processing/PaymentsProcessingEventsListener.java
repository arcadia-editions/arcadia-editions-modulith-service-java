package com.arcadiaeditions.orders.checkout.adapters.events.processing;

import com.arcadiaeditions.orders.checkout.adapters.events.processing.mappers.PaymentsProcessingEventsListenerMapper;
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
    private final PaymentsProcessingEventsListenerMapper mapper = PaymentsProcessingEventsListenerMapper.INSTANCE;

    public PaymentsProcessingEventsListener(OrdersCheckoutService ordersCheckoutService) {
        this.ordersCheckoutService = ordersCheckoutService;
    }

    @ApplicationModuleListener
    public void onPaymentAuthorized(com.arcadiaeditions.payments.processing.domain.event.PaymentAuthorized event) {
        ordersCheckoutService.confirmOrder(mapper.asConfirmOrderInput(event));
    }
}
