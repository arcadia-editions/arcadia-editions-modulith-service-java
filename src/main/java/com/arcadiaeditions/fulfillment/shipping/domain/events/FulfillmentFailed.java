package com.arcadiaeditions.fulfillment.shipping.domain.events;

import com.arcadiaeditions.fulfillment.shipping.domain.*;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.math.*;
import java.time.*;
import java.util.*;

/**
 *
 */
@org.springframework.modulith.NamedInterface("events.FulfillmentFailed")
@org.jmolecules.event.annotation.DomainEvent
public class FulfillmentFailed implements Serializable {

    @java.io.Serial
    private static final long serialVersionUID = 1L;

    private String fulfillmentId;

    private String orderId;

    private String customerId;

    private String reason;

    private Instant failedAt;

    public String getFulfillmentId() {
        return fulfillmentId;
    }

    public FulfillmentFailed setFulfillmentId(String fulfillmentId) {
        this.fulfillmentId = fulfillmentId;
        return this;
    }

    public String getOrderId() {
        return orderId;
    }

    public FulfillmentFailed setOrderId(String orderId) {
        this.orderId = orderId;
        return this;
    }

    public String getCustomerId() {
        return customerId;
    }

    public FulfillmentFailed setCustomerId(String customerId) {
        this.customerId = customerId;
        return this;
    }

    public String getReason() {
        return reason;
    }

    public FulfillmentFailed setReason(String reason) {
        this.reason = reason;
        return this;
    }

    public Instant getFailedAt() {
        return failedAt;
    }

    public FulfillmentFailed setFailedAt(Instant failedAt) {
        this.failedAt = failedAt;
        return this;
    }
}
