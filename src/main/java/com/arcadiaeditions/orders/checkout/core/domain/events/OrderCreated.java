package com.arcadiaeditions.orders.checkout.core.domain.events;

import com.arcadiaeditions.orders.checkout.core.domain.*;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.math.*;
import java.time.*;
import java.util.*;

/**
 *
 */
@org.springframework.modulith.NamedInterface("events.OrderCreated")
@org.jmolecules.event.annotation.DomainEvent
public class OrderCreated implements Serializable {

    @java.io.Serial
    private static final long serialVersionUID = 1L;

    private String orderId;

    private Integer version;

    private String customerId;

    private String customerEmail;

    private String paymentMethodId;

    private BigDecimal amount;

    private String currency;

    public String getOrderId() {
        return orderId;
    }

    public OrderCreated setOrderId(String orderId) {
        this.orderId = orderId;
        return this;
    }

    public Integer getVersion() {
        return version;
    }

    public OrderCreated setVersion(Integer version) {
        this.version = version;
        return this;
    }

    public String getCustomerId() {
        return customerId;
    }

    public OrderCreated setCustomerId(String customerId) {
        this.customerId = customerId;
        return this;
    }

    public String getCustomerEmail() {
        return customerEmail;
    }

    public OrderCreated setCustomerEmail(String customerEmail) {
        this.customerEmail = customerEmail;
        return this;
    }

    public String getPaymentMethodId() {
        return paymentMethodId;
    }

    public OrderCreated setPaymentMethodId(String paymentMethodId) {
        this.paymentMethodId = paymentMethodId;
        return this;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public OrderCreated setAmount(BigDecimal amount) {
        this.amount = amount;
        return this;
    }

    public String getCurrency() {
        return currency;
    }

    public OrderCreated setCurrency(String currency) {
        this.currency = currency;
        return this;
    }
}
