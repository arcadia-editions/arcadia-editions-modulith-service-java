package com.arcadiaeditions.payments.processing.adapter.driven.provider;

import com.arcadiaeditions.payments.processing.application.port.driven.provider.PaymentProvider;
import org.springframework.stereotype.Component;

/**
 * Deterministic stand-in for the payment processor (data.sql header assumptions): the decision is forced by the
 * paymentMethodId. Triggers: pm_decline, pm_fail_once, pm_fail_always, pm_capture_fail; any other value is the
 * happy path. Provider references: STANDIN-{orderId}, STANDIN-CAPFAIL-{orderId} for pm_capture_fail.
 */
@Component
@org.jmolecules.architecture.hexagonal.SecondaryAdapter
public class StandInPaymentProvider implements PaymentProvider {

    public static final String PM_DECLINE = "pm_decline";
    public static final String PM_FAIL_ONCE = "pm_fail_once";
    public static final String PM_FAIL_ALWAYS = "pm_fail_always";
    public static final String PM_CAPTURE_FAIL = "pm_capture_fail";

    @Override
    public AuthorizationResult authorize(AuthorizationRequest request) {
        // TODO T-18: pm_decline -> declined "card_declined"
        // TODO T-23/T-24: pm_fail_once (first attempt), pm_fail_always -> failed "processor_unavailable"
        return AuthorizationResult.authorized(providerReference(request.orderId(), request.paymentMethodId()));
    }

    @Override
    public CaptureResult capture(CaptureRequest request) {
        throw new UnsupportedOperationException("capture not implemented yet (T-11, T-33)");
    }

    @Override
    public VoidResult voidAuthorization(VoidRequest request) {
        throw new UnsupportedOperationException("void not implemented yet (T-30)");
    }

    static String providerReference(String orderId, String paymentMethodId) {
        return PM_CAPTURE_FAIL.equals(paymentMethodId) ? "STANDIN-CAPFAIL-" + orderId : "STANDIN-" + orderId;
    }
}
