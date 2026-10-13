package com.arcadiaeditions.payments.processing.application.port.in.dto;

import com.arcadiaeditions.payments.processing.domain.*;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.math.*;
import java.time.*;
import java.util.*;

/**
 * CapturePaymentInput.
 */
@org.jmolecules.ddd.annotation.ValueObject
public class CapturePaymentInput implements Serializable {

    @NotNull
    private String orderId;

    public String getOrderId() {
        return orderId;
    }

    public CapturePaymentInput setOrderId(String orderId) {
        this.orderId = orderId;
        return this;
    }
}
