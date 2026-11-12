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
@org.springframework.modulith.NamedInterface("events.FulfillmentScheduled")
@org.jmolecules.event.annotation.DomainEvent
public class FulfillmentScheduled implements Serializable {

    @java.io.Serial
    private static final long serialVersionUID = 1L;

    private String fulfillmentId;

    private String orderId;

    private String customerId;

    private Instant scheduledAt;

    public String getFulfillmentId() {
        return fulfillmentId;
    }

    public FulfillmentScheduled setFulfillmentId(String fulfillmentId) {
        this.fulfillmentId = fulfillmentId;
        return this;
    }

    public String getOrderId() {
        return orderId;
    }

    public FulfillmentScheduled setOrderId(String orderId) {
        this.orderId = orderId;
        return this;
    }

    public String getCustomerId() {
        return customerId;
    }

    public FulfillmentScheduled setCustomerId(String customerId) {
        this.customerId = customerId;
        return this;
    }

    public Instant getScheduledAt() {
        return scheduledAt;
    }

    public FulfillmentScheduled setScheduledAt(Instant scheduledAt) {
        this.scheduledAt = scheduledAt;
        return this;
    }
}
