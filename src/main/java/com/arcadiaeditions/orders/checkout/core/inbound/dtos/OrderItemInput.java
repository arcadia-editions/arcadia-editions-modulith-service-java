package com.arcadiaeditions.orders.checkout.core.inbound.dtos;

import com.arcadiaeditions.orders.checkout.core.domain.*;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.math.*;
import java.time.*;
import java.util.*;

/**
 * OrderItemInput.
 */
@org.jmolecules.ddd.annotation.ValueObject
public class OrderItemInput implements Serializable {

    @NotNull
    private String sku;

    @NotNull
    @Min(1)
    private Integer quantity;

    @NotNull
    private BigDecimal unitPrice;

    public String getSku() {
        return sku;
    }

    public OrderItemInput setSku(String sku) {
        this.sku = sku;
        return this;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public OrderItemInput setQuantity(Integer quantity) {
        this.quantity = quantity;
        return this;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public OrderItemInput setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
        return this;
    }
}
