package com.arcadiaeditions.catalog.inventory.service.dtos;

import com.arcadiaeditions.catalog.inventory.domain.*;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.math.*;
import java.time.*;
import java.util.*;

/**
 * ReserveStockItemInput.
 */
@org.jmolecules.ddd.annotation.ValueObject
public class ReserveStockItemInput implements Serializable {

    @NotNull
    private String sku;

    @NotNull
    @Min(1)
    private Integer quantity;

    public String getSku() {
        return sku;
    }

    public ReserveStockItemInput setSku(String sku) {
        this.sku = sku;
        return this;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public ReserveStockItemInput setQuantity(Integer quantity) {
        this.quantity = quantity;
        return this;
    }
}
