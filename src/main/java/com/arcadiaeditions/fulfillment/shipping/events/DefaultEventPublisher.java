package com.arcadiaeditions.fulfillment.shipping.events;

import com.arcadiaeditions.fulfillment.shipping.EventPublisher;
import com.arcadiaeditions.fulfillment.shipping.domain.*;
import com.arcadiaeditions.fulfillment.shipping.domain.events.*;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
public class DefaultEventPublisher implements EventPublisher {

    private final ApplicationEventPublisher applicationEventPublisher;

    public DefaultEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
        this.applicationEventPublisher = applicationEventPublisher;
    }

    public void onFulfillmentScheduled(FulfillmentScheduled event) {
        applicationEventPublisher.publishEvent(event);
    }

    public void onFulfillmentReadyToShip(FulfillmentReadyToShip event) {
        applicationEventPublisher.publishEvent(event);
    }

    public void onFulfillmentFailed(FulfillmentFailed event) {
        applicationEventPublisher.publishEvent(event);
    }

    public void onFulfillmentCancelled(FulfillmentCancelled event) {
        applicationEventPublisher.publishEvent(event);
    }
}
