package com.arcadiaeditions.orders.checkout.core.application.mappers;

import com.arcadiaeditions.common.mappers.BaseMapper;
import com.arcadiaeditions.orders.checkout.core.domain.*;
import com.arcadiaeditions.orders.checkout.core.inbound.dtos.*;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.factory.Mappers;

@Mapper(uses = {BaseMapper.class})
public interface OrdersCheckoutServiceMapper {

    OrdersCheckoutServiceMapper INSTANCE = Mappers.getMapper(OrdersCheckoutServiceMapper.class);

    // input mappings
    // CancelOrderInputnull-Order cancelOrder
    Order asOrder(CancelOrderInput input);

    @Mapping(target = "id", ignore = true)
    Order update(@MappingTarget Order entity, CancelOrderInput input);
    // StartOrderCheckoutInputnull-Order startOrderCheckout
    Order asOrder(StartOrderCheckoutInput input);

    @Mapping(target = "id", ignore = true)
    Order update(@MappingTarget Order entity, StartOrderCheckoutInput input);
    // ConfirmOrderInputnull-Order confirmOrder
    Order asOrder(ConfirmOrderInput input);

    @Mapping(target = "id", ignore = true)
    Order update(@MappingTarget Order entity, ConfirmOrderInput input);
    // output mappings
}
