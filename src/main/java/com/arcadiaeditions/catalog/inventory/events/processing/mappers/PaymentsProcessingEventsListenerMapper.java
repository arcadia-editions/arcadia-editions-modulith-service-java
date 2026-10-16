package com.arcadiaeditions.catalog.inventory.events.processing.mappers;

import com.arcadiaeditions.catalog.inventory.domain.events.*;
import com.arcadiaeditions.catalog.inventory.service.dtos.*;
import com.arcadiaeditions.common.mappers.BaseMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper(uses = {BaseMapper.class})
public interface PaymentsProcessingEventsListenerMapper {

    PaymentsProcessingEventsListenerMapper INSTANCE = Mappers.getMapper(PaymentsProcessingEventsListenerMapper.class);

    /** The release reason is fixed by the source event type, never the provider's free text. */
    @Mapping(target = "reason", constant = "PAYMENT_DECLINED")
    com.arcadiaeditions.catalog.inventory.service.dtos.ReleaseStockInput asReleaseStockInput(
            com.arcadiaeditions.payments.processing.domain.event.PaymentDeclined event);

    /** The release reason is fixed by the source event type, never the provider's free text. */
    @Mapping(target = "reason", constant = "PAYMENT_RETRY_EXHAUSTED")
    com.arcadiaeditions.catalog.inventory.service.dtos.ReleaseStockInput asReleaseStockInput(
            com.arcadiaeditions.payments.processing.domain.event.PaymentRetryExhausted event);

    /** The release reason is fixed by the source event type, never the provider's free text. */
    @Mapping(target = "reason", constant = "PAYMENT_VOIDED")
    com.arcadiaeditions.catalog.inventory.service.dtos.ReleaseStockInput asReleaseStockInput(
            com.arcadiaeditions.payments.processing.domain.event.PaymentVoided event);
}
