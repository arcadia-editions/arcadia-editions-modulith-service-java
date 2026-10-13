package com.arcadiaeditions.payments.processing.application.port.in.dto;

import com.arcadiaeditions.payments.processing.domain.*;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.math.*;
import java.time.*;
import java.util.*;

/**
 * AuthorizePaymentInput.
 */
@org.jmolecules.ddd.annotation.ValueObject
public class AuthorizePaymentInput implements Serializable {

    @NotNull
    private String orderId;

    @NotNull
    private String customerId;

    @NotNull
    private String customerEmail;

    @NotNull
    private BigDecimal amount;

    @NotNull
    private String currency;

    @NotNull
    private String paymentMethodId;

    public String getOrderId() {
        return orderId;
    }

    public AuthorizePaymentInput setOrderId(String orderId) {
        this.orderId = orderId;
        return this;
    }

    public String getCustomerId() {
        return customerId;
    }

    public AuthorizePaymentInput setCustomerId(String customerId) {
        this.customerId = customerId;
        return this;
    }

    public String getCustomerEmail() {
        return customerEmail;
    }

    public AuthorizePaymentInput setCustomerEmail(String customerEmail) {
        this.customerEmail = customerEmail;
        return this;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public AuthorizePaymentInput setAmount(BigDecimal amount) {
        this.amount = amount;
        return this;
    }

    public String getCurrency() {
        return currency;
    }

    public AuthorizePaymentInput setCurrency(String currency) {
        this.currency = currency;
        return this;
    }

    public String getPaymentMethodId() {
        return paymentMethodId;
    }

    public AuthorizePaymentInput setPaymentMethodId(String paymentMethodId) {
        this.paymentMethodId = paymentMethodId;
        return this;
    }
}
