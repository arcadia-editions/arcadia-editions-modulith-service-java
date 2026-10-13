package com.arcadiaeditions.catalog.inventory.service.dtos;

import com.arcadiaeditions.catalog.inventory.domain.*;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.math.*;
import java.time.*;
import java.util.*;

/**
 * StockReservedResult.
 */
@org.jmolecules.ddd.annotation.ValueObject
public class StockReservedResult implements Serializable {

    private String orderId;

    private Instant expiresAt;

    private List<StockReservedLine> reservations = new ArrayList<>();

    public StockReservedResult addReservations(StockReservedLine reservations) {
        this.reservations.add(reservations);
        return this;
    }

    public String getOrderId() {
        return orderId;
    }

    public StockReservedResult setOrderId(String orderId) {
        this.orderId = orderId;
        return this;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public StockReservedResult setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
        return this;
    }

    public List<StockReservedLine> getReservations() {
        return reservations;
    }

    public StockReservedResult setReservations(List<StockReservedLine> reservations) {
        this.reservations = reservations;
        return this;
    }
}
