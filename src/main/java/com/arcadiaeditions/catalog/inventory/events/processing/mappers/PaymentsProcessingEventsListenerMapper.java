package com.arcadiaeditions.catalog.inventory.events.processing.mappers;

import com.arcadiaeditions.catalog.inventory.domain.events.*;
import com.arcadiaeditions.catalog.inventory.service.dtos.*;
import com.arcadiaeditions.common.mappers.BaseMapper;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper(uses = {BaseMapper.class})
public interface PaymentsProcessingEventsListenerMapper {

    PaymentsProcessingEventsListenerMapper INSTANCE = Mappers.getMapper(PaymentsProcessingEventsListenerMapper.class);

    /** Add {@code @Mapping} here when MapStruct cannot infer the conversion. */
    com.arcadiaeditions.catalog.inventory.service.dtos.ReleaseStockInput asReleaseStockInput(
            com.arcadiaeditions.payments.processing.domain.event.PaymentDeclined event);

    /** Add {@code @Mapping} here when MapStruct cannot infer the conversion. */
    com.arcadiaeditions.catalog.inventory.service.dtos.ReleaseStockInput asReleaseStockInput(
            com.arcadiaeditions.payments.processing.domain.event.PaymentRetryExhausted event);

    /** Add {@code @Mapping} here when MapStruct cannot infer the conversion. */
    com.arcadiaeditions.catalog.inventory.service.dtos.ReleaseStockInput asReleaseStockInput(
            com.arcadiaeditions.payments.processing.domain.event.PaymentVoided event);
}
