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
@org.springframework.modulith.NamedInterface("events.FulfillmentCancelled")
@org.jmolecules.event.annotation.DomainEvent
public class FulfillmentCancelled implements Serializable {

    @java.io.Serial
    private static final long serialVersionUID = 1L;

    private String fulfillmentId;

    private String orderId;

    private String customerId;

    private String reason;

    private Instant cancelledAt;

    public String getFulfillmentId() {
        return fulfillmentId;
    }

    public FulfillmentCancelled setFulfillmentId(String fulfillmentId) {
        this.fulfillmentId = fulfillmentId;
        return this;
    }

    public String getOrderId() {
        return orderId;
    }

    public FulfillmentCancelled setOrderId(String orderId) {
        this.orderId = orderId;
        return this;
    }

    public String getCustomerId() {
        return customerId;
    }

    public FulfillmentCancelled setCustomerId(String customerId) {
        this.customerId = customerId;
        return this;
    }

    public String getReason() {
        return reason;
    }

    public FulfillmentCancelled setReason(String reason) {
        this.reason = reason;
        return this;
    }

    public Instant getCancelledAt() {
        return cancelledAt;
    }

    public FulfillmentCancelled setCancelledAt(Instant cancelledAt) {
        this.cancelledAt = cancelledAt;
        return this;
    }
}
