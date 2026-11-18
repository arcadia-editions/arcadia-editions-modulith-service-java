package com.arcadiaeditions.catalog.products.domain;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.Arrays;

/**
 * Enum for InventoryTrackingMode.
 */
public enum InventoryTrackingMode {
    QUANTITY(1),
    SERIALIZED(2),
    ;
    private final Integer value;

    private InventoryTrackingMode(Integer value) {
        this.value = value;
    }

    public Integer getValue() {
        return value;
    }

    public static InventoryTrackingMode fromValue(Integer value) {
        return Arrays.stream(InventoryTrackingMode.values())
                .filter(e -> e.value.equals(value))
                .findFirst()
                .orElse(null);
    }

    @Converter
    static class InventoryTrackingModeConverter implements AttributeConverter<InventoryTrackingMode, Integer> {

        @Override
        public Integer convertToDatabaseColumn(InventoryTrackingMode attribute) {
            if (attribute == null) {
                return null;
            }

            return attribute.value;
        }

        @Override
        public InventoryTrackingMode convertToEntityAttribute(Integer dbData) {
            return InventoryTrackingMode.fromValue(dbData);
        }
    }
}
