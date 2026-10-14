package com.arcadiaeditions.payments.processing.domain.event;

import com.arcadiaeditions.payments.processing.domain.*;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.math.*;
import java.time.*;
import java.util.*;

/**
 *
 */
@org.springframework.modulith.NamedInterface("events.PaymentRetryExhausted")
@org.jmolecules.event.annotation.DomainEvent
public class PaymentRetryExhausted implements Serializable {

    @java.io.Serial
    private static final long serialVersionUID = 1L;

    private String orderId;

    private String customerId;

    private Integer attemptCount;

    private String reason;

    private Instant exhaustedAt;

    public String getOrderId() {
        return orderId;
    }

    public PaymentRetryExhausted setOrderId(String orderId) {
        this.orderId = orderId;
        return this;
    }

    public String getCustomerId() {
        return customerId;
    }

    public PaymentRetryExhausted setCustomerId(String customerId) {
        this.customerId = customerId;
        return this;
    }

    public Integer getAttemptCount() {
        return attemptCount;
    }

    public PaymentRetryExhausted setAttemptCount(Integer attemptCount) {
        this.attemptCount = attemptCount;
        return this;
    }

    public String getReason() {
        return reason;
    }

    public PaymentRetryExhausted setReason(String reason) {
        this.reason = reason;
        return this;
    }

    public Instant getExhaustedAt() {
        return exhaustedAt;
    }

    public PaymentRetryExhausted setExhaustedAt(Instant exhaustedAt) {
        this.exhaustedAt = exhaustedAt;
        return this;
    }
}
