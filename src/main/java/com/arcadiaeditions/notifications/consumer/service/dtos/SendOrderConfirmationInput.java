package com.arcadiaeditions.notifications.consumer.service.dtos;

import com.arcadiaeditions.notifications.consumer.domain.*;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.math.*;
import java.time.*;
import java.util.*;

/**
 * SendOrderConfirmationInput.
 */
@org.jmolecules.ddd.annotation.ValueObject
public class SendOrderConfirmationInput implements Serializable {

    @NotNull
    private String orderId;

    @NotNull
    private String customerId;

    @NotNull
    private String customerEmail;

    public String getOrderId() {
        return orderId;
    }

    public SendOrderConfirmationInput setOrderId(String orderId) {
        this.orderId = orderId;
        return this;
    }

    public String getCustomerId() {
        return customerId;
    }

    public SendOrderConfirmationInput setCustomerId(String customerId) {
        this.customerId = customerId;
        return this;
    }

    public String getCustomerEmail() {
        return customerEmail;
    }

    public SendOrderConfirmationInput setCustomerEmail(String customerEmail) {
        this.customerEmail = customerEmail;
        return this;
    }
}
