package com.arcadiaeditions.catalog.inventory.events;

import com.arcadiaeditions.catalog.inventory.domain.*;
import com.arcadiaeditions.catalog.inventory.domain.events.*;

public interface EventPublisher {

    void onStockAdjusted(StockAdjusted event);

    void onStockReservationConfirmed(StockReservationConfirmed event);

    void onStockReleased(StockReleased event);

    void onStockReservationConfirmedItem(StockReservationConfirmedItem event);

    void onStockReceived(StockReceived event);

    void onStockReleasedItem(StockReleasedItem event);
}
