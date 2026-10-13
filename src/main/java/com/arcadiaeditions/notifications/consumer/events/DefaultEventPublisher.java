package com.arcadiaeditions.notifications.consumer.events;

import com.arcadiaeditions.notifications.consumer.domain.*;
import com.arcadiaeditions.notifications.consumer.domain.events.*;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
@org.jmolecules.architecture.layered.InfrastructureLayer
public class DefaultEventPublisher implements EventPublisher {

    private final ApplicationEventPublisher applicationEventPublisher;

    public DefaultEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
        this.applicationEventPublisher = applicationEventPublisher;
    }

    public void onStockUnavailableNotificationSent(StockUnavailableNotificationSent event) {
        applicationEventPublisher.publishEvent(event);
    }

    public void onOrderCancelledNotificationSent(OrderCancelledNotificationSent event) {
        applicationEventPublisher.publishEvent(event);
    }

    public void onOrderConfirmationSent(OrderConfirmationSent event) {
        applicationEventPublisher.publishEvent(event);
    }

    public void onNotificationDeliveryFailed(NotificationDeliveryFailed event) {
        applicationEventPublisher.publishEvent(event);
    }
}
