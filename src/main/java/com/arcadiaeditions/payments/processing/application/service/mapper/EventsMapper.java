package com.arcadiaeditions.payments.processing.application.service.mapper;

import com.arcadiaeditions.common.mappers.BaseMapper;
import com.arcadiaeditions.payments.processing.application.port.in.dto.*;
import com.arcadiaeditions.payments.processing.domain.*;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper(uses = {BaseMapper.class})
public interface EventsMapper {

    EventsMapper INSTANCE = Mappers.getMapper(EventsMapper.class);

    com.arcadiaeditions.payments.processing.domain.event.PaymentDeclined asPaymentDeclined(Payment payment);

    com.arcadiaeditions.payments.processing.domain.event.PaymentRetryExhausted asPaymentRetryExhausted(Payment payment);

    com.arcadiaeditions.payments.processing.domain.event.PaymentCaptured asPaymentCaptured(Payment payment);

    com.arcadiaeditions.payments.processing.domain.event.PaymentAuthorized asPaymentAuthorized(Payment payment);

    com.arcadiaeditions.payments.processing.domain.event.PaymentRetried asPaymentRetried(Payment payment);

    com.arcadiaeditions.payments.processing.domain.event.PaymentCaptureFailed asPaymentCaptureFailed(Payment payment);

    com.arcadiaeditions.payments.processing.domain.event.PaymentVoided asPaymentVoided(Payment payment);

    com.arcadiaeditions.payments.processing.domain.event.PaymentFailed asPaymentFailed(Payment payment);
}
