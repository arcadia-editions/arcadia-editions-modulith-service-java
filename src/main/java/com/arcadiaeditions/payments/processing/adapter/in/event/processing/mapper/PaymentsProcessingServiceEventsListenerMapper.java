package com.arcadiaeditions.payments.processing.adapter.in.event.processing.mapper;

import com.arcadiaeditions.common.mappers.BaseMapper;
import com.arcadiaeditions.payments.processing.application.port.in.dto.*;
import com.arcadiaeditions.payments.processing.domain.event.*;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper(uses = {BaseMapper.class})
public interface PaymentsProcessingServiceEventsListenerMapper {

    PaymentsProcessingServiceEventsListenerMapper INSTANCE =
            Mappers.getMapper(PaymentsProcessingServiceEventsListenerMapper.class);

    /** Add {@code @Mapping} here when MapStruct cannot infer the conversion. */
    com.arcadiaeditions.payments.processing.application.port.in.dto.AuthorizePaymentInput asAuthorizePaymentInput(
            PaymentRetried event);

    /** Add {@code @Mapping} here when MapStruct cannot infer the conversion. */
    com.arcadiaeditions.payments.processing.application.port.in.dto.RetryPaymentInput asRetryPaymentInput(
            PaymentFailed event);

    /** Add {@code @Mapping} here when MapStruct cannot infer the conversion. */
    com.arcadiaeditions.payments.processing.application.port.in.dto.VoidPaymentInput asVoidPaymentInput(
            PaymentCaptureFailed event);
}
