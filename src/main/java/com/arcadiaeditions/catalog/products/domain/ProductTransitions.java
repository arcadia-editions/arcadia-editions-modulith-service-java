package com.arcadiaeditions.catalog.products.domain;

import java.util.Arrays;

public final class ProductTransitions {

    private ProductTransitions() {}

    public static void ensureCanUpdateProduct(Product entity) {
        require("updateProduct", entity, ProductStatus.DRAFT, ProductStatus.PUBLISHED);
    }

    public static void ensureCanPublishProduct(Product entity) {
        require("publishProduct", entity, ProductStatus.DRAFT);
    }

    public static void ensureCanRetireProduct(Product entity) {
        require("retireProduct", entity, ProductStatus.DRAFT, ProductStatus.PUBLISHED);
    }

    private static void require(String command, Product entity, ProductStatus... allowed) {
        var current = entity.getStatus();
        for (var state : allowed) {
            if (current != null && current == state) {
                return;
            }
        }
        throw new InvalidStateTransitionException(
                entity.getClass().getSimpleName(), entity.getId(), null, command, current, Arrays.asList(allowed));
    }

    public static class InvalidStateTransitionException
            extends com.arcadiaeditions.common.InvalidStateTransitionException {

        public InvalidStateTransitionException(
                String entityType,
                Object entityId,
                Object naturalId,
                String operation,
                Object currentState,
                java.util.Collection<?> allowedStates) {
            super("Invalid state transition for " + entityType + " ("
                    + (naturalId != null ? naturalId : "id=" + entityId) + ")" + ": operation '" + operation
                    + "' not allowed from state " + currentState + ", allowed states: " + allowedStates);
        }
    }
}
