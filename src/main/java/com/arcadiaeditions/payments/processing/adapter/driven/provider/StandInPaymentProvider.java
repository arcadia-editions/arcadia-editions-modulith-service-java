package com.arcadiaeditions.payments.processing.adapter.driven.provider;

import com.arcadiaeditions.payments.processing.application.port.driven.provider.PaymentProvider;
import org.springframework.stereotype.Component;

/**
 * Deterministic stand-in for the payment processor (data.sql header assumptions): the decision is forced by the
 * paymentMethodId. Triggers: pm_decline, pm_fail_once, pm_fail_always, pm_capture_fail, pm_void_fail (void refused); any other value is the
 * happy path. Provider references: STANDIN-{orderId}, STANDIN-CAPFAIL-{orderId} for pm_capture_fail.
 */
@Component
@org.jmolecules.architecture.hexagonal.SecondaryAdapter
public class StandInPaymentProvider implements PaymentProvider {

    public static final String PM_DECLINE = "pm_decline";
    public static final String PM_FAIL_ONCE = "pm_fail_once";
    public static final String PM_FAIL_ALWAYS = "pm_fail_always";
    public static final String PM_CAPTURE_FAIL = "pm_capture_fail";
    public static final String PM_VOID_FAIL = "pm_void_fail";

    @Override
    public AuthorizationResult authorize(AuthorizationRequest request) {
        if (PM_DECLINE.equals(request.paymentMethodId())) {
            return AuthorizationResult.declined(CARD_DECLINED);
        }
        if (PM_FAIL_ALWAYS.equals(request.paymentMethodId())
                || (PM_FAIL_ONCE.equals(request.paymentMethodId()) && request.attempt() <= 1)) {
            return AuthorizationResult.failed(PROCESSOR_UNAVAILABLE);
        }
        return AuthorizationResult.authorized(providerReference(request.orderId(), request.paymentMethodId()));
    }

    @Override
    public CaptureResult capture(CaptureRequest request) {
        // pm_capture_fail (providerReference STANDIN-CAPFAIL-<orderId>) -> not captured "capture_rejected"
        if (request.providerReference() != null && request.providerReference().startsWith("STANDIN-CAPFAIL-")) {
            return new CaptureResult(false, CAPTURE_REJECTED);
        }
        return new CaptureResult(true, null);
    }

    @Override
    public VoidResult voidAuthorization(VoidRequest request) {
        if (PM_VOID_FAIL.equals(request.paymentMethodId())) {
            return new VoidResult(false, VOID_REJECTED);
        }
        return new VoidResult(true, null);
    }

    static String providerReference(String orderId, String paymentMethodId) {
        return PM_CAPTURE_FAIL.equals(paymentMethodId) ? "STANDIN-CAPFAIL-" + orderId : "STANDIN-" + orderId;
    }
}
