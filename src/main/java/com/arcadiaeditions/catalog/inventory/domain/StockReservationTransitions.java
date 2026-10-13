package com.arcadiaeditions.catalog.inventory.domain;

import java.util.Arrays;

public final class StockReservationTransitions {

    private StockReservationTransitions() {}

    public static void ensureCanReleaseStock(StockReservation entity) {
        require("releaseStock", entity, ReservationStatus.RESERVED, ReservationStatus.CONFIRMED);
    }

    public static void ensureCanConfirmStockReservation(StockReservation entity) {
        require("confirmStockReservation", entity, ReservationStatus.RESERVED);
    }

    private static void require(String command, StockReservation entity, ReservationStatus... allowed) {
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
