package com.arcadiaeditions.orders.checkout.core.domain;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.Arrays;

/**
 * Enum for OrderStatus.
 */
public enum OrderStatus {
    CREATED(1),
    CONFIRMED(2),
    CANCELLED(3),
    ;
    private final Integer value;

    private OrderStatus(Integer value) {
        this.value = value;
    }

    public Integer getValue() {
        return value;
    }

    public static OrderStatus fromValue(Integer value) {
        return Arrays.stream(OrderStatus.values())
                .filter(e -> e.value.equals(value))
                .findFirst()
                .orElse(null);
    }

    @Converter
    static class OrderStatusConverter implements AttributeConverter<OrderStatus, Integer> {

        @Override
        public Integer convertToDatabaseColumn(OrderStatus attribute) {
            if (attribute == null) {
                return null;
            }

            return attribute.value;
        }

        @Override
        public OrderStatus convertToEntityAttribute(Integer dbData) {
            return OrderStatus.fromValue(dbData);
        }
    }
}
