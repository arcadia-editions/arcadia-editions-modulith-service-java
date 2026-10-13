package com.arcadiaeditions.catalog.inventory.service.dtos;

import com.arcadiaeditions.catalog.inventory.domain.*;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.math.*;
import java.time.*;
import java.util.*;

/**
 * ReserveStockInput.
 */
@org.jmolecules.ddd.annotation.ValueObject
public class ReserveStockInput implements Serializable {

    @NotNull
    private String orderId;

    @NotNull
    @Size(min = 1)
    private List<ReserveStockItemInput> items = new ArrayList<>();

    public ReserveStockInput addItems(ReserveStockItemInput items) {
        this.items.add(items);
        return this;
    }

    public String getOrderId() {
        return orderId;
    }

    public ReserveStockInput setOrderId(String orderId) {
        this.orderId = orderId;
        return this;
    }

    public List<ReserveStockItemInput> getItems() {
        return items;
    }

    public ReserveStockInput setItems(List<ReserveStockItemInput> items) {
        this.items = items;
        return this;
    }
}
