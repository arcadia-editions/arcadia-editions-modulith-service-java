package com.arcadiaeditions.fulfillment.shipping.processing;

import com.arcadiaeditions.fulfillment.shipping.*;
import com.arcadiaeditions.fulfillment.shipping.domain.events.*;
import com.arcadiaeditions.fulfillment.shipping.dtos.*;
import com.arcadiaeditions.fulfillment.shipping.processing.mappers.PaymentsProcessingEventsListenerMapper;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

/**
 * Internal domain event listeners for PaymentsProcessingZdl.
 *
 * This class is generated once and can be customized safely.
 */
@Component
public class PaymentsProcessingEventsListener {
    private final FulfillmentShippingService fulfillmentShippingService;
    private final PaymentsProcessingEventsListenerMapper mapper = PaymentsProcessingEventsListenerMapper.INSTANCE;

    public PaymentsProcessingEventsListener(FulfillmentShippingService fulfillmentShippingService) {
        this.fulfillmentShippingService = fulfillmentShippingService;
    }

    @ApplicationModuleListener
    public void onPaymentCaptured(com.arcadiaeditions.payments.processing.domain.event.PaymentCaptured event) {
        fulfillmentShippingService.recordPaymentCaptured(mapper.asRecordPaymentCapturedInput(event));
    }
}
