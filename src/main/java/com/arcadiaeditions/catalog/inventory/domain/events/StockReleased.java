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
@org.springframework.modulith.NamedInterface("events.StockReleased")
@org.jmolecules.event.annotation.DomainEvent
@org.jmolecules.architecture.layered.DomainLayer
public class StockReleased implements Serializable {

    @java.io.Serial
    private static final long serialVersionUID = 1L;

    private String orderId;

    private StockReleaseReason reason;

    private Instant releasedAt;

    private List<StockReleasedItem> items;

    public StockReleased addItems(StockReleasedItem items) {
        this.items.add(items);
        return this;
    }

    public String getOrderId() {
        return orderId;
    }

    public StockReleased setOrderId(String orderId) {
        this.orderId = orderId;
        return this;
    }

    public StockReleaseReason getReason() {
        return reason;
    }

    public StockReleased setReason(StockReleaseReason reason) {
        this.reason = reason;
        return this;
    }

    public Instant getReleasedAt() {
        return releasedAt;
    }

    public StockReleased setReleasedAt(Instant releasedAt) {
        this.releasedAt = releasedAt;
        return this;
    }

    public List<StockReleasedItem> getItems() {
        return items;
    }

    public StockReleased setItems(List<StockReleasedItem> items) {
        this.items = items;
        return this;
    }
}
