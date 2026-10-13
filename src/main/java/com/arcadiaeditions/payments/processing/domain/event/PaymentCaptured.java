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
@org.springframework.modulith.NamedInterface("events.PaymentCaptured")
@org.jmolecules.event.annotation.DomainEvent
public class PaymentCaptured implements Serializable {

    @java.io.Serial
    private static final long serialVersionUID = 1L;

    private String orderId;

    private String customerId;

    private String customerEmail;

    private BigDecimal amount;

    private String currency;

    private String providerReference;

    private Instant capturedAt;

    public String getOrderId() {
        return orderId;
    }

    public PaymentCaptured setOrderId(String orderId) {
        this.orderId = orderId;
        return this;
    }

    public String getCustomerId() {
        return customerId;
    }

    public PaymentCaptured setCustomerId(String customerId) {
        this.customerId = customerId;
        return this;
    }

    public String getCustomerEmail() {
        return customerEmail;
    }

    public PaymentCaptured setCustomerEmail(String customerEmail) {
        this.customerEmail = customerEmail;
        return this;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public PaymentCaptured setAmount(BigDecimal amount) {
        this.amount = amount;
        return this;
    }

    public String getCurrency() {
        return currency;
    }

    public PaymentCaptured setCurrency(String currency) {
        this.currency = currency;
        return this;
    }

    public String getProviderReference() {
        return providerReference;
    }

    public PaymentCaptured setProviderReference(String providerReference) {
        this.providerReference = providerReference;
        return this;
    }

    public Instant getCapturedAt() {
        return capturedAt;
    }

    public PaymentCaptured setCapturedAt(Instant capturedAt) {
        this.capturedAt = capturedAt;
        return this;
    }
}
