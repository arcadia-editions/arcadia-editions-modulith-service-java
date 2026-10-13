package com.arcadiaeditions.orders.checkout.core.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.math.*;
import java.time.*;
import java.util.*;

/**
 *
 */
// @Embeddable // json embedded
@org.jmolecules.ddd.annotation.ValueObject
public class OrderItem implements Serializable {

    @java.io.Serial
    private static final long serialVersionUID = 1L;

    @NotNull
    @Column(name = "sku", nullable = false)
    private String sku;

    @NotNull
    @Min(1)
    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @NotNull
    @Column(name = "unit_price", nullable = false)
    private BigDecimal unitPrice;

    public String getSku() {
        return sku;
    }

    public OrderItem setSku(String sku) {
        this.sku = sku;
        return this;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public OrderItem setQuantity(Integer quantity) {
        this.quantity = quantity;
        return this;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public OrderItem setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
        return this;
    }
}
