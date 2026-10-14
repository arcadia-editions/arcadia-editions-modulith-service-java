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
@org.springframework.modulith.NamedInterface("events.PaymentRetried")
@org.jmolecules.event.annotation.DomainEvent
public class PaymentRetried implements Serializable {

    @java.io.Serial
    private static final long serialVersionUID = 1L;

    private String orderId;

    private String customerId;

    private Integer attemptCount;

    private Instant retriedAt;

    public String getOrderId() {
        return orderId;
    }

    public PaymentRetried setOrderId(String orderId) {
        this.orderId = orderId;
        return this;
    }

    public String getCustomerId() {
        return customerId;
    }

    public PaymentRetried setCustomerId(String customerId) {
        this.customerId = customerId;
        return this;
    }

    public Integer getAttemptCount() {
        return attemptCount;
    }

    public PaymentRetried setAttemptCount(Integer attemptCount) {
        this.attemptCount = attemptCount;
        return this;
    }

    public Instant getRetriedAt() {
        return retriedAt;
    }

    public PaymentRetried setRetriedAt(Instant retriedAt) {
        this.retriedAt = retriedAt;
        return this;
    }
}
