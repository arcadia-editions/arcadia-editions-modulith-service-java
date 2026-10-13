package com.arcadiaeditions.payments.processing.application.port.out.event;

import com.arcadiaeditions.payments.processing.domain.*;
import com.arcadiaeditions.payments.processing.domain.event.*;

@org.jmolecules.architecture.hexagonal.SecondaryPort
public interface EventPublisher {

    void onPaymentFailed(PaymentFailed event);

    void onPaymentAuthorized(PaymentAuthorized event);

    void onPaymentRetried(PaymentRetried event);

    void onPaymentRetryExhausted(PaymentRetryExhausted event);

    void onPaymentDeclined(PaymentDeclined event);

    void onPaymentCaptureFailed(PaymentCaptureFailed event);

    void onPaymentCaptured(PaymentCaptured event);

    void onPaymentVoided(PaymentVoided event);
}
