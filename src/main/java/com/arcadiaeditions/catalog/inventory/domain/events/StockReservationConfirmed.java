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
@org.springframework.modulith.NamedInterface("events.StockReservationConfirmed")
@org.jmolecules.event.annotation.DomainEvent
@org.jmolecules.architecture.layered.DomainLayer
public class StockReservationConfirmed implements Serializable {

    @java.io.Serial
    private static final long serialVersionUID = 1L;

    private String orderId;

    private Instant confirmedAt;

    private List<StockReservationConfirmedItem> items;

    public StockReservationConfirmed addItems(StockReservationConfirmedItem items) {
        this.items.add(items);
        return this;
    }

    public String getOrderId() {
        return orderId;
    }

    public StockReservationConfirmed setOrderId(String orderId) {
        this.orderId = orderId;
        return this;
    }

    public Instant getConfirmedAt() {
        return confirmedAt;
    }

    public StockReservationConfirmed setConfirmedAt(Instant confirmedAt) {
        this.confirmedAt = confirmedAt;
        return this;
    }

    public List<StockReservationConfirmedItem> getItems() {
        return items;
    }

    public StockReservationConfirmed setItems(List<StockReservationConfirmedItem> items) {
        this.items = items;
        return this;
    }
}
