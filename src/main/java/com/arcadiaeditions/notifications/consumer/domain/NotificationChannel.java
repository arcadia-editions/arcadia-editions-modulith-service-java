package com.arcadiaeditions.notifications.consumer.domain;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.Arrays;

/**
 * Enum for NotificationChannel.
 */
public enum NotificationChannel {
    EMAIL(1),
    SMS(2),
    PUSH(3),
    ;
    private final Integer value;

    private NotificationChannel(Integer value) {
        this.value = value;
    }

    public Integer getValue() {
        return value;
    }

    public static NotificationChannel fromValue(Integer value) {
        return Arrays.stream(NotificationChannel.values())
                .filter(e -> e.value.equals(value))
                .findFirst()
                .orElse(null);
    }

    @Converter
    static class NotificationChannelConverter implements AttributeConverter<NotificationChannel, Integer> {

        @Override
        public Integer convertToDatabaseColumn(NotificationChannel attribute) {
            if (attribute == null) {
                return null;
            }

            return attribute.value;
        }

        @Override
        public NotificationChannel convertToEntityAttribute(Integer dbData) {
            return NotificationChannel.fromValue(dbData);
        }
    }
}
