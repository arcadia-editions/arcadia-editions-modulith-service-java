package com.arcadiaeditions.payments.processing.adapter.in.event.checkout.mapper;

import com.arcadiaeditions.common.mappers.BaseMapper;
import com.arcadiaeditions.payments.processing.application.port.in.dto.*;
import com.arcadiaeditions.payments.processing.domain.event.*;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper(uses = {BaseMapper.class})
public interface OrdersCheckoutEventsListenerMapper {

    OrdersCheckoutEventsListenerMapper INSTANCE = Mappers.getMapper(OrdersCheckoutEventsListenerMapper.class);

    /** Add {@code @Mapping} here when MapStruct cannot infer the conversion. */
    com.arcadiaeditions.payments.processing.application.port.in.dto.AuthorizePaymentInput asAuthorizePaymentInput(
            com.arcadiaeditions.orders.checkout.core.domain.events.OrderCreated event);
}
