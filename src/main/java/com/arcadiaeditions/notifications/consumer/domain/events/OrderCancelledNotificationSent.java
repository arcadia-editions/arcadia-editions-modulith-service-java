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
@org.springframework.modulith.NamedInterface("events.OrderCancelledNotificationSent")
@org.jmolecules.event.annotation.DomainEvent
@org.jmolecules.architecture.layered.DomainLayer
public class OrderCancelledNotificationSent implements Serializable {

    @java.io.Serial
    private static final long serialVersionUID = 1L;

    private String notificationId;

    private String orderId;

    private String customerId;

    private String reason;

    private NotificationChannel channel;

    private String recipient;

    private Instant sentAt;

    public String getNotificationId() {
        return notificationId;
    }

    public OrderCancelledNotificationSent setNotificationId(String notificationId) {
        this.notificationId = notificationId;
        return this;
    }

    public String getOrderId() {
        return orderId;
    }

    public OrderCancelledNotificationSent setOrderId(String orderId) {
        this.orderId = orderId;
        return this;
    }

    public String getCustomerId() {
        return customerId;
    }

    public OrderCancelledNotificationSent setCustomerId(String customerId) {
        this.customerId = customerId;
        return this;
    }

    public String getReason() {
        return reason;
    }

    public OrderCancelledNotificationSent setReason(String reason) {
        this.reason = reason;
        return this;
    }

    public NotificationChannel getChannel() {
        return channel;
    }

    public OrderCancelledNotificationSent setChannel(NotificationChannel channel) {
        this.channel = channel;
        return this;
    }

    public String getRecipient() {
        return recipient;
    }

    public OrderCancelledNotificationSent setRecipient(String recipient) {
        this.recipient = recipient;
        return this;
    }

    public Instant getSentAt() {
        return sentAt;
    }

    public OrderCancelledNotificationSent setSentAt(Instant sentAt) {
        this.sentAt = sentAt;
        return this;
    }
}
