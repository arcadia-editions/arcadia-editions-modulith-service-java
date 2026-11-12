package com.arcadiaeditions.payments.processing.application.port.driven.provider;

import java.math.BigDecimal;

/**
 * Payment processor port: two-phase payment (authorization hold, later capture or void of that hold).
 */
@org.jmolecules.architecture.hexagonal.SecondaryPort
public interface PaymentProvider {

    AuthorizationResult authorize(AuthorizationRequest request);

    CaptureResult capture(CaptureRequest request);

    VoidResult voidAuthorization(VoidRequest request);

    record AuthorizationRequest(
            String orderId, String paymentMethodId, BigDecimal amount, String currency, int attempt) {}

    enum AuthorizationOutcome {
        AUTHORIZED,
        DECLINED,
        FAILED
    }

    /** {@code providerReference} when AUTHORIZED, {@code reason} when DECLINED or FAILED. */
    record AuthorizationResult(AuthorizationOutcome outcome, String providerReference, String reason) {

        public static AuthorizationResult authorized(String providerReference) {
            return new AuthorizationResult(AuthorizationOutcome.AUTHORIZED, providerReference, null);
        }

        public static AuthorizationResult declined(String reason) {
            return new AuthorizationResult(AuthorizationOutcome.DECLINED, null, reason);
        }

        public static AuthorizationResult failed(String reason) {
            return new AuthorizationResult(AuthorizationOutcome.FAILED, null, reason);
        }
    }

    record CaptureRequest(
            String orderId, String paymentMethodId, String providerReference, BigDecimal amount, String currency) {}

    /** {@code captured} false carries the {@code reason}. */
    record CaptureResult(boolean captured, String reason) {}

    record VoidRequest(String orderId, String providerReference, String reason) {}

    record VoidResult(boolean voided, String reason) {}
}
