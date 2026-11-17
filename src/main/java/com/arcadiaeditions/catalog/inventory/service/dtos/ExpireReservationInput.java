package com.arcadiaeditions.catalog.inventory.service.dtos;

import com.arcadiaeditions.catalog.inventory.domain.*;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.math.*;
import java.time.*;
import java.util.*;

/**
 * ExpireReservationInput.
 */
@org.jmolecules.ddd.annotation.ValueObject
public class ExpireReservationInput implements Serializable {

    @NotNull
    private String orderId;

    public String getOrderId() {
        return orderId;
    }

    public ExpireReservationInput setOrderId(String orderId) {
        this.orderId = orderId;
        return this;
    }
}
