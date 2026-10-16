package com.arcadiaeditions.payments.processing.application.service.mapper;

import com.arcadiaeditions.common.mappers.BaseMapper;
import com.arcadiaeditions.payments.processing.application.port.in.dto.*;
import com.arcadiaeditions.payments.processing.domain.*;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.factory.Mappers;

@Mapper(uses = {BaseMapper.class})
public interface PaymentsProcessingServiceMapper {

    PaymentsProcessingServiceMapper INSTANCE = Mappers.getMapper(PaymentsProcessingServiceMapper.class);

    // input mappings
    // AuthorizePaymentInputnull-Payment authorizePayment
    // attemptCount is set by PaymentsProcessingServiceImpl.authorizePayment; providerReference, authorizedAt,
    // declinedAt,
    // failedAt, failureReason by the provider outcome; capturedAt and voidedAt by capturePayment / voidPayment.
    @Mapping(target = "providerReference", ignore = true)
    @Mapping(target = "attemptCount", ignore = true)
    @Mapping(target = "authorizedAt", ignore = true)
    @Mapping(target = "declinedAt", ignore = true)
    @Mapping(target = "failedAt", ignore = true)
    @Mapping(target = "capturedAt", ignore = true)
    @Mapping(target = "voidedAt", ignore = true)
    @Mapping(target = "failureReason", ignore = true)
    Payment asPayment(AuthorizePaymentInput input);

    @Mapping(target = "id", ignore = true)
    // attemptCount is set by PaymentsProcessingServiceImpl.authorizePayment; providerReference, authorizedAt,
    // declinedAt,
    // failedAt, failureReason by the provider outcome; capturedAt and voidedAt by capturePayment / voidPayment.
    @Mapping(target = "providerReference", ignore = true)
    @Mapping(target = "attemptCount", ignore = true)
    @Mapping(target = "authorizedAt", ignore = true)
    @Mapping(target = "declinedAt", ignore = true)
    @Mapping(target = "failedAt", ignore = true)
    @Mapping(target = "capturedAt", ignore = true)
    @Mapping(target = "voidedAt", ignore = true)
    @Mapping(target = "failureReason", ignore = true)
    Payment update(@MappingTarget Payment entity, AuthorizePaymentInput input);
    // VoidPaymentInputnull-Payment voidPayment
    Payment asPayment(VoidPaymentInput input);

    @Mapping(target = "id", ignore = true)
    Payment update(@MappingTarget Payment entity, VoidPaymentInput input);
    // RetryPaymentInputnull-Payment retryPayment
    Payment asPayment(RetryPaymentInput input);

    @Mapping(target = "id", ignore = true)
    Payment update(@MappingTarget Payment entity, RetryPaymentInput input);
    // CapturePaymentInputnull-Payment capturePayment
    Payment asPayment(CapturePaymentInput input);

    @Mapping(target = "id", ignore = true)
    Payment update(@MappingTarget Payment entity, CapturePaymentInput input);
    // output mappings
}
