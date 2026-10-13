package com.arcadiaeditions.fulfillment.shipping.checkout.mappers;

import com.arcadiaeditions.common.mappers.BaseMapper;
import com.arcadiaeditions.fulfillment.shipping.domain.events.*;
import com.arcadiaeditions.fulfillment.shipping.dtos.*;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper(uses = {BaseMapper.class})
public interface OrdersCheckoutEventsListenerMapper {

    OrdersCheckoutEventsListenerMapper INSTANCE = Mappers.getMapper(OrdersCheckoutEventsListenerMapper.class);

    /** Add {@code @Mapping} here when MapStruct cannot infer the conversion. */
    com.arcadiaeditions.fulfillment.shipping.dtos.ScheduleFulfillmentInput asScheduleFulfillmentInput(
            com.arcadiaeditions.orders.checkout.core.domain.events.OrderConfirmed event);
}
