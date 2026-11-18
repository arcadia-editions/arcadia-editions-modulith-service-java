package com.arcadiaeditions.catalog.products.domain;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.Arrays;

/**
 * Enum for ProductStatus.
 */
public enum ProductStatus {
    DRAFT(1),
    PUBLISHED(2),
    RETIRED(3),
    ;
    private final Integer value;

    private ProductStatus(Integer value) {
        this.value = value;
    }

    public Integer getValue() {
        return value;
    }

    public static ProductStatus fromValue(Integer value) {
        return Arrays.stream(ProductStatus.values())
                .filter(e -> e.value.equals(value))
                .findFirst()
                .orElse(null);
    }

    @Converter
    static class ProductStatusConverter implements AttributeConverter<ProductStatus, Integer> {

        @Override
        public Integer convertToDatabaseColumn(ProductStatus attribute) {
            if (attribute == null) {
                return null;
            }

            return attribute.value;
        }

        @Override
        public ProductStatus convertToEntityAttribute(Integer dbData) {
            return ProductStatus.fromValue(dbData);
        }
    }
}
