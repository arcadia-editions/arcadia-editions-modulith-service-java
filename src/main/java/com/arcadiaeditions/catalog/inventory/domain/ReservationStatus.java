package com.arcadiaeditions.catalog.inventory.domain;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.Arrays;

/**
 * Enum for ReservationStatus.
 */
public enum ReservationStatus {
    RESERVED(1),
    RELEASED(2),
    CONFIRMED(3),
    ;
    private final Integer value;

    private ReservationStatus(Integer value) {
        this.value = value;
    }

    public Integer getValue() {
        return value;
    }

    public static ReservationStatus fromValue(Integer value) {
        return Arrays.stream(ReservationStatus.values())
                .filter(e -> e.value.equals(value))
                .findFirst()
                .orElse(null);
    }

    @Converter
    static class ReservationStatusConverter implements AttributeConverter<ReservationStatus, Integer> {

        @Override
        public Integer convertToDatabaseColumn(ReservationStatus attribute) {
            if (attribute == null) {
                return null;
            }

            return attribute.value;
        }

        @Override
        public ReservationStatus convertToEntityAttribute(Integer dbData) {
            return ReservationStatus.fromValue(dbData);
        }
    }
}
