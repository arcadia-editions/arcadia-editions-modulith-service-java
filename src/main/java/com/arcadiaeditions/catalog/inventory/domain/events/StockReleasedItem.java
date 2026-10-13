package com.arcadiaeditions.catalog.inventory.domain.events;

import com.arcadiaeditions.catalog.inventory.domain.*;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.math.*;
import java.time.*;
import java.util.*;

/**
 *
 */
@org.springframework.modulith.NamedInterface("events.StockReleasedItem")
public class StockReleasedItem implements Serializable {

    @java.io.Serial
    private static final long serialVersionUID = 1L;

    private String reservationId;

    private String sku;

    private Integer quantity;

    public String getReservationId() {
        return reservationId;
    }

    public StockReleasedItem setReservationId(String reservationId) {
        this.reservationId = reservationId;
        return this;
    }

    public String getSku() {
        return sku;
    }

    public StockReleasedItem setSku(String sku) {
        this.sku = sku;
        return this;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public StockReleasedItem setQuantity(Integer quantity) {
        this.quantity = quantity;
        return this;
    }
}
