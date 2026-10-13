package com.arcadiaeditions.notifications.consumer.events;

import com.arcadiaeditions.notifications.consumer.domain.*;
import com.arcadiaeditions.notifications.consumer.domain.events.*;

public interface EventPublisher {

    void onStockUnavailableNotificationSent(StockUnavailableNotificationSent event);

    void onOrderCancelledNotificationSent(OrderCancelledNotificationSent event);

    void onOrderConfirmationSent(OrderConfirmationSent event);

    void onNotificationDeliveryFailed(NotificationDeliveryFailed event);
}
