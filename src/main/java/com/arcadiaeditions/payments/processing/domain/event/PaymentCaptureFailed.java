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
@org.springframework.modulith.NamedInterface("events.PaymentCaptureFailed")
@org.jmolecules.event.annotation.DomainEvent
public class PaymentCaptureFailed implements Serializable {

    @java.io.Serial
    private static final long serialVersionUID = 1L;

    private String orderId;

    private String customerId;

    private String reason;

    private Instant failedAt;

    public String getOrderId() {
        return orderId;
    }

    public PaymentCaptureFailed setOrderId(String orderId) {
        this.orderId = orderId;
        return this;
    }

    public String getCustomerId() {
        return customerId;
    }

    public PaymentCaptureFailed setCustomerId(String customerId) {
        this.customerId = customerId;
        return this;
    }

    public String getReason() {
        return reason;
    }

    public PaymentCaptureFailed setReason(String reason) {
        this.reason = reason;
        return this;
    }

    public Instant getFailedAt() {
        return failedAt;
    }

    public PaymentCaptureFailed setFailedAt(Instant failedAt) {
        this.failedAt = failedAt;
        return this;
    }
}
