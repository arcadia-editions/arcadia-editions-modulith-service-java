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
@org.springframework.modulith.NamedInterface("events.PaymentDeclined")
@org.jmolecules.event.annotation.DomainEvent
public class PaymentDeclined implements Serializable {

    @java.io.Serial
    private static final long serialVersionUID = 1L;

    private String orderId;

    private String customerId;

    private BigDecimal amount;

    private String currency;

    private String reason;

    private Instant declinedAt;

    public String getOrderId() {
        return orderId;
    }

    public PaymentDeclined setOrderId(String orderId) {
        this.orderId = orderId;
        return this;
    }

    public String getCustomerId() {
        return customerId;
    }

    public PaymentDeclined setCustomerId(String customerId) {
        this.customerId = customerId;
        return this;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public PaymentDeclined setAmount(BigDecimal amount) {
        this.amount = amount;
        return this;
    }

    public String getCurrency() {
        return currency;
    }

    public PaymentDeclined setCurrency(String currency) {
        this.currency = currency;
        return this;
    }

    public String getReason() {
        return reason;
    }

    public PaymentDeclined setReason(String reason) {
        this.reason = reason;
        return this;
    }

    public Instant getDeclinedAt() {
        return declinedAt;
    }

    public PaymentDeclined setDeclinedAt(Instant declinedAt) {
        this.declinedAt = declinedAt;
        return this;
    }
}
