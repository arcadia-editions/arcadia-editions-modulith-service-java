package com.arcadiaeditions.catalog.inventory.service.dtos;

import com.arcadiaeditions.catalog.inventory.domain.*;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.math.*;
import java.time.*;
import java.util.*;

/**
 * StockReservationNotHeldResult.
 */
@org.jmolecules.ddd.annotation.ValueObject
public class StockReservationNotHeldResult implements Serializable {

    private String orderId;

    public String getOrderId() {
        return orderId;
    }

    public StockReservationNotHeldResult setOrderId(String orderId) {
        this.orderId = orderId;
        return this;
    }
}
