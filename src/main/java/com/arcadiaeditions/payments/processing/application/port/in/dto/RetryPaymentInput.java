package com.arcadiaeditions.payments.processing.application.port.in.dto;

import com.arcadiaeditions.payments.processing.domain.*;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.math.*;
import java.time.*;
import java.util.*;

/**
 * RetryPaymentInput.
 */
@org.jmolecules.ddd.annotation.ValueObject
public class RetryPaymentInput implements Serializable {

    @NotNull
    private String orderId;

    public String getOrderId() {
        return orderId;
    }

    public RetryPaymentInput setOrderId(String orderId) {
        this.orderId = orderId;
        return this;
    }
}
