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
@org.springframework.modulith.NamedInterface("events.PaymentVoided")
@org.jmolecules.event.annotation.DomainEvent
public class PaymentVoided implements Serializable {

    @java.io.Serial
    private static final long serialVersionUID = 1L;

    private String orderId;

    private String customerId;

    private String providerReference;

    private String reason;

    private Instant voidedAt;

    public String getOrderId() {
        return orderId;
    }

    public PaymentVoided setOrderId(String orderId) {
        this.orderId = orderId;
        return this;
    }

    public String getCustomerId() {
        return customerId;
    }

    public PaymentVoided setCustomerId(String customerId) {
        this.customerId = customerId;
        return this;
    }

    public String getProviderReference() {
        return providerReference;
    }

    public PaymentVoided setProviderReference(String providerReference) {
        this.providerReference = providerReference;
        return this;
    }

    public String getReason() {
        return reason;
    }

    public PaymentVoided setReason(String reason) {
        this.reason = reason;
        return this;
    }

    public Instant getVoidedAt() {
        return voidedAt;
    }

    public PaymentVoided setVoidedAt(Instant voidedAt) {
        this.voidedAt = voidedAt;
        return this;
    }
}
