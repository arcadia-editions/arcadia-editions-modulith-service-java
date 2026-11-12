package com.arcadiaeditions.catalog.inventory.service.dtos;

import com.arcadiaeditions.catalog.inventory.domain.*;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.math.*;
import java.time.*;
import java.util.*;

/**
 * StockUnavailableLine.
 */
@org.jmolecules.ddd.annotation.ValueObject
public class StockUnavailableLine implements Serializable {

    private String sku;

    private Integer requestedQuantity;

    private Integer availableQuantity;

    public String getSku() {
        return sku;
    }

    public StockUnavailableLine setSku(String sku) {
        this.sku = sku;
        return this;
    }

    public Integer getRequestedQuantity() {
        return requestedQuantity;
    }

    public StockUnavailableLine setRequestedQuantity(Integer requestedQuantity) {
        this.requestedQuantity = requestedQuantity;
        return this;
    }

    public Integer getAvailableQuantity() {
        return availableQuantity;
    }

    public StockUnavailableLine setAvailableQuantity(Integer availableQuantity) {
        this.availableQuantity = availableQuantity;
        return this;
    }
}
