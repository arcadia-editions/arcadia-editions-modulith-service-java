package com.arcadiaeditions.orders.checkout.core.application.mappers;

import com.arcadiaeditions.common.mappers.BaseMapper;
import com.arcadiaeditions.orders.checkout.core.domain.*;
import com.arcadiaeditions.orders.checkout.core.inbound.dtos.*;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper(uses = {BaseMapper.class})
public interface EventsMapper {

    EventsMapper INSTANCE = Mappers.getMapper(EventsMapper.class);

    @org.mapstruct.Mapping(target = "amount", source = "totalAmount")
    com.arcadiaeditions.orders.checkout.core.domain.events.OrderCreated asOrderCreated(Order order);

    com.arcadiaeditions.orders.checkout.core.domain.events.OrderConfirmed asOrderConfirmed(Order order);

    @org.mapstruct.Mapping(target = "reason", source = "cancellationReason")
    com.arcadiaeditions.orders.checkout.core.domain.events.OrderCancelled asOrderCancelled(Order order);

    com.arcadiaeditions.orders.checkout.core.domain.events.StockUnavailable asStockUnavailable(Order order);

    /** reason and rejectedAt come from the command, not from the Order. */
    @org.mapstruct.Mapping(target = "reason", ignore = true)
    @org.mapstruct.Mapping(target = "rejectedAt", ignore = true)
    com.arcadiaeditions.orders.checkout.core.domain.events.OrderConfirmationRejected asOrderConfirmationRejected(
            Order order);
}
