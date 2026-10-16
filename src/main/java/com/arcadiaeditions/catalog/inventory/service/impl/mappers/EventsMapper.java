package com.arcadiaeditions.catalog.inventory.service.impl.mappers;

import com.arcadiaeditions.catalog.inventory.domain.*;
import com.arcadiaeditions.catalog.inventory.service.dtos.*;
import com.arcadiaeditions.common.mappers.BaseMapper;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper(uses = {BaseMapper.class})
public interface EventsMapper {

    EventsMapper INSTANCE = Mappers.getMapper(EventsMapper.class);

    com.arcadiaeditions.catalog.inventory.domain.events.StockAdjusted asStockAdjusted(
            InventoryPosition inventoryPosition);

    /** One event for the whole order: orderId, confirmedAt and items come from the confirmed reservations. */
    default com.arcadiaeditions.catalog.inventory.domain.events.StockReservationConfirmed asStockReservationConfirmed(
            List<StockReservation> stockReservations) {
        var first = stockReservations.get(0);
        return new com.arcadiaeditions.catalog.inventory.domain.events.StockReservationConfirmed()
                .setOrderId(first.getOrderId())
                .setConfirmedAt(first.getConfirmedAt())
                .setItems(asStockReservationConfirmedItems(stockReservations));
    }

    List<com.arcadiaeditions.catalog.inventory.domain.events.StockReservationConfirmedItem>
            asStockReservationConfirmedItems(List<StockReservation> stockReservations);

    com.arcadiaeditions.catalog.inventory.domain.events.StockReservationConfirmedItem asStockReservationConfirmedItem(
            StockReservation stockReservation);

    /** One event for the whole order: orderId, releasedAt and reason from the reservations, items as a list. */
    default com.arcadiaeditions.catalog.inventory.domain.events.StockReleased asStockReleased(
            List<StockReservation> stockReservations, ReleaseStockInput input) {
        var first = stockReservations.get(0);
        return new com.arcadiaeditions.catalog.inventory.domain.events.StockReleased()
                .setOrderId(input.getOrderId())
                .setReason(input.getReason())
                .setReleasedAt(first.getReleasedAt())
                .setItems(asStockReleasedItems(stockReservations));
    }

    List<com.arcadiaeditions.catalog.inventory.domain.events.StockReleasedItem> asStockReleasedItems(
            List<StockReservation> stockReservations);

    com.arcadiaeditions.catalog.inventory.domain.events.StockReleasedItem asStockReleasedItem(
            StockReservation stockReservation);

    com.arcadiaeditions.catalog.inventory.domain.events.StockReceived asStockReceived(
            InventoryPosition inventoryPosition);
}
