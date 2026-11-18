package com.arcadiaeditions.catalog.inventory.domain;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.Arrays;

/**
 * Enum for ConfirmStockReservationOutcome.
 */
public enum ConfirmStockReservationOutcome {
    CONFIRMED(1),
    NOT_HELD(2),
    ;
    private final Integer value;

    private ConfirmStockReservationOutcome(Integer value) {
        this.value = value;
    }

    public Integer getValue() {
        return value;
    }

    public static ConfirmStockReservationOutcome fromValue(Integer value) {
        return Arrays.stream(ConfirmStockReservationOutcome.values())
                .filter(e -> e.value.equals(value))
                .findFirst()
                .orElse(null);
    }

    @Converter
    static class ConfirmStockReservationOutcomeConverter
            implements AttributeConverter<ConfirmStockReservationOutcome, Integer> {

        @Override
        public Integer convertToDatabaseColumn(ConfirmStockReservationOutcome attribute) {
            if (attribute == null) {
                return null;
            }

            return attribute.value;
        }

        @Override
        public ConfirmStockReservationOutcome convertToEntityAttribute(Integer dbData) {
            return ConfirmStockReservationOutcome.fromValue(dbData);
        }
    }
}
