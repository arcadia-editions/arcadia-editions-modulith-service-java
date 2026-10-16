package com.arcadiaeditions.payments.processing.application.port.out.provider;

import java.math.BigDecimal;

/**
 * Outbound port to the external payment provider (gap report X-02).
 *
 * <p>{@code orderId} + {@code attempt} is the idempotency key of {@link #authorize}: the same key returns the same
 * result.
 */
@org.jmolecules.architecture.hexagonal.SecondaryPort
public interface PaymentProvider {

    AuthorizeResult authorize(String orderId, int attempt, BigDecimal amount, String currency, String paymentMethodId);

    CaptureResult capture(String providerReference, BigDecimal amount, String currency);

    VoidResult voidAuthorization(String providerReference);

    sealed interface AuthorizeResult {}

    record Authorized(String providerReference) implements AuthorizeResult {}

    record Declined(String reason) implements AuthorizeResult {}

    record Failed(String reason) implements AuthorizeResult {}

    sealed interface CaptureResult {}

    record Captured() implements CaptureResult {}

    record CaptureFailed(String reason) implements CaptureResult {}

    sealed interface VoidResult {}

    record Voided() implements VoidResult {}
}
