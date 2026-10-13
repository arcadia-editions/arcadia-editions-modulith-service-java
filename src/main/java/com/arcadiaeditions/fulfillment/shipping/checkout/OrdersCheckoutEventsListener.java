package com.arcadiaeditions.fulfillment.shipping.checkout;

import com.arcadiaeditions.fulfillment.shipping.*;
import com.arcadiaeditions.fulfillment.shipping.checkout.mappers.OrdersCheckoutEventsListenerMapper;
import com.arcadiaeditions.fulfillment.shipping.domain.events.*;
import com.arcadiaeditions.fulfillment.shipping.dtos.*;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

/**
 * Internal domain event listeners for OrdersCheckoutZdl.
 *
 * This class is generated once and can be customized safely.
 */
@Component
public class OrdersCheckoutEventsListener {
    private final FulfillmentShippingService fulfillmentShippingService;
    private final OrdersCheckoutEventsListenerMapper mapper = OrdersCheckoutEventsListenerMapper.INSTANCE;

    public OrdersCheckoutEventsListener(FulfillmentShippingService fulfillmentShippingService) {
        this.fulfillmentShippingService = fulfillmentShippingService;
    }

    @ApplicationModuleListener
    public void onOrderConfirmed(com.arcadiaeditions.orders.checkout.core.domain.events.OrderConfirmed event) {
        fulfillmentShippingService.scheduleFulfillment(mapper.asScheduleFulfillmentInput(event));
    }
}
