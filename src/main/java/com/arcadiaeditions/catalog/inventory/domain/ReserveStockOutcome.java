package com.arcadiaeditions.catalog.inventory.domain;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.Arrays;

/**
 * Enum for ReserveStockOutcome.
 */
public enum ReserveStockOutcome {
    RESERVED(1),
    UNAVAILABLE(2),
    ;
    private final Integer value;

    private ReserveStockOutcome(Integer value) {
        this.value = value;
    }

    public Integer getValue() {
        return value;
    }

    public static ReserveStockOutcome fromValue(Integer value) {
        return Arrays.stream(ReserveStockOutcome.values())
                .filter(e -> e.value.equals(value))
                .findFirst()
                .orElse(null);
    }

    @Converter
    static class ReserveStockOutcomeConverter implements AttributeConverter<ReserveStockOutcome, Integer> {

        @Override
        public Integer convertToDatabaseColumn(ReserveStockOutcome attribute) {
            if (attribute == null) {
                return null;
            }

            return attribute.value;
        }

        @Override
        public ReserveStockOutcome convertToEntityAttribute(Integer dbData) {
            return ReserveStockOutcome.fromValue(dbData);
        }
    }
}
