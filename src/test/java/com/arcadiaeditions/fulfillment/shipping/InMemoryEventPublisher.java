package com.arcadiaeditions.fulfillment.shipping;

import com.arcadiaeditions.fulfillment.shipping.domain.*;
import com.arcadiaeditions.fulfillment.shipping.domain.events.*;
import com.arcadiaeditions.fulfillment.shipping.events.EventPublisher;
import java.util.ArrayList;
import java.util.List;

public class InMemoryEventPublisher implements EventPublisher {

    private final List<Object> events = new ArrayList<>();

    public List<Object> getEvents() {
        return events;
    }

    public void onFulfillmentScheduled(FulfillmentScheduled event) {
        events.add(event);
    }

    public void onFulfillmentReadyToShip(FulfillmentReadyToShip event) {
        events.add(event);
    }

    public void onFulfillmentFailed(FulfillmentFailed event) {
        events.add(event);
    }
}
