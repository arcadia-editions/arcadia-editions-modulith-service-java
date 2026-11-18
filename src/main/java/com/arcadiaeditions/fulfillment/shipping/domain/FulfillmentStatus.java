package com.arcadiaeditions.fulfillment.shipping.domain;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.Arrays;

/**
 * Enum for FulfillmentStatus.
 */
public enum FulfillmentStatus {
    SCHEDULED(1),
    READY_TO_SHIP(2),
    FAILED(3),
    CANCELLED(4),
    ;
    private final Integer value;

    private FulfillmentStatus(Integer value) {
        this.value = value;
    }

    public Integer getValue() {
        return value;
    }

    public static FulfillmentStatus fromValue(Integer value) {
        return Arrays.stream(FulfillmentStatus.values())
                .filter(e -> e.value.equals(value))
                .findFirst()
                .orElse(null);
    }

    @Converter
    static class FulfillmentStatusConverter implements AttributeConverter<FulfillmentStatus, Integer> {

        @Override
        public Integer convertToDatabaseColumn(FulfillmentStatus attribute) {
            if (attribute == null) {
                return null;
            }

            return attribute.value;
        }

        @Override
        public FulfillmentStatus convertToEntityAttribute(Integer dbData) {
            return FulfillmentStatus.fromValue(dbData);
        }
    }
}
