package com.arcadiaeditions.payments.processing.application.service.mapper;

import com.arcadiaeditions.common.mappers.BaseMapper;
import com.arcadiaeditions.payments.processing.application.port.in.dto.*;
import com.arcadiaeditions.payments.processing.domain.*;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper(uses = {BaseMapper.class})
public interface EventsMapper {

    EventsMapper INSTANCE = Mappers.getMapper(EventsMapper.class);

    @Mapping(target = "reason", source = "failureReason")
    com.arcadiaeditions.payments.processing.domain.event.PaymentDeclined asPaymentDeclined(Payment payment);

    @Mapping(target = "reason", source = "payment.failureReason")
    @Mapping(target = "exhaustedAt", source = "exhaustedAt")
    com.arcadiaeditions.payments.processing.domain.event.PaymentRetryExhausted asPaymentRetryExhausted(
            Payment payment, java.time.Instant exhaustedAt);

    com.arcadiaeditions.payments.processing.domain.event.PaymentCaptured asPaymentCaptured(Payment payment);

    com.arcadiaeditions.payments.processing.domain.event.PaymentAuthorized asPaymentAuthorized(Payment payment);

    @Mapping(target = "retriedAt", source = "retriedAt")
    com.arcadiaeditions.payments.processing.domain.event.PaymentRetried asPaymentRetried(
            Payment payment, java.time.Instant retriedAt);

    @Mapping(target = "reason", source = "failureReason")
    com.arcadiaeditions.payments.processing.domain.event.PaymentCaptureFailed asPaymentCaptureFailed(Payment payment);

    @Mapping(target = "reason", source = "failureReason")
    com.arcadiaeditions.payments.processing.domain.event.PaymentVoided asPaymentVoided(Payment payment);

    @Mapping(target = "reason", source = "failureReason")
    com.arcadiaeditions.payments.processing.domain.event.PaymentFailed asPaymentFailed(Payment payment);
}
