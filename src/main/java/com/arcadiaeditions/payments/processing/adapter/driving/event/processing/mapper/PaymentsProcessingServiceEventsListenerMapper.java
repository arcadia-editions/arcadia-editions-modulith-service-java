package com.arcadiaeditions.payments.processing.adapter.driving.event.processing.mapper;

import com.arcadiaeditions.common.mappers.BaseMapper;
import com.arcadiaeditions.payments.processing.application.port.driving.dto.*;
import com.arcadiaeditions.payments.processing.domain.event.*;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper(uses = {BaseMapper.class})
public interface PaymentsProcessingServiceEventsListenerMapper {

    PaymentsProcessingServiceEventsListenerMapper INSTANCE =
            Mappers.getMapper(PaymentsProcessingServiceEventsListenerMapper.class);

    /** Add {@code @Mapping} here when MapStruct cannot infer the conversion. */
    com.arcadiaeditions.payments.processing.application.port.driving.dto.ReauthorizePaymentInput asReauthorizePaymentInput(
            PaymentRetried event);

    /** Add {@code @Mapping} here when MapStruct cannot infer the conversion. */
    com.arcadiaeditions.payments.processing.application.port.driving.dto.RetryPaymentInput asRetryPaymentInput(
            PaymentFailed event);

    /** Add {@code @Mapping} here when MapStruct cannot infer the conversion. */
    com.arcadiaeditions.payments.processing.application.port.driving.dto.VoidPaymentInput asVoidPaymentInput(
            PaymentCaptureFailed event);
}
