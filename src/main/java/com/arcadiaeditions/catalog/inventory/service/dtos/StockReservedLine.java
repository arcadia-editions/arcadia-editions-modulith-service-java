package com.arcadiaeditions.catalog.inventory.service.dtos;

import com.arcadiaeditions.catalog.inventory.domain.*;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.math.*;
import java.time.*;
import java.util.*;

/**
 * StockReservedLine.
 */
@org.jmolecules.ddd.annotation.ValueObject
public class StockReservedLine implements Serializable {

    private String reservationId;

    private String sku;

    private Integer quantity;

    private Instant reservedAt;

    public String getReservationId() {
        return reservationId;
    }

    public StockReservedLine setReservationId(String reservationId) {
        this.reservationId = reservationId;
        return this;
    }

    public String getSku() {
        return sku;
    }

    public StockReservedLine setSku(String sku) {
        this.sku = sku;
        return this;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public StockReservedLine setQuantity(Integer quantity) {
        this.quantity = quantity;
        return this;
    }

    public Instant getReservedAt() {
        return reservedAt;
    }

    public StockReservedLine setReservedAt(Instant reservedAt) {
        this.reservedAt = reservedAt;
        return this;
    }
}
