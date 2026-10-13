package com.arcadiaeditions.catalog.inventory.service.impl.mappers;

import com.arcadiaeditions.catalog.inventory.domain.*;
import com.arcadiaeditions.catalog.inventory.service.dtos.*;
import com.arcadiaeditions.common.mappers.BaseMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.factory.Mappers;

@Mapper(uses = {BaseMapper.class})
public interface InventoryBackofficeServiceMapper {

    InventoryBackofficeServiceMapper INSTANCE = Mappers.getMapper(InventoryBackofficeServiceMapper.class);

    // input mappings
    // CreateInventoryPositionInputnull-InventoryPosition createInventoryPosition
    InventoryPosition asInventoryPosition(CreateInventoryPositionInput input);

    @Mapping(target = "id", ignore = true)
    InventoryPosition update(@MappingTarget InventoryPosition entity, CreateInventoryPositionInput input);
    // ReceiveStockInputnull-InventoryPosition receiveStock
    InventoryPosition asInventoryPosition(ReceiveStockInput input);

    @Mapping(target = "id", ignore = true)
    InventoryPosition update(@MappingTarget InventoryPosition entity, ReceiveStockInput input);
    // AdjustStockInputnull-InventoryPosition adjustStock
    InventoryPosition asInventoryPosition(AdjustStockInput input);

    @Mapping(target = "id", ignore = true)
    InventoryPosition update(@MappingTarget InventoryPosition entity, AdjustStockInput input);
    // output mappings
}
