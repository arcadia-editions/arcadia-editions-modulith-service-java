package com.arcadiaeditions.notifications.consumer.events;

import com.arcadiaeditions.notifications.consumer.domain.*;
import com.arcadiaeditions.notifications.consumer.domain.events.*;
import java.util.ArrayList;
import java.util.List;

public class InMemoryEventPublisher implements EventPublisher {

    private final List<Object> events = new ArrayList<>();

    public List<Object> getEvents() {
        return events;
    }

    public void onStockUnavailableNotificationSent(StockUnavailableNotificationSent event) {
        events.add(event);
    }

    public void onOrderCancelledNotificationSent(OrderCancelledNotificationSent event) {
        events.add(event);
    }

    public void onOrderConfirmationSent(OrderConfirmationSent event) {
        events.add(event);
    }

    public void onNotificationDeliveryFailed(NotificationDeliveryFailed event) {
        events.add(event);
    }
}
