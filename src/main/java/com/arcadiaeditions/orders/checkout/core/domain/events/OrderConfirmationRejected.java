package com.arcadiaeditions.orders.checkout.core.domain.events;

import com.arcadiaeditions.orders.checkout.core.domain.*;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.math.*;
import java.time.*;
import java.util.*;

/**
 *
 */
@org.springframework.modulith.NamedInterface("events.OrderConfirmationRejected")
@org.jmolecules.event.annotation.DomainEvent
public class OrderConfirmationRejected implements Serializable {

    @java.io.Serial
    private static final long serialVersionUID = 1L;

    private String orderId;

    private String customerId;

    private String reason;

    private Instant rejectedAt;

    public String getOrderId() {
        return orderId;
    }

    public OrderConfirmationRejected setOrderId(String orderId) {
        this.orderId = orderId;
        return this;
    }

    public String getCustomerId() {
        return customerId;
    }

    public OrderConfirmationRejected setCustomerId(String customerId) {
        this.customerId = customerId;
        return this;
    }

    public String getReason() {
        return reason;
    }

    public OrderConfirmationRejected setReason(String reason) {
        this.reason = reason;
        return this;
    }

    public Instant getRejectedAt() {
        return rejectedAt;
    }

    public OrderConfirmationRejected setRejectedAt(Instant rejectedAt) {
        this.rejectedAt = rejectedAt;
        return this;
    }
}
