package com.arcadiaeditions.catalog.inventory.service.dtos;

import com.arcadiaeditions.catalog.inventory.domain.*;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.math.*;
import java.time.*;
import java.util.*;

/**
 * AdjustStockInput.
 */
@org.jmolecules.ddd.annotation.ValueObject
public class AdjustStockInput implements Serializable {

    @NotNull
    private String sku;

    @NotNull
    private Integer quantityDelta;

    @NotNull
    private StockAdjustmentReason reason;

    private String reference;

    public String getSku() {
        return sku;
    }

    public AdjustStockInput setSku(String sku) {
        this.sku = sku;
        return this;
    }

    public Integer getQuantityDelta() {
        return quantityDelta;
    }

    public AdjustStockInput setQuantityDelta(Integer quantityDelta) {
        this.quantityDelta = quantityDelta;
        return this;
    }

    public StockAdjustmentReason getReason() {
        return reason;
    }

    public AdjustStockInput setReason(StockAdjustmentReason reason) {
        this.reason = reason;
        return this;
    }

    public String getReference() {
        return reference;
    }

    public AdjustStockInput setReference(String reference) {
        this.reference = reference;
        return this;
    }
}
