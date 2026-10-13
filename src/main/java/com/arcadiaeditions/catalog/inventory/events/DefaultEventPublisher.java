package com.arcadiaeditions.catalog.inventory.events;

import com.arcadiaeditions.catalog.inventory.domain.*;
import com.arcadiaeditions.catalog.inventory.domain.events.*;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
@org.jmolecules.architecture.layered.InfrastructureLayer
public class DefaultEventPublisher implements EventPublisher {

    private final ApplicationEventPublisher applicationEventPublisher;

    public DefaultEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
        this.applicationEventPublisher = applicationEventPublisher;
    }

    public void onStockAdjusted(StockAdjusted event) {
        applicationEventPublisher.publishEvent(event);
    }

    public void onStockReservationConfirmed(StockReservationConfirmed event) {
        applicationEventPublisher.publishEvent(event);
    }

    public void onStockReleased(StockReleased event) {
        applicationEventPublisher.publishEvent(event);
    }

    public void onStockReservationConfirmedItem(StockReservationConfirmedItem event) {
        applicationEventPublisher.publishEvent(event);
    }

    public void onStockReceived(StockReceived event) {
        applicationEventPublisher.publishEvent(event);
    }

    public void onStockReleasedItem(StockReleasedItem event) {
        applicationEventPublisher.publishEvent(event);
    }
}
