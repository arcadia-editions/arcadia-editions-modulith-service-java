package com.arcadiaeditions.orders.checkout.core.inbound.dtos;

import com.arcadiaeditions.orders.checkout.core.domain.*;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.math.*;
import java.time.*;
import java.util.*;

/**
 * StartOrderCheckoutInput.
 */
@org.jmolecules.ddd.annotation.ValueObject
public class StartOrderCheckoutInput implements Serializable {

    @NotNull
    private String customerId;

    @NotNull
    @Size(max = 254)
    private String customerEmail;

    @NotNull
    private String paymentMethodId;

    @NotNull
    @Size(max = 3)
    private String currency;

    @NotNull
    @Size(min = 1)
    private List<OrderItemInput> items = new ArrayList<>();

    @NotNull
    private ShippingAddressInput shippingAddress;

    public StartOrderCheckoutInput addItems(OrderItemInput items) {
        this.items.add(items);
        return this;
    }

    public String getCustomerId() {
        return customerId;
    }

    public StartOrderCheckoutInput setCustomerId(String customerId) {
        this.customerId = customerId;
        return this;
    }

    public String getCustomerEmail() {
        return customerEmail;
    }

    public StartOrderCheckoutInput setCustomerEmail(String customerEmail) {
        this.customerEmail = customerEmail;
        return this;
    }

    public String getPaymentMethodId() {
        return paymentMethodId;
    }

    public StartOrderCheckoutInput setPaymentMethodId(String paymentMethodId) {
        this.paymentMethodId = paymentMethodId;
        return this;
    }

    public String getCurrency() {
        return currency;
    }

    public StartOrderCheckoutInput setCurrency(String currency) {
        this.currency = currency;
        return this;
    }

    public List<OrderItemInput> getItems() {
        return items;
    }

    public StartOrderCheckoutInput setItems(List<OrderItemInput> items) {
        this.items = items;
        return this;
    }

    public ShippingAddressInput getShippingAddress() {
        return shippingAddress;
    }

    public StartOrderCheckoutInput setShippingAddress(ShippingAddressInput shippingAddress) {
        this.shippingAddress = shippingAddress;
        return this;
    }
}
