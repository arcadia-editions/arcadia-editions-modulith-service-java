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
@org.springframework.modulith.NamedInterface("events.OrderCancelled")
@org.jmolecules.event.annotation.DomainEvent
public class OrderCancelled implements Serializable {

    @java.io.Serial
    private static final long serialVersionUID = 1L;

    private String orderId;

    private Integer version;

    private String customerId;

    private String customerEmail;

    private String reason;

    private Instant cancelledAt;

    public String getOrderId() {
        return orderId;
    }

    public OrderCancelled setOrderId(String orderId) {
        this.orderId = orderId;
        return this;
    }

    public Integer getVersion() {
        return version;
    }

    public OrderCancelled setVersion(Integer version) {
        this.version = version;
        return this;
    }

    public String getCustomerId() {
        return customerId;
    }

    public OrderCancelled setCustomerId(String customerId) {
        this.customerId = customerId;
        return this;
    }

    public String getCustomerEmail() {
        return customerEmail;
    }

    public OrderCancelled setCustomerEmail(String customerEmail) {
        this.customerEmail = customerEmail;
        return this;
    }

    public String getReason() {
        return reason;
    }

    public OrderCancelled setReason(String reason) {
        this.reason = reason;
        return this;
    }

    public Instant getCancelledAt() {
        return cancelledAt;
    }

    public OrderCancelled setCancelledAt(Instant cancelledAt) {
        this.cancelledAt = cancelledAt;
        return this;
    }
}
