package com.arcadiaeditions.payments.processing.adapter.out.provider;

import com.arcadiaeditions.payments.processing.application.port.out.provider.PaymentProvider;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

/**
 * Deterministic stand-in for the external payment provider (research R-06, contracts/stand-in-adapters.md).
 *
 * <p>Outcomes are forced by {@code paymentMethodId}: {@code pm_decline}, {@code pm_fail_once},
 * {@code pm_fail_always}, {@code pm_capture_fail}; anything else takes the happy path.
 *
 * <p>The capture trigger is encoded in the provider reference so it survives restarts: the reference is
 * {@code STANDIN-<orderId>}, or {@code STANDIN-CAPFAIL-<orderId>} for {@code pm_capture_fail}; {@link #capture}
 * fails when the reference starts with {@code STANDIN-CAPFAIL-}.
 */
@Component
@org.jmolecules.architecture.hexagonal.SecondaryAdapter
public class StandInPaymentProvider implements PaymentProvider {

    static final String REFERENCE_PREFIX = "STANDIN-";
    static final String CAPTURE_FAIL_PREFIX = "STANDIN-CAPFAIL-";

    @Override
    public AuthorizeResult authorize(
            String orderId, int attempt, BigDecimal amount, String currency, String paymentMethodId) {
        if (paymentMethodId == null) {
            return new Authorized(REFERENCE_PREFIX + orderId);
        }
        return switch (paymentMethodId) {
            case "pm_decline" -> new Declined("card_declined");
            case "pm_fail_once" ->
                attempt <= 1 ? new Failed("processor_unavailable") : new Authorized(REFERENCE_PREFIX + orderId);
            case "pm_fail_always" -> new Failed("processor_unavailable");
            case "pm_capture_fail" -> new Authorized(CAPTURE_FAIL_PREFIX + orderId);
            default -> new Authorized(REFERENCE_PREFIX + orderId);
        };
    }

    @Override
    public CaptureResult capture(String providerReference, BigDecimal amount, String currency) {
        if (providerReference != null && providerReference.startsWith(CAPTURE_FAIL_PREFIX)) {
            return new CaptureFailed("capture_rejected");
        }
        return new Captured();
    }

    @Override
    public VoidResult voidAuthorization(String providerReference) {
        return new Voided();
    }
}
