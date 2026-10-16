package com.arcadiaeditions.catalog.inventory.service.impl.mappers;

import com.arcadiaeditions.catalog.inventory.domain.*;
import com.arcadiaeditions.catalog.inventory.service.dtos.*;
import com.arcadiaeditions.common.mappers.BaseMapper;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.factory.Mappers;

@Mapper(uses = {BaseMapper.class})
public interface InventoryServiceMapper {

    InventoryServiceMapper INSTANCE = Mappers.getMapper(InventoryServiceMapper.class);

    // input mappings
    // ReserveStockInputnull-StockReservation reserveStock
    StockReservation asStockReservation(ReserveStockInput input);

    @Mapping(target = "id", ignore = true)
    StockReservation update(@MappingTarget StockReservation entity, ReserveStockInput input);
    // ConfirmStockReservationInputnull-StockReservation confirmStockReservation
    StockReservation asStockReservation(ConfirmStockReservationInput input);

    @Mapping(target = "id", ignore = true)
    StockReservation update(@MappingTarget StockReservation entity, ConfirmStockReservationInput input);
    // ReleaseStockInputnull-StockReservation releaseStock
    StockReservation asStockReservation(ReleaseStockInput input);

    @Mapping(target = "id", ignore = true)
    StockReservation update(@MappingTarget StockReservation entity, ReleaseStockInput input);
    // output mappings
    // reserveStock outcome RESERVED: reservation lines -> StockReservedResult -> ReserveStockResult
    StockReservedLine asStockReservedLine(StockReservation entity);

    @Mapping(target = "reservations", source = "reservations")
    StockReservedResult asStockReservedResult(
            String orderId, java.time.Instant expiresAt, List<StockReservation> reservations);

    @Mapping(target = "outcome", constant = "RESERVED")
    @Mapping(target = "reserved", source = "reserved")
    @Mapping(target = "unavailable", ignore = true)
    ReserveStockResult asReserveStockResult(StockReservedResult reserved);

    // reserveStock outcome UNAVAILABLE: short lines -> StockUnavailableResult -> ReserveStockResult
    @Mapping(target = "sku", source = "sku")
    @Mapping(target = "requestedQuantity", source = "requested")
    @Mapping(target = "availableQuantity", source = "available")
    StockUnavailableLine asStockUnavailableLine(String sku, Integer requested, Integer available);

    @Mapping(target = "unavailableItems", source = "unavailableItems")
    StockUnavailableResult asStockUnavailableResult(String orderId, List<StockUnavailableLine> unavailableItems);

    @Mapping(target = "outcome", constant = "UNAVAILABLE")
    @Mapping(target = "unavailable", source = "unavailable")
    @Mapping(target = "reserved", ignore = true)
    ReserveStockResult asReserveStockResult(StockUnavailableResult unavailable);
}
