package com.arcadiaeditions.notifications.consumer.events.processing.mappers;

import com.arcadiaeditions.common.mappers.BaseMapper;
import com.arcadiaeditions.notifications.consumer.domain.events.*;
import com.arcadiaeditions.notifications.consumer.service.dtos.*;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper(uses = {BaseMapper.class})
public interface PaymentsProcessingEventsListenerMapper {

    PaymentsProcessingEventsListenerMapper INSTANCE = Mappers.getMapper(PaymentsProcessingEventsListenerMapper.class);

    /** Add {@code @Mapping} here when MapStruct cannot infer the conversion. */
    com.arcadiaeditions.notifications.consumer.service.dtos.SendOrderConfirmationInput asSendOrderConfirmationInput(
            com.arcadiaeditions.payments.processing.domain.event.PaymentCaptured event);
}
