package com.arcadiaeditions.payments.processing.domain;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.Arrays;

/**
 * Enum for GetPaymentStatusOutcome.
 */
public enum GetPaymentStatusOutcome {
    NO_PAYMENT(1),
    STARTED(2),
    ;
    private final Integer value;

    private GetPaymentStatusOutcome(Integer value) {
        this.value = value;
    }

    public Integer getValue() {
        return value;
    }

    public static GetPaymentStatusOutcome fromValue(Integer value) {
        return Arrays.stream(GetPaymentStatusOutcome.values())
                .filter(e -> e.value.equals(value))
                .findFirst()
                .orElse(null);
    }

    @Converter
    static class GetPaymentStatusOutcomeConverter implements AttributeConverter<GetPaymentStatusOutcome, Integer> {

        @Override
        public Integer convertToDatabaseColumn(GetPaymentStatusOutcome attribute) {
            if (attribute == null) {
                return null;
            }

            return attribute.value;
        }

        @Override
        public GetPaymentStatusOutcome convertToEntityAttribute(Integer dbData) {
            return GetPaymentStatusOutcome.fromValue(dbData);
        }
    }
}
