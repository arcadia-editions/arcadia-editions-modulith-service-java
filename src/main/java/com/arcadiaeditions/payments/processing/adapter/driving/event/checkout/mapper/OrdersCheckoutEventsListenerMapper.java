package com.arcadiaeditions.payments.processing.adapter.driving.event.checkout.mapper;

import com.arcadiaeditions.common.mappers.BaseMapper;
import com.arcadiaeditions.payments.processing.application.port.driving.dto.*;
import com.arcadiaeditions.payments.processing.domain.event.*;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper(uses = {BaseMapper.class})
public interface OrdersCheckoutEventsListenerMapper {

    OrdersCheckoutEventsListenerMapper INSTANCE = Mappers.getMapper(OrdersCheckoutEventsListenerMapper.class);

    /** Add {@code @Mapping} here when MapStruct cannot infer the conversion. */
    com.arcadiaeditions.payments.processing.application.port.driving.dto.AuthorizePaymentInput asAuthorizePaymentInput(
            com.arcadiaeditions.orders.checkout.core.domain.events.OrderCreated event);
}
