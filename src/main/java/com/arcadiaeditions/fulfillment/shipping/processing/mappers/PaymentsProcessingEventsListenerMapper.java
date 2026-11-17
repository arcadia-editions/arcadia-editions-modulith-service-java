package com.arcadiaeditions.fulfillment.shipping.processing.mappers;

import com.arcadiaeditions.common.mappers.BaseMapper;
import com.arcadiaeditions.fulfillment.shipping.domain.events.*;
import com.arcadiaeditions.fulfillment.shipping.dtos.*;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper(uses = {BaseMapper.class})
public interface PaymentsProcessingEventsListenerMapper {

    PaymentsProcessingEventsListenerMapper INSTANCE = Mappers.getMapper(PaymentsProcessingEventsListenerMapper.class);

    /** Add {@code @Mapping} here when MapStruct cannot infer the conversion. */
    com.arcadiaeditions.fulfillment.shipping.dtos.RecordPaymentCapturedInput asRecordPaymentCapturedInput(
            com.arcadiaeditions.payments.processing.domain.event.PaymentCaptured event);
}
