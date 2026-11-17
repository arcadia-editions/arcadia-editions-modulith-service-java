package com.arcadiaeditions.notifications.consumer.service;

import com.arcadiaeditions.notifications.consumer.domain.NotificationChannel;
import com.arcadiaeditions.notifications.consumer.domain.NotificationType;

/**
 * Outbound port: delivers one notification message to its recipient over its channel (e-mail today).
 * Used by every send command (order confirmation, stock unavailable, order cancelled).
 */
@org.jspecify.annotations.NullMarked
@org.jmolecules.architecture.layered.ApplicationLayer
public interface NotificationSender {

    DeliveryResult send(NotificationMessage message);

    record NotificationMessage(
            String notificationId,
            NotificationType type,
            NotificationChannel channel,
            String recipient,
            String subject,
            String body) {}

    /** {@code delivered} false carries the {@code reason}. */
    record DeliveryResult(boolean delivered, @org.jspecify.annotations.Nullable String reason) {

        public static DeliveryResult sent() {
            return new DeliveryResult(true, null);
        }

        public static DeliveryResult failed(String reason) {
            return new DeliveryResult(false, reason);
        }
    }
}
