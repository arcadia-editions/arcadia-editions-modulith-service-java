package com.arcadiaeditions.orders.checkout.core.inbound.dtos;

import com.arcadiaeditions.orders.checkout.core.domain.*;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.math.*;
import java.time.*;
import java.util.*;

/**
 * ConfirmOrderInput.
 */
@org.jmolecules.ddd.annotation.ValueObject
public class ConfirmOrderInput implements Serializable {

    @NotNull
    private String orderId;

    public String getOrderId() {
        return orderId;
    }

    public ConfirmOrderInput setOrderId(String orderId) {
        this.orderId = orderId;
        return this;
    }
}
