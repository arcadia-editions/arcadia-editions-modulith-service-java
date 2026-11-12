package com.arcadiaeditions.catalog.inventory.service.dtos;

import com.arcadiaeditions.catalog.inventory.domain.*;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.math.*;
import java.time.*;
import java.util.*;

/**
 * StockUnavailableResult.
 */
@org.jmolecules.ddd.annotation.ValueObject
public class StockUnavailableResult implements Serializable {

    private String orderId;

    private List<StockUnavailableLine> unavailableItems = new ArrayList<>();

    public StockUnavailableResult addUnavailableItems(StockUnavailableLine unavailableItems) {
        this.unavailableItems.add(unavailableItems);
        return this;
    }

    public String getOrderId() {
        return orderId;
    }

    public StockUnavailableResult setOrderId(String orderId) {
        this.orderId = orderId;
        return this;
    }

    public List<StockUnavailableLine> getUnavailableItems() {
        return unavailableItems;
    }

    public StockUnavailableResult setUnavailableItems(List<StockUnavailableLine> unavailableItems) {
        this.unavailableItems = unavailableItems;
        return this;
    }
}
