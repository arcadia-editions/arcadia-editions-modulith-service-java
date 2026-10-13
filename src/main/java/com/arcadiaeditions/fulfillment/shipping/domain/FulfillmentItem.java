package com.arcadiaeditions.fulfillment.shipping.domain;

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
public class FulfillmentItem implements Serializable {

    @java.io.Serial
    private static final long serialVersionUID = 1L;

    @NotNull
    @Column(name = "sku", nullable = false)
    private String sku;

    @NotNull
    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    public String getSku() {
        return sku;
    }

    public FulfillmentItem setSku(String sku) {
        this.sku = sku;
        return this;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public FulfillmentItem setQuantity(Integer quantity) {
        this.quantity = quantity;
        return this;
    }
}
