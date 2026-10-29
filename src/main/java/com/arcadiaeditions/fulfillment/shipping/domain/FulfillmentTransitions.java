package com.arcadiaeditions.fulfillment.shipping.domain;

import java.util.Arrays;

public final class FulfillmentTransitions {

    private FulfillmentTransitions() {}

    public static void ensureCanMarkReadyToShip(Fulfillment entity) {
        require("markReadyToShip", entity, FulfillmentStatus.SCHEDULED);
    }

    private static void require(String command, Fulfillment entity, FulfillmentStatus... allowed) {
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
