package com.arcadiaeditions.catalog.inventory.service.dtos;

import com.arcadiaeditions.catalog.inventory.domain.*;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.math.*;
import java.time.*;
import java.util.*;

/**
 * ReserveStockResult.
 */
@org.jmolecules.ddd.annotation.ValueObject
public class ReserveStockResult implements Serializable {

    @NotNull
    private ReserveStockOutcome outcome;

    private StockReservedResult reserved;

    private StockUnavailableResult unavailable;

    public ReserveStockOutcome getOutcome() {
        return outcome;
    }

    public ReserveStockResult setOutcome(ReserveStockOutcome outcome) {
        this.outcome = outcome;
        return this;
    }

    public StockReservedResult getReserved() {
        return reserved;
    }

    public ReserveStockResult setReserved(StockReservedResult reserved) {
        this.reserved = reserved;
        return this;
    }

    public StockUnavailableResult getUnavailable() {
        return unavailable;
    }

    public ReserveStockResult setUnavailable(StockUnavailableResult unavailable) {
        this.unavailable = unavailable;
        return this;
    }
}
