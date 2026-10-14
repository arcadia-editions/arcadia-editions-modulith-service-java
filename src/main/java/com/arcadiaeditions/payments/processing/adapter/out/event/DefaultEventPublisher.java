package com.arcadiaeditions.payments.processing.adapter.out.event;

import com.arcadiaeditions.payments.processing.application.port.out.event.EventPublisher;
import com.arcadiaeditions.payments.processing.domain.*;
import com.arcadiaeditions.payments.processing.domain.event.*;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
@org.jmolecules.architecture.hexagonal.SecondaryAdapter
public class DefaultEventPublisher implements EventPublisher {

    private final ApplicationEventPublisher applicationEventPublisher;

    public DefaultEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
        this.applicationEventPublisher = applicationEventPublisher;
    }

    public void onPaymentFailed(PaymentFailed event) {
        applicationEventPublisher.publishEvent(event);
    }

    public void onPaymentAuthorized(PaymentAuthorized event) {
        applicationEventPublisher.publishEvent(event);
    }

    public void onPaymentRetried(PaymentRetried event) {
        applicationEventPublisher.publishEvent(event);
    }

    public void onPaymentRetryExhausted(PaymentRetryExhausted event) {
        applicationEventPublisher.publishEvent(event);
    }

    public void onPaymentDeclined(PaymentDeclined event) {
        applicationEventPublisher.publishEvent(event);
    }

    public void onPaymentCaptureFailed(PaymentCaptureFailed event) {
        applicationEventPublisher.publishEvent(event);
    }

    public void onPaymentCaptured(PaymentCaptured event) {
        applicationEventPublisher.publishEvent(event);
    }

    public void onPaymentVoided(PaymentVoided event) {
        applicationEventPublisher.publishEvent(event);
    }
}
