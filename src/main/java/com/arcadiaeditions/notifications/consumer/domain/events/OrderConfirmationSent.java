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
@org.springframework.modulith.NamedInterface("events.OrderConfirmationSent")
@org.jmolecules.event.annotation.DomainEvent
@org.jmolecules.architecture.layered.DomainLayer
public class OrderConfirmationSent implements Serializable {

    @java.io.Serial
    private static final long serialVersionUID = 1L;

    private String notificationId;

    private String orderId;

    private String customerId;

    private NotificationChannel channel;

    private String recipient;

    private Instant sentAt;

    public String getNotificationId() {
        return notificationId;
    }

    public OrderConfirmationSent setNotificationId(String notificationId) {
        this.notificationId = notificationId;
        return this;
    }

    public String getOrderId() {
        return orderId;
    }

    public OrderConfirmationSent setOrderId(String orderId) {
        this.orderId = orderId;
        return this;
    }

    public String getCustomerId() {
        return customerId;
    }

    public OrderConfirmationSent setCustomerId(String customerId) {
        this.customerId = customerId;
        return this;
    }

    public NotificationChannel getChannel() {
        return channel;
    }

    public OrderConfirmationSent setChannel(NotificationChannel channel) {
        this.channel = channel;
        return this;
    }

    public String getRecipient() {
        return recipient;
    }

    public OrderConfirmationSent setRecipient(String recipient) {
        this.recipient = recipient;
        return this;
    }

    public Instant getSentAt() {
        return sentAt;
    }

    public OrderConfirmationSent setSentAt(Instant sentAt) {
        this.sentAt = sentAt;
        return this;
    }
}
