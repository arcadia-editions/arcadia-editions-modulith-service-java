package com.arcadiaeditions.orders.checkout.core.domain;

import java.util.Arrays;

public final class OrderTransitions {

    private OrderTransitions() {}

    public static void ensureCanConfirmOrder(Order entity) {
        require("confirmOrder", entity, OrderStatus.CREATED);
    }

    public static void ensureCanCancelOrder(Order entity) {
        require("cancelOrder", entity, OrderStatus.CREATED, OrderStatus.CONFIRMED);
    }

    private static void require(String command, Order entity, OrderStatus... allowed) {
        var current = entity.getStatus();
        for (var state : allowed) {
            if (current != null && current == state) {
                return;
            }
        }
        throw new InvalidStateTransitionException(
                entity.getClass().getSimpleName(), entity.getId(), null, command, current, Arrays.asList(allowed));
    }

    public static class InvalidStateTransitionException extends IllegalStateException {

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
