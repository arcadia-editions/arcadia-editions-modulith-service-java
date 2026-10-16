package com.arcadiaeditions.orders.checkout.core.application.mappers;

import com.arcadiaeditions.common.mappers.BaseMapper;
import com.arcadiaeditions.orders.checkout.core.domain.*;
import com.arcadiaeditions.orders.checkout.core.inbound.dtos.*;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper(uses = {BaseMapper.class})
public interface EventsMapper {

    EventsMapper INSTANCE = Mappers.getMapper(EventsMapper.class);

    @Mapping(target = "amount", source = "totalAmount")
    com.arcadiaeditions.orders.checkout.core.domain.events.OrderCreated asOrderCreated(Order order);

    com.arcadiaeditions.orders.checkout.core.domain.events.OrderConfirmed asOrderConfirmed(Order order);

    @Mapping(target = "reason", source = "cancellationReason")
    com.arcadiaeditions.orders.checkout.core.domain.events.OrderCancelled asOrderCancelled(Order order);

    @Mapping(target = "orderId", source = "order.orderId")
    @Mapping(target = "customerId", source = "order.customerId")
    @Mapping(target = "customerEmail", source = "order.customerEmail")
    @Mapping(target = "unavailableItems", source = "result.unavailable.unavailableItems")
    com.arcadiaeditions.orders.checkout.core.domain.events.StockUnavailable asStockUnavailable(
            Order order, com.arcadiaeditions.catalog.inventory.service.dtos.ReserveStockResult result);
}
