package com.arcadiaeditions.orders.checkout.adapters.events.processing.mappers;

import com.arcadiaeditions.common.mappers.BaseMapper;
import com.arcadiaeditions.orders.checkout.core.domain.events.*;
import com.arcadiaeditions.orders.checkout.core.inbound.dtos.*;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper(uses = {BaseMapper.class})
public interface PaymentsProcessingEventsListenerMapper {

    PaymentsProcessingEventsListenerMapper INSTANCE = Mappers.getMapper(PaymentsProcessingEventsListenerMapper.class);

    /** Add {@code @Mapping} here when MapStruct cannot infer the conversion. */
    com.arcadiaeditions.orders.checkout.core.inbound.dtos.ConfirmOrderInput asConfirmOrderInput(
            com.arcadiaeditions.payments.processing.domain.event.PaymentAuthorized event);
}
