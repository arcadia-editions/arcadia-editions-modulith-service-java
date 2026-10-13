package com.arcadiaeditions.payments.processing.application.port.in.dto;

import com.arcadiaeditions.payments.processing.domain.*;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.math.*;
import java.time.*;
import java.util.*;

/**
 * VoidPaymentInput.
 */
@org.jmolecules.ddd.annotation.ValueObject
public class VoidPaymentInput implements Serializable {

    @NotNull
    private String orderId;

    @NotNull
    private String reason;

    public String getOrderId() {
        return orderId;
    }

    public VoidPaymentInput setOrderId(String orderId) {
        this.orderId = orderId;
        return this;
    }

    public String getReason() {
        return reason;
    }

    public VoidPaymentInput setReason(String reason) {
        this.reason = reason;
        return this;
    }
}
