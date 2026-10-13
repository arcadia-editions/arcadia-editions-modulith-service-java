package com.arcadiaeditions.notifications.consumer.domain.events;

import com.arcadiaeditions.notifications.consumer.domain.*;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.math.*;
import java.time.*;
import java.util.*;

/**
 *
 */
@org.springframework.modulith.NamedInterface("events.NotificationDeliveryFailed")
@org.jmolecules.event.annotation.DomainEvent
@org.jmolecules.architecture.layered.DomainLayer
public class NotificationDeliveryFailed implements Serializable {

    @java.io.Serial
    private static final long serialVersionUID = 1L;

    private String notificationId;

    private String orderId;

    private String customerId;

    private List<String> skus;

    private NotificationType type;

    private NotificationChannel channel;

    private String recipient;

    private String reason;

    private Instant failedAt;

    public NotificationDeliveryFailed addSkus(String skus) {
        this.skus.add(skus);
        return this;
    }

    public String getNotificationId() {
        return notificationId;
    }

    public NotificationDeliveryFailed setNotificationId(String notificationId) {
        this.notificationId = notificationId;
        return this;
    }

    public String getOrderId() {
        return orderId;
    }

    public NotificationDeliveryFailed setOrderId(String orderId) {
        this.orderId = orderId;
        return this;
    }

    public String getCustomerId() {
        return customerId;
    }

    public NotificationDeliveryFailed setCustomerId(String customerId) {
        this.customerId = customerId;
        return this;
    }

    public List<String> getSkus() {
        return skus;
    }

    public NotificationDeliveryFailed setSkus(List<String> skus) {
        this.skus = skus;
        return this;
    }

    public NotificationType getType() {
        return type;
    }

    public NotificationDeliveryFailed setType(NotificationType type) {
        this.type = type;
        return this;
    }

    public NotificationChannel getChannel() {
        return channel;
    }

    public NotificationDeliveryFailed setChannel(NotificationChannel channel) {
        this.channel = channel;
        return this;
    }

    public String getRecipient() {
        return recipient;
    }

    public NotificationDeliveryFailed setRecipient(String recipient) {
        this.recipient = recipient;
        return this;
    }

    public String getReason() {
        return reason;
    }

    public NotificationDeliveryFailed setReason(String reason) {
        this.reason = reason;
        return this;
    }

    public Instant getFailedAt() {
        return failedAt;
    }

    public NotificationDeliveryFailed setFailedAt(Instant failedAt) {
        this.failedAt = failedAt;
        return this;
    }
}
