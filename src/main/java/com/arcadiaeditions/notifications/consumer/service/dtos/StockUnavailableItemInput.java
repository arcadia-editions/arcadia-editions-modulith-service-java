package com.arcadiaeditions.notifications.consumer.service.dtos;

import com.arcadiaeditions.notifications.consumer.domain.*;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.math.*;
import java.time.*;
import java.util.*;

/**
 * StockUnavailableItemInput.
 */
@org.jmolecules.ddd.annotation.ValueObject
public class StockUnavailableItemInput implements Serializable {

    @NotNull
    private String sku;

    private Integer requestedQuantity;

    private Integer availableQuantity;

    public String getSku() {
        return sku;
    }

    public StockUnavailableItemInput setSku(String sku) {
        this.sku = sku;
        return this;
    }

    public Integer getRequestedQuantity() {
        return requestedQuantity;
    }

    public StockUnavailableItemInput setRequestedQuantity(Integer requestedQuantity) {
        this.requestedQuantity = requestedQuantity;
        return this;
    }

    public Integer getAvailableQuantity() {
        return availableQuantity;
    }

    public StockUnavailableItemInput setAvailableQuantity(Integer availableQuantity) {
        this.availableQuantity = availableQuantity;
        return this;
    }
}
