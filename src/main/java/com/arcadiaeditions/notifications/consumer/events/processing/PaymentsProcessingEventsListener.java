package com.arcadiaeditions.notifications.consumer.events.processing;

import com.arcadiaeditions.notifications.consumer.domain.events.*;
import com.arcadiaeditions.notifications.consumer.events.processing.mappers.PaymentsProcessingEventsListenerMapper;
import com.arcadiaeditions.notifications.consumer.service.*;
import com.arcadiaeditions.notifications.consumer.service.dtos.*;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

/**
 * Internal domain event listeners for PaymentsProcessingZdl.
 *
 * This class is generated once and can be customized safely.
 */
@Component
@org.jmolecules.architecture.layered.InterfaceLayer
public class PaymentsProcessingEventsListener {
    private final NotificationsConsumerService notificationsConsumerService;
    private final PaymentsProcessingEventsListenerMapper mapper = PaymentsProcessingEventsListenerMapper.INSTANCE;

    public PaymentsProcessingEventsListener(NotificationsConsumerService notificationsConsumerService) {
        this.notificationsConsumerService = notificationsConsumerService;
    }

    @ApplicationModuleListener
    public void onPaymentCaptured(com.arcadiaeditions.payments.processing.domain.event.PaymentCaptured event) {
        notificationsConsumerService.sendOrderConfirmation(mapper.asSendOrderConfirmationInput(event));
    }
}
