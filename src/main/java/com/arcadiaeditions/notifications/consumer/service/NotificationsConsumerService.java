package com.arcadiaeditions.notifications.consumer.service;

import com.arcadiaeditions.notifications.consumer.domain.*;
import com.arcadiaeditions.notifications.consumer.service.dtos.*;
import java.math.*;
import java.time.*;
import java.util.*;

/**
 * Inbound Service Port for managing [Notification].
 */
@org.springframework.modulith.NamedInterface("NotificationsConsumerService")
@org.jspecify.annotations.NullMarked
@org.jmolecules.architecture.layered.ApplicationLayer
public interface NotificationsConsumerService {

    /**
     *
     *
     * With Events: [[OrderConfirmationSent, NotificationDeliveryFailed]].
     *
     */
    public Notification sendOrderConfirmation(SendOrderConfirmationInput input);
    /**
     *
     *
     * With Events: [[StockUnavailableNotificationSent, NotificationDeliveryFailed]].
     *
     */
    public Notification sendStockUnavailableNotification(SendStockUnavailableNotificationInput input);
    /**
     *
     *
     * With Events: [[OrderCancelledNotificationSent, NotificationDeliveryFailed]].
     *
     */
    public Notification sendOrderCancelledNotification(SendOrderCancelledNotificationInput input);
}
