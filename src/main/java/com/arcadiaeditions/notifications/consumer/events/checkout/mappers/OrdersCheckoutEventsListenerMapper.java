package com.arcadiaeditions.notifications.consumer.events.checkout.mappers;

import com.arcadiaeditions.common.mappers.BaseMapper;
import com.arcadiaeditions.notifications.consumer.domain.events.*;
import com.arcadiaeditions.notifications.consumer.service.dtos.*;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper(uses = {BaseMapper.class})
public interface OrdersCheckoutEventsListenerMapper {

    OrdersCheckoutEventsListenerMapper INSTANCE = Mappers.getMapper(OrdersCheckoutEventsListenerMapper.class);

    /** Add {@code @Mapping} here when MapStruct cannot infer the conversion. */
    com.arcadiaeditions.notifications.consumer.service.dtos.SendStockUnavailableNotificationInput
            asSendStockUnavailableNotificationInput(
                    com.arcadiaeditions.orders.checkout.core.domain.events.StockUnavailable event);

    /** Add {@code @Mapping} here when MapStruct cannot infer the conversion. */
    com.arcadiaeditions.notifications.consumer.service.dtos.SendOrderCancelledNotificationInput
            asSendOrderCancelledNotificationInput(
                    com.arcadiaeditions.orders.checkout.core.domain.events.OrderCancelled event);
}
