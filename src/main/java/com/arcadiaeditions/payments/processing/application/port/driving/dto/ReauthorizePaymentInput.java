package com.arcadiaeditions.payments.processing.application.port.driving.dto;

import com.arcadiaeditions.payments.processing.domain.*;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.math.*;
import java.time.*;
import java.util.*;

/**
 * ReauthorizePaymentInput.
 */
@org.jmolecules.ddd.annotation.ValueObject
public class ReauthorizePaymentInput implements Serializable {

    @NotNull
    private String orderId;

    @NotNull
    private Integer attemptCount;

    public String getOrderId() {
        return orderId;
    }

    public ReauthorizePaymentInput setOrderId(String orderId) {
        this.orderId = orderId;
        return this;
    }

    public Integer getAttemptCount() {
        return attemptCount;
    }

    public ReauthorizePaymentInput setAttemptCount(Integer attemptCount) {
        this.attemptCount = attemptCount;
        return this;
    }
}
