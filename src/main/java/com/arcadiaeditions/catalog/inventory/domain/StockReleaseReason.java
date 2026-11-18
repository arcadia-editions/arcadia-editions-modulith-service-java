package com.arcadiaeditions.catalog.inventory.domain;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.Arrays;

/**
 * Enum for StockReleaseReason.
 */
public enum StockReleaseReason {
    PAYMENT_DECLINED(1),
    PAYMENT_RETRY_EXHAUSTED(2),
    PAYMENT_VOIDED(3),
    PAYMENT_VOID_FAILED(4),
    RESERVATION_EXPIRED(5),
    ;
    private final Integer value;

    private StockReleaseReason(Integer value) {
        this.value = value;
    }

    public Integer getValue() {
        return value;
    }

    public static StockReleaseReason fromValue(Integer value) {
        return Arrays.stream(StockReleaseReason.values())
                .filter(e -> e.value.equals(value))
                .findFirst()
                .orElse(null);
    }

    @Converter
    static class StockReleaseReasonConverter implements AttributeConverter<StockReleaseReason, Integer> {

        @Override
        public Integer convertToDatabaseColumn(StockReleaseReason attribute) {
            if (attribute == null) {
                return null;
            }

            return attribute.value;
        }

        @Override
        public StockReleaseReason convertToEntityAttribute(Integer dbData) {
            return StockReleaseReason.fromValue(dbData);
        }
    }
}
