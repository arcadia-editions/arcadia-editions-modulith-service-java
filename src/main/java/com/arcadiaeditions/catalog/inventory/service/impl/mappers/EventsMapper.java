package com.arcadiaeditions.catalog.inventory.service.impl.mappers;

import com.arcadiaeditions.catalog.inventory.domain.*;
import com.arcadiaeditions.catalog.inventory.service.dtos.*;
import com.arcadiaeditions.common.mappers.BaseMapper;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper(uses = {BaseMapper.class})
public interface EventsMapper {

    EventsMapper INSTANCE = Mappers.getMapper(EventsMapper.class);

    com.arcadiaeditions.catalog.inventory.domain.events.StockAdjusted asStockAdjusted(
            InventoryPosition inventoryPosition);

    com.arcadiaeditions.catalog.inventory.domain.events.StockReservationConfirmed asStockReservationConfirmed(
            StockReservation stockReservation);

    com.arcadiaeditions.catalog.inventory.domain.events.StockReleased asStockReleased(
            StockReservation stockReservation);

    com.arcadiaeditions.catalog.inventory.domain.events.StockReceived asStockReceived(
            InventoryPosition inventoryPosition);
}
