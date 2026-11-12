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
@org.springframework.modulith.NamedInterface("events.StockUnavailable")
@org.jmolecules.event.annotation.DomainEvent
public class StockUnavailable implements Serializable {

    @java.io.Serial
    private static final long serialVersionUID = 1L;

    private String orderId;

    private String customerId;

    private String customerEmail;

    private List<StockUnavailableItem> unavailableItems;

    public StockUnavailable addUnavailableItems(StockUnavailableItem unavailableItems) {
        this.unavailableItems.add(unavailableItems);
        return this;
    }

    public String getOrderId() {
        return orderId;
    }

    public StockUnavailable setOrderId(String orderId) {
        this.orderId = orderId;
        return this;
    }

    public String getCustomerId() {
        return customerId;
    }

    public StockUnavailable setCustomerId(String customerId) {
        this.customerId = customerId;
        return this;
    }

    public String getCustomerEmail() {
        return customerEmail;
    }

    public StockUnavailable setCustomerEmail(String customerEmail) {
        this.customerEmail = customerEmail;
        return this;
    }

    public List<StockUnavailableItem> getUnavailableItems() {
        return unavailableItems;
    }

    public StockUnavailable setUnavailableItems(List<StockUnavailableItem> unavailableItems) {
        this.unavailableItems = unavailableItems;
        return this;
    }
}
