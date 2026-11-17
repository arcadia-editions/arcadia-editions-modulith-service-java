package com.arcadiaeditions.catalog.inventory.events.processing.mappers;

import com.arcadiaeditions.catalog.inventory.domain.events.*;
import com.arcadiaeditions.catalog.inventory.service.dtos.*;
import com.arcadiaeditions.common.mappers.BaseMapper;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper(uses = {BaseMapper.class})
public interface PaymentsProcessingEventsListenerMapper {

    PaymentsProcessingEventsListenerMapper INSTANCE = Mappers.getMapper(PaymentsProcessingEventsListenerMapper.class);

    /** The event reason is the gateway's decline code; the release reason is fixed by the event type. */
    @org.mapstruct.Mapping(target = "reason", constant = "PAYMENT_DECLINED")
    com.arcadiaeditions.catalog.inventory.service.dtos.ReleaseStockInput asReleaseStockInput(
            com.arcadiaeditions.payments.processing.domain.event.PaymentDeclined event);

    /** Release reason fixed by the event type, not the gateway reason. */
    @org.mapstruct.Mapping(target = "reason", constant = "PAYMENT_RETRY_EXHAUSTED")
    com.arcadiaeditions.catalog.inventory.service.dtos.ReleaseStockInput asReleaseStockInput(
            com.arcadiaeditions.payments.processing.domain.event.PaymentRetryExhausted event);

    /** Release reason fixed by the event type, not the void reason. */
    @org.mapstruct.Mapping(target = "reason", constant = "PAYMENT_VOIDED")
    com.arcadiaeditions.catalog.inventory.service.dtos.ReleaseStockInput asReleaseStockInput(
            com.arcadiaeditions.payments.processing.domain.event.PaymentVoided event);

    /** Release reason fixed by the event type, not the void failure reason. */
    @org.mapstruct.Mapping(target = "reason", constant = "PAYMENT_VOID_FAILED")
    com.arcadiaeditions.catalog.inventory.service.dtos.ReleaseStockInput asReleaseStockInput(
            com.arcadiaeditions.payments.processing.domain.event.PaymentVoidFailed event);
}
