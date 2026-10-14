package com.arcadiaeditions.payments.processing.domain;

import java.util.Arrays;

public final class PaymentTransitions {

    private PaymentTransitions() {}

    public static void ensureCanRetryPayment(Payment entity) {
        require("retryPayment", entity, PaymentStatus.FAILED);
    }

    public static void ensureCanCapturePayment(Payment entity) {
        require("capturePayment", entity, PaymentStatus.AUTHORIZED);
    }

    public static void ensureCanVoidPayment(Payment entity) {
        require(
                "voidPayment",
                entity,
                PaymentStatus.AUTHORIZED,
                PaymentStatus.AUTHORIZATION_REQUESTED,
                PaymentStatus.FAILED,
                PaymentStatus.CAPTURE_FAILED);
    }

    private static void require(String command, Payment entity, PaymentStatus... allowed) {
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
