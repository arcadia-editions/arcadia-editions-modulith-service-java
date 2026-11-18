package com.arcadiaeditions.notifications.consumer.domain;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.Arrays;

/**
 * Enum for NotificationType.
 */
public enum NotificationType {
    ORDER_CONFIRMATION(1),
    STOCK_UNAVAILABLE(2),
    ORDER_CANCELLED(3),
    ;
    private final Integer value;

    private NotificationType(Integer value) {
        this.value = value;
    }

    public Integer getValue() {
        return value;
    }

    public static NotificationType fromValue(Integer value) {
        return Arrays.stream(NotificationType.values())
                .filter(e -> e.value.equals(value))
                .findFirst()
                .orElse(null);
    }

    @Converter
    static class NotificationTypeConverter implements AttributeConverter<NotificationType, Integer> {

        @Override
        public Integer convertToDatabaseColumn(NotificationType attribute) {
            if (attribute == null) {
                return null;
            }

            return attribute.value;
        }

        @Override
        public NotificationType convertToEntityAttribute(Integer dbData) {
            return NotificationType.fromValue(dbData);
        }
    }
}
