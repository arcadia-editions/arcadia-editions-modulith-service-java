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
@org.springframework.modulith.NamedInterface("events.PaymentAuthorized")
@org.jmolecules.event.annotation.DomainEvent
public class PaymentAuthorized implements Serializable {

    @java.io.Serial
    private static final long serialVersionUID = 1L;

    private String orderId;

    private String customerId;

    private BigDecimal amount;

    private String currency;

    private String providerReference;

    private Instant authorizedAt;

    public String getOrderId() {
        return orderId;
    }

    public PaymentAuthorized setOrderId(String orderId) {
        this.orderId = orderId;
        return this;
    }

    public String getCustomerId() {
        return customerId;
    }

    public PaymentAuthorized setCustomerId(String customerId) {
        this.customerId = customerId;
        return this;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public PaymentAuthorized setAmount(BigDecimal amount) {
        this.amount = amount;
        return this;
    }

    public String getCurrency() {
        return currency;
    }

    public PaymentAuthorized setCurrency(String currency) {
        this.currency = currency;
        return this;
    }

    public String getProviderReference() {
        return providerReference;
    }

    public PaymentAuthorized setProviderReference(String providerReference) {
        this.providerReference = providerReference;
        return this;
    }

    public Instant getAuthorizedAt() {
        return authorizedAt;
    }

    public PaymentAuthorized setAuthorizedAt(Instant authorizedAt) {
        this.authorizedAt = authorizedAt;
        return this;
    }
}
