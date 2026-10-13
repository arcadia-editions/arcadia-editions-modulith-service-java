package com.arcadiaeditions.payments.processing.adapter.in.event.checkout;

import com.arcadiaeditions.payments.processing.adapter.in.event.checkout.mapper.OrdersCheckoutEventsListenerMapper;
import com.arcadiaeditions.payments.processing.application.port.in.*;
import com.arcadiaeditions.payments.processing.application.port.in.dto.*;
import com.arcadiaeditions.payments.processing.domain.event.*;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

/**
 * Internal domain event listeners for OrdersCheckoutZdl.
 *
 * This class is generated once and can be customized safely.
 */
@Component
@org.jmolecules.architecture.hexagonal.PrimaryAdapter
public class OrdersCheckoutEventsListener {
    private final PaymentsProcessingService paymentsProcessingService;
    private final OrdersCheckoutEventsListenerMapper mapper = OrdersCheckoutEventsListenerMapper.INSTANCE;

    public OrdersCheckoutEventsListener(PaymentsProcessingService paymentsProcessingService) {
        this.paymentsProcessingService = paymentsProcessingService;
    }

    @ApplicationModuleListener
    public void onOrderCreated(com.arcadiaeditions.orders.checkout.core.domain.events.OrderCreated event) {
        paymentsProcessingService.authorizePayment(mapper.asAuthorizePaymentInput(event));
    }
}
