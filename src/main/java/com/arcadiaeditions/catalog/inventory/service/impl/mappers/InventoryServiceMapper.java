package com.arcadiaeditions.catalog.inventory.service.impl.mappers;

import com.arcadiaeditions.catalog.inventory.domain.*;
import com.arcadiaeditions.catalog.inventory.service.dtos.*;
import com.arcadiaeditions.common.mappers.BaseMapper;
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
    // StockReservation-ReserveStockResult reserveStock
    ReserveStockResult asReserveStockResult(StockReservation entity);
}
