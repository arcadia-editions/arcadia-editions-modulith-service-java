package com.arcadiaeditions.catalog.inventory.service.dtos;

import com.arcadiaeditions.catalog.inventory.domain.*;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.math.*;
import java.time.*;
import java.util.*;

/**
 * ReleaseStockInput.
 */
@org.jmolecules.ddd.annotation.ValueObject
public class ReleaseStockInput implements Serializable {

    @NotNull
    private String orderId;

    @NotNull
    private StockReleaseReason reason;

    public String getOrderId() {
        return orderId;
    }

    public ReleaseStockInput setOrderId(String orderId) {
        this.orderId = orderId;
        return this;
    }

    public StockReleaseReason getReason() {
        return reason;
    }

    public ReleaseStockInput setReason(StockReleaseReason reason) {
        this.reason = reason;
        return this;
    }
}
