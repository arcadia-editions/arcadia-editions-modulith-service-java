package com.arcadiaeditions.fulfillment.shipping.dtos;

import com.arcadiaeditions.fulfillment.shipping.domain.*;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.math.*;
import java.time.*;
import java.util.*;

/**
 * ScheduleFulfillmentInput.
 */
@org.jmolecules.ddd.annotation.ValueObject
public class ScheduleFulfillmentInput implements Serializable {

    @NotNull
    private String orderId;

    @NotNull
    private String customerId;

    @NotNull
    private ShippingAddressInput shippingAddress;

    @NotNull
    private List<FulfillmentItemInput> items = new ArrayList<>();

    public ScheduleFulfillmentInput addItems(FulfillmentItemInput items) {
        this.items.add(items);
        return this;
    }

    public String getOrderId() {
        return orderId;
    }

    public ScheduleFulfillmentInput setOrderId(String orderId) {
        this.orderId = orderId;
        return this;
    }

    public String getCustomerId() {
        return customerId;
    }

    public ScheduleFulfillmentInput setCustomerId(String customerId) {
        this.customerId = customerId;
        return this;
    }

    public ShippingAddressInput getShippingAddress() {
        return shippingAddress;
    }

    public ScheduleFulfillmentInput setShippingAddress(ShippingAddressInput shippingAddress) {
        this.shippingAddress = shippingAddress;
        return this;
    }

    public List<FulfillmentItemInput> getItems() {
        return items;
    }

    public ScheduleFulfillmentInput setItems(List<FulfillmentItemInput> items) {
        this.items = items;
        return this;
    }
}
