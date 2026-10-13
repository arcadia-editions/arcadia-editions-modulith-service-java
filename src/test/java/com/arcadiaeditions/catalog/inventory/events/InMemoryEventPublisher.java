package com.arcadiaeditions.catalog.inventory.events;

import com.arcadiaeditions.catalog.inventory.domain.*;
import com.arcadiaeditions.catalog.inventory.domain.events.*;
import java.util.ArrayList;
import java.util.List;

public class InMemoryEventPublisher implements EventPublisher {

    private final List<Object> events = new ArrayList<>();

    public List<Object> getEvents() {
        return events;
    }

    public void onStockAdjusted(StockAdjusted event) {
        events.add(event);
    }

    public void onStockReservationConfirmed(StockReservationConfirmed event) {
        events.add(event);
    }

    public void onStockReleased(StockReleased event) {
        events.add(event);
    }

    public void onStockReservationConfirmedItem(StockReservationConfirmedItem event) {
        events.add(event);
    }

    public void onStockReceived(StockReceived event) {
        events.add(event);
    }

    public void onStockReleasedItem(StockReleasedItem event) {
        events.add(event);
    }
}
