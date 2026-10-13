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
@org.springframework.modulith.NamedInterface("events.StockUnavailableItem")
public class StockUnavailableItem implements Serializable {

    @java.io.Serial
    private static final long serialVersionUID = 1L;

    private String sku;

    private Integer requestedQuantity;

    private Integer availableQuantity;

    public String getSku() {
        return sku;
    }

    public StockUnavailableItem setSku(String sku) {
        this.sku = sku;
        return this;
    }

    public Integer getRequestedQuantity() {
        return requestedQuantity;
    }

    public StockUnavailableItem setRequestedQuantity(Integer requestedQuantity) {
        this.requestedQuantity = requestedQuantity;
        return this;
    }

    public Integer getAvailableQuantity() {
        return availableQuantity;
    }

    public StockUnavailableItem setAvailableQuantity(Integer availableQuantity) {
        this.availableQuantity = availableQuantity;
        return this;
    }
}
