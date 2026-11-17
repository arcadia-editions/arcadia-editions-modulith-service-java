package com.arcadiaeditions.payments.processing.application.port.driving.dto;

import com.arcadiaeditions.payments.processing.domain.*;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.math.*;
import java.time.*;
import java.util.*;

/**
 * PaymentStartedResult.
 */
@org.jmolecules.ddd.annotation.ValueObject
public class PaymentStartedResult implements Serializable {

    private String orderId;

    private PaymentStatus status;

    public String getOrderId() {
        return orderId;
    }

    public PaymentStartedResult setOrderId(String orderId) {
        this.orderId = orderId;
        return this;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public PaymentStartedResult setStatus(PaymentStatus status) {
        this.status = status;
        return this;
    }
}
