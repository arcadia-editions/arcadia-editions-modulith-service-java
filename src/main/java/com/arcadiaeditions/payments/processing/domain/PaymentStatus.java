package com.arcadiaeditions.payments.processing.domain;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.Arrays;

/**
 * Enum for PaymentStatus.
 */
public enum PaymentStatus {
    AUTHORIZATION_REQUESTED(1),
    AUTHORIZED(2),
    DECLINED(3),
    FAILED(4),
    RETRY_EXHAUSTED(5),
    CAPTURED(6),
    CAPTURE_FAILED(7),
    VOIDED(8),
    VOID_FAILED(9),
    ;
    private final Integer value;

    private PaymentStatus(Integer value) {
        this.value = value;
    }

    public Integer getValue() {
        return value;
    }

    public static PaymentStatus fromValue(Integer value) {
        return Arrays.stream(PaymentStatus.values())
                .filter(e -> e.value.equals(value))
                .findFirst()
                .orElse(null);
    }

    @Converter
    static class PaymentStatusConverter implements AttributeConverter<PaymentStatus, Integer> {

        @Override
        public Integer convertToDatabaseColumn(PaymentStatus attribute) {
            if (attribute == null) {
                return null;
            }

            return attribute.value;
        }

        @Override
        public PaymentStatus convertToEntityAttribute(Integer dbData) {
            return PaymentStatus.fromValue(dbData);
        }
    }
}
