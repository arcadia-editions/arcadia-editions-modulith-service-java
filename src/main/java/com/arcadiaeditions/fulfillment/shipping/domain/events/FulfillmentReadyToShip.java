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
@org.springframework.modulith.NamedInterface("events.FulfillmentReadyToShip")
@org.jmolecules.event.annotation.DomainEvent
public class FulfillmentReadyToShip implements Serializable {

    @java.io.Serial
    private static final long serialVersionUID = 1L;

    private String fulfillmentId;

    private String orderId;

    private String customerId;

    private String carrierCode;

    private String trackingNumber;

    private Instant readyAt;

    public String getFulfillmentId() {
        return fulfillmentId;
    }

    public FulfillmentReadyToShip setFulfillmentId(String fulfillmentId) {
        this.fulfillmentId = fulfillmentId;
        return this;
    }

    public String getOrderId() {
        return orderId;
    }

    public FulfillmentReadyToShip setOrderId(String orderId) {
        this.orderId = orderId;
        return this;
    }

    public String getCustomerId() {
        return customerId;
    }

    public FulfillmentReadyToShip setCustomerId(String customerId) {
        this.customerId = customerId;
        return this;
    }

    public String getCarrierCode() {
        return carrierCode;
    }

    public FulfillmentReadyToShip setCarrierCode(String carrierCode) {
        this.carrierCode = carrierCode;
        return this;
    }

    public String getTrackingNumber() {
        return trackingNumber;
    }

    public FulfillmentReadyToShip setTrackingNumber(String trackingNumber) {
        this.trackingNumber = trackingNumber;
        return this;
    }

    public Instant getReadyAt() {
        return readyAt;
    }

    public FulfillmentReadyToShip setReadyAt(Instant readyAt) {
        this.readyAt = readyAt;
        return this;
    }
}
