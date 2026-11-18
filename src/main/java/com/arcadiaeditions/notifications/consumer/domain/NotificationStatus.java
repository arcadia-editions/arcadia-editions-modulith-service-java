package com.arcadiaeditions.notifications.consumer.domain;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.Arrays;

/**
 * Enum for NotificationStatus.
 */
public enum NotificationStatus {
    PENDING(1),
    SENT(2),
    FAILED(3),
    ;
    private final Integer value;

    private NotificationStatus(Integer value) {
        this.value = value;
    }

    public Integer getValue() {
        return value;
    }

    public static NotificationStatus fromValue(Integer value) {
        return Arrays.stream(NotificationStatus.values())
                .filter(e -> e.value.equals(value))
                .findFirst()
                .orElse(null);
    }

    @Converter
    static class NotificationStatusConverter implements AttributeConverter<NotificationStatus, Integer> {

        @Override
        public Integer convertToDatabaseColumn(NotificationStatus attribute) {
            if (attribute == null) {
                return null;
            }

            return attribute.value;
        }

        @Override
        public NotificationStatus convertToEntityAttribute(Integer dbData) {
            return NotificationStatus.fromValue(dbData);
        }
    }
}
