package com.arcadiaeditions.notifications.consumer.service.dtos;

import com.arcadiaeditions.notifications.consumer.domain.*;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.math.*;
import java.time.*;
import java.util.*;

/**
 * SendOrderCancelledNotificationInput.
 */
@org.jmolecules.ddd.annotation.ValueObject
public class SendOrderCancelledNotificationInput implements Serializable {

    @NotNull
    private String orderId;

    @NotNull
    private String customerId;

    @NotNull
    private String reason;

    @NotNull
    private String customerEmail;

    public String getOrderId() {
        return orderId;
    }

    public SendOrderCancelledNotificationInput setOrderId(String orderId) {
        this.orderId = orderId;
        return this;
    }

    public String getCustomerId() {
        return customerId;
    }

    public SendOrderCancelledNotificationInput setCustomerId(String customerId) {
        this.customerId = customerId;
        return this;
    }

    public String getReason() {
        return reason;
    }

    public SendOrderCancelledNotificationInput setReason(String reason) {
        this.reason = reason;
        return this;
    }

    public String getCustomerEmail() {
        return customerEmail;
    }

    public SendOrderCancelledNotificationInput setCustomerEmail(String customerEmail) {
        this.customerEmail = customerEmail;
        return this;
    }
}
