package com.arcadiaeditions.payments.processing.adapter.driving.event.shipping.mapper;

import com.arcadiaeditions.common.mappers.BaseMapper;
import com.arcadiaeditions.payments.processing.application.port.driving.dto.*;
import com.arcadiaeditions.payments.processing.domain.event.*;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper(uses = {BaseMapper.class})
public interface FulfillmentShippingEventsListenerMapper {

    FulfillmentShippingEventsListenerMapper INSTANCE = Mappers.getMapper(FulfillmentShippingEventsListenerMapper.class);

    /** Add {@code @Mapping} here when MapStruct cannot infer the conversion. */
    com.arcadiaeditions.payments.processing.application.port.driving.dto.CapturePaymentInput asCapturePaymentInput(
            com.arcadiaeditions.fulfillment.shipping.domain.events.FulfillmentScheduled event);

    /** Add {@code @Mapping} here when MapStruct cannot infer the conversion. */
    com.arcadiaeditions.payments.processing.application.port.driving.dto.VoidPaymentInput asVoidPaymentInput(
            com.arcadiaeditions.fulfillment.shipping.domain.events.FulfillmentFailed event);
}
