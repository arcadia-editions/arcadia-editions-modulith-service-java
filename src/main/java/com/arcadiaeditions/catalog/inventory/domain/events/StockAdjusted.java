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
@org.springframework.modulith.NamedInterface("events.StockAdjusted")
@org.jmolecules.event.annotation.DomainEvent
@org.jmolecules.architecture.layered.DomainLayer
public class StockAdjusted implements Serializable {

    @java.io.Serial
    private static final long serialVersionUID = 1L;

    private String sku;

    private Integer quantityDelta;

    private Integer availableQuantity;

    private Integer reservedQuantity;

    private Integer soldQuantity;

    private StockAdjustmentReason reason;

    private String reference;

    private Instant adjustedAt;

    public String getSku() {
        return sku;
    }

    public StockAdjusted setSku(String sku) {
        this.sku = sku;
        return this;
    }

    public Integer getQuantityDelta() {
        return quantityDelta;
    }

    public StockAdjusted setQuantityDelta(Integer quantityDelta) {
        this.quantityDelta = quantityDelta;
        return this;
    }

    public Integer getAvailableQuantity() {
        return availableQuantity;
    }

    public StockAdjusted setAvailableQuantity(Integer availableQuantity) {
        this.availableQuantity = availableQuantity;
        return this;
    }

    public Integer getReservedQuantity() {
        return reservedQuantity;
    }

    public StockAdjusted setReservedQuantity(Integer reservedQuantity) {
        this.reservedQuantity = reservedQuantity;
        return this;
    }

    public Integer getSoldQuantity() {
        return soldQuantity;
    }

    public StockAdjusted setSoldQuantity(Integer soldQuantity) {
        this.soldQuantity = soldQuantity;
        return this;
    }

    public StockAdjustmentReason getReason() {
        return reason;
    }

    public StockAdjusted setReason(StockAdjustmentReason reason) {
        this.reason = reason;
        return this;
    }

    public String getReference() {
        return reference;
    }

    public StockAdjusted setReference(String reference) {
        this.reference = reference;
        return this;
    }

    public Instant getAdjustedAt() {
        return adjustedAt;
    }

    public StockAdjusted setAdjustedAt(Instant adjustedAt) {
        this.adjustedAt = adjustedAt;
        return this;
    }
}
