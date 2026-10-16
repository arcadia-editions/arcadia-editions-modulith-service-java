package com.arcadiaeditions.notifications.consumer.service;

import com.arcadiaeditions.notifications.consumer.domain.Notification;

/**
 * Port to the external notification delivery channel (gap report X-03, research R-06).
 */
public interface NotificationSender {

    DeliveryResult send(Notification notification);

    sealed interface DeliveryResult {}

    record Delivered() implements DeliveryResult {}

    record DeliveryFailed(String reason) implements DeliveryResult {}
}
