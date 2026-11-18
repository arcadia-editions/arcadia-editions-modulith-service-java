package com.arcadiaeditions.catalog.inventory.domain;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.Arrays;

/**
 * Enum for StockAdjustmentReason.
 */
public enum StockAdjustmentReason {
    DAMAGED(1),
    LOST(2),
    FOUND(3),
    MANUAL_CORRECTION(4),
    RETURNED(5),
    ;
    private final Integer value;

    private StockAdjustmentReason(Integer value) {
        this.value = value;
    }

    public Integer getValue() {
        return value;
    }

    public static StockAdjustmentReason fromValue(Integer value) {
        return Arrays.stream(StockAdjustmentReason.values())
                .filter(e -> e.value.equals(value))
                .findFirst()
                .orElse(null);
    }

    @Converter
    static class StockAdjustmentReasonConverter implements AttributeConverter<StockAdjustmentReason, Integer> {

        @Override
        public Integer convertToDatabaseColumn(StockAdjustmentReason attribute) {
            if (attribute == null) {
                return null;
            }

            return attribute.value;
        }

        @Override
        public StockAdjustmentReason convertToEntityAttribute(Integer dbData) {
            return StockAdjustmentReason.fromValue(dbData);
        }
    }
}
