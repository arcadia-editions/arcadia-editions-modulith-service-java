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
@org.springframework.modulith.NamedInterface("events.OrderConfirmedItem")
public class OrderConfirmedItem implements Serializable {

    @java.io.Serial
    private static final long serialVersionUID = 1L;

    private String sku;

    private Integer quantity;

    public String getSku() {
        return sku;
    }

    public OrderConfirmedItem setSku(String sku) {
        this.sku = sku;
        return this;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public OrderConfirmedItem setQuantity(Integer quantity) {
        this.quantity = quantity;
        return this;
    }
}
