package com.arcadiaeditions.orders.checkout.adapters.events.inventory.mappers;

import com.arcadiaeditions.common.mappers.BaseMapper;
import com.arcadiaeditions.orders.checkout.core.domain.events.*;
import com.arcadiaeditions.orders.checkout.core.inbound.dtos.*;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper(uses = {BaseMapper.class})
public interface CatalogInventoryEventsListenerMapper {

    CatalogInventoryEventsListenerMapper INSTANCE = Mappers.getMapper(CatalogInventoryEventsListenerMapper.class);

    /** Add {@code @Mapping} here when MapStruct cannot infer the conversion. */
    com.arcadiaeditions.orders.checkout.core.inbound.dtos.CancelOrderInput asCancelOrderInput(
            com.arcadiaeditions.catalog.inventory.domain.events.StockReleased event);
}
