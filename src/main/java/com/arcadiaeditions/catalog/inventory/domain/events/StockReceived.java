package com.arcadiaeditions.catalog.inventory.domain.events;

import com.arcadiaeditions.catalog.inventory.domain.*;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.math.*;
import java.time.*;
import java.util.*;

/**
 *
 */
@org.springframework.modulith.NamedInterface("events.StockReceived")
@org.jmolecules.event.annotation.DomainEvent
@org.jmolecules.architecture.layered.DomainLayer
public class StockReceived implements Serializable {

    @java.io.Serial
    private static final long serialVersionUID = 1L;

    private String sku;

    private Integer quantity;

    private Integer availableQuantity;

    private Integer reservedQuantity;

    private Integer soldQuantity;

    private String reason;

    private String reference;

    private Instant receivedAt;

    public String getSku() {
        return sku;
    }

    public StockReceived setSku(String sku) {
        this.sku = sku;
        return this;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public StockReceived setQuantity(Integer quantity) {
        this.quantity = quantity;
        return this;
    }

    public Integer getAvailableQuantity() {
        return availableQuantity;
    }

    public StockReceived setAvailableQuantity(Integer availableQuantity) {
        this.availableQuantity = availableQuantity;
        return this;
    }

    public Integer getReservedQuantity() {
        return reservedQuantity;
    }

    public StockReceived setReservedQuantity(Integer reservedQuantity) {
        this.reservedQuantity = reservedQuantity;
        return this;
    }

    public Integer getSoldQuantity() {
        return soldQuantity;
    }

    public StockReceived setSoldQuantity(Integer soldQuantity) {
        this.soldQuantity = soldQuantity;
        return this;
    }

    public String getReason() {
        return reason;
    }

    public StockReceived setReason(String reason) {
        this.reason = reason;
        return this;
    }

    public String getReference() {
        return reference;
    }

    public StockReceived setReference(String reference) {
        this.reference = reference;
        return this;
    }

    public Instant getReceivedAt() {
        return receivedAt;
    }

    public StockReceived setReceivedAt(Instant receivedAt) {
        this.receivedAt = receivedAt;
        return this;
    }
}
