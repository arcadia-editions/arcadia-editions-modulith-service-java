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
@org.springframework.modulith.NamedInterface("events.StockUnavailableNotificationSent")
@org.jmolecules.event.annotation.DomainEvent
@org.jmolecules.architecture.layered.DomainLayer
public class StockUnavailableNotificationSent implements Serializable {

    @java.io.Serial
    private static final long serialVersionUID = 1L;

    private String notificationId;

    private String orderId;

    private String customerId;

    private List<String> skus;

    private NotificationChannel channel;

    private String recipient;

    private Instant sentAt;

    public StockUnavailableNotificationSent addSkus(String skus) {
        this.skus.add(skus);
        return this;
    }

    public String getNotificationId() {
        return notificationId;
    }

    public StockUnavailableNotificationSent setNotificationId(String notificationId) {
        this.notificationId = notificationId;
        return this;
    }

    public String getOrderId() {
        return orderId;
    }

    public StockUnavailableNotificationSent setOrderId(String orderId) {
        this.orderId = orderId;
        return this;
    }

    public String getCustomerId() {
        return customerId;
    }

    public StockUnavailableNotificationSent setCustomerId(String customerId) {
        this.customerId = customerId;
        return this;
    }

    public List<String> getSkus() {
        return skus;
    }

    public StockUnavailableNotificationSent setSkus(List<String> skus) {
        this.skus = skus;
        return this;
    }

    public NotificationChannel getChannel() {
        return channel;
    }

    public StockUnavailableNotificationSent setChannel(NotificationChannel channel) {
        this.channel = channel;
        return this;
    }

    public String getRecipient() {
        return recipient;
    }

    public StockUnavailableNotificationSent setRecipient(String recipient) {
        this.recipient = recipient;
        return this;
    }

    public Instant getSentAt() {
        return sentAt;
    }

    public StockUnavailableNotificationSent setSentAt(Instant sentAt) {
        this.sentAt = sentAt;
        return this;
    }
}
