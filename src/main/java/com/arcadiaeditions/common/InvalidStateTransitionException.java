package com.arcadiaeditions.common;

/**
 * Base type for an illegal aggregate state transition. Each module's
 * {@code *Transitions.InvalidStateTransitionException} extends this so
 * {@code config.WebExceptionsTranslator} can map them to HTTP 409 without
 * depending on types inside a closed module.
 */
public class InvalidStateTransitionException extends IllegalStateException {

    @java.io.Serial
    private static final long serialVersionUID = 1L;

    public InvalidStateTransitionException(String message) {
        super(message);
    }
}
