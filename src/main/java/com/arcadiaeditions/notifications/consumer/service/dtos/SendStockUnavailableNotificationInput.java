package com.arcadiaeditions.notifications.consumer.service.dtos;

import com.arcadiaeditions.notifications.consumer.domain.*;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.math.*;
import java.time.*;
import java.util.*;

/**
 * SendStockUnavailableNotificationInput.
 */
@org.jmolecules.ddd.annotation.ValueObject
public class SendStockUnavailableNotificationInput implements Serializable {

    @NotNull
    private String orderId;

    @NotNull
    private String customerId;

    @NotNull
    private String customerEmail;

    @NotNull
    @Size(min = 1)
    private List<StockUnavailableItemInput> unavailableItems = new ArrayList<>();

    public SendStockUnavailableNotificationInput addUnavailableItems(StockUnavailableItemInput unavailableItems) {
        this.unavailableItems.add(unavailableItems);
        return this;
    }

    public String getOrderId() {
        return orderId;
    }

    public SendStockUnavailableNotificationInput setOrderId(String orderId) {
        this.orderId = orderId;
        return this;
    }

    public String getCustomerId() {
        return customerId;
    }

    public SendStockUnavailableNotificationInput setCustomerId(String customerId) {
        this.customerId = customerId;
        return this;
    }

    public String getCustomerEmail() {
        return customerEmail;
    }

    public SendStockUnavailableNotificationInput setCustomerEmail(String customerEmail) {
        this.customerEmail = customerEmail;
        return this;
    }

    public List<StockUnavailableItemInput> getUnavailableItems() {
        return unavailableItems;
    }

    public SendStockUnavailableNotificationInput setUnavailableItems(List<StockUnavailableItemInput> unavailableItems) {
        this.unavailableItems = unavailableItems;
        return this;
    }
}
