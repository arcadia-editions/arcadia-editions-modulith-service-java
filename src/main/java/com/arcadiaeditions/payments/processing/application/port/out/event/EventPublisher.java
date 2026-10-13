package com.arcadiaeditions.payments.processing.application.port.out.event;

import com.arcadiaeditions.payments.processing.domain.*;
import com.arcadiaeditions.payments.processing.domain.event.*;

@org.jmolecules.architecture.hexagonal.SecondaryPort
public interface EventPublisher {

    void onPaymentDeclined(PaymentDeclined event);

    void onPaymentRetryExhausted(PaymentRetryExhausted event);

    void onPaymentCaptureFailed(PaymentCaptureFailed event);

    void onPaymentRetried(PaymentRetried event);

    void onPaymentVoided(PaymentVoided event);

    void onPaymentFailed(PaymentFailed event);

    void onPaymentCaptured(PaymentCaptured event);

    void onPaymentAuthorized(PaymentAuthorized event);
}
