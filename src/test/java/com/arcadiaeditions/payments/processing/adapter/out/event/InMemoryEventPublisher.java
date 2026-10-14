package com.arcadiaeditions.payments.processing.adapter.out.event;

import com.arcadiaeditions.payments.processing.application.port.out.event.EventPublisher;
import com.arcadiaeditions.payments.processing.domain.*;
import com.arcadiaeditions.payments.processing.domain.event.*;
import java.util.ArrayList;
import java.util.List;

public class InMemoryEventPublisher implements EventPublisher {

    private final List<Object> events = new ArrayList<>();

    public List<Object> getEvents() {
        return events;
    }

    public void onPaymentFailed(PaymentFailed event) {
        events.add(event);
    }

    public void onPaymentAuthorized(PaymentAuthorized event) {
        events.add(event);
    }

    public void onPaymentRetried(PaymentRetried event) {
        events.add(event);
    }

    public void onPaymentRetryExhausted(PaymentRetryExhausted event) {
        events.add(event);
    }

    public void onPaymentDeclined(PaymentDeclined event) {
        events.add(event);
    }

    public void onPaymentCaptureFailed(PaymentCaptureFailed event) {
        events.add(event);
    }

    public void onPaymentCaptured(PaymentCaptured event) {
        events.add(event);
    }

    public void onPaymentVoided(PaymentVoided event) {
        events.add(event);
    }
}
