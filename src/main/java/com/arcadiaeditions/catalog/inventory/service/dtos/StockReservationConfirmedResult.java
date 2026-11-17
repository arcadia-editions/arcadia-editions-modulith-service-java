package com.arcadiaeditions.catalog.inventory.service.dtos;

import com.arcadiaeditions.catalog.inventory.domain.*;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.math.*;
import java.time.*;
import java.util.*;

/**
 * StockReservationConfirmedResult.
 */
@org.jmolecules.ddd.annotation.ValueObject
public class StockReservationConfirmedResult implements Serializable {

    private String orderId;

    private Instant confirmedAt;

    public String getOrderId() {
        return orderId;
    }

    public StockReservationConfirmedResult setOrderId(String orderId) {
        this.orderId = orderId;
        return this;
    }

    public Instant getConfirmedAt() {
        return confirmedAt;
    }

    public StockReservationConfirmedResult setConfirmedAt(Instant confirmedAt) {
        this.confirmedAt = confirmedAt;
        return this;
    }
}
