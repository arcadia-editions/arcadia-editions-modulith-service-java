package com.arcadiaeditions.payments.processing.adapter.in.event.shipping;

import com.arcadiaeditions.payments.processing.adapter.in.event.shipping.mapper.FulfillmentShippingEventsListenerMapper;
import com.arcadiaeditions.payments.processing.application.port.in.*;
import com.arcadiaeditions.payments.processing.application.port.in.dto.*;
import com.arcadiaeditions.payments.processing.domain.event.*;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

/**
 * Internal domain event listeners for FulfillmentShippingZdl.
 *
 * This class is generated once and can be customized safely.
 */
@Component
@org.jmolecules.architecture.hexagonal.PrimaryAdapter
public class FulfillmentShippingEventsListener {
    private final PaymentsProcessingService paymentsProcessingService;
    private final FulfillmentShippingEventsListenerMapper mapper = FulfillmentShippingEventsListenerMapper.INSTANCE;

    public FulfillmentShippingEventsListener(PaymentsProcessingService paymentsProcessingService) {
        this.paymentsProcessingService = paymentsProcessingService;
    }

    @ApplicationModuleListener
    public void onFulfillmentScheduled(
            com.arcadiaeditions.fulfillment.shipping.domain.events.FulfillmentScheduled event) {
        paymentsProcessingService.capturePayment(mapper.asCapturePaymentInput(event));
    }

    @ApplicationModuleListener
    public void onFulfillmentFailed(com.arcadiaeditions.fulfillment.shipping.domain.events.FulfillmentFailed event) {
        paymentsProcessingService.voidPayment(mapper.asVoidPaymentInput(event));
    }
}
