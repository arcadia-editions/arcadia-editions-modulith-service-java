package com.arcadiaeditions.catalog.inventory.service.dtos;

import com.arcadiaeditions.catalog.inventory.domain.*;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.math.*;
import java.time.*;
import java.util.*;

/**
 * ReceiveStockInput.
 */
@org.jmolecules.ddd.annotation.ValueObject
public class ReceiveStockInput implements Serializable {

    @NotNull
    private String sku;

    @NotNull
    private Integer quantity;

    private String reason;

    private String reference;

    public String getSku() {
        return sku;
    }

    public ReceiveStockInput setSku(String sku) {
        this.sku = sku;
        return this;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public ReceiveStockInput setQuantity(Integer quantity) {
        this.quantity = quantity;
        return this;
    }

    public String getReason() {
        return reason;
    }

    public ReceiveStockInput setReason(String reason) {
        this.reason = reason;
        return this;
    }

    public String getReference() {
        return reference;
    }

    public ReceiveStockInput setReference(String reference) {
        this.reference = reference;
        return this;
    }
}
