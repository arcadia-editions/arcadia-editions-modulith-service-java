package com.arcadiaeditions.payments.processing.adapter.driving.event.processing;

import com.arcadiaeditions.payments.processing.adapter.driving.event.processing.mapper.PaymentsProcessingServiceEventsListenerMapper;
import com.arcadiaeditions.payments.processing.application.port.driving.*;
import com.arcadiaeditions.payments.processing.application.port.driving.dto.*;
import com.arcadiaeditions.payments.processing.domain.event.*;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

/**
 * Internal domain event listeners for PaymentsProcessingService.
 *
 * This class is generated once and can be customized safely.
 */
@Component
@org.jmolecules.architecture.hexagonal.PrimaryAdapter
public class PaymentsProcessingServiceEventsListener {
    private final PaymentsProcessingService paymentsProcessingService;
    private final PaymentsProcessingServiceEventsListenerMapper mapper =
            PaymentsProcessingServiceEventsListenerMapper.INSTANCE;

    private final com.arcadiaeditions.config.FlowProperties flowProperties;

    public PaymentsProcessingServiceEventsListener(
            PaymentsProcessingService paymentsProcessingService,
            com.arcadiaeditions.config.FlowProperties flowProperties) {
        this.paymentsProcessingService = paymentsProcessingService;
        this.flowProperties = flowProperties;
    }

    @ApplicationModuleListener
    public void authorizePayment(PaymentRetried event) {
        // retry-delay (arcadia.payments.retry-delay) is applied here, not in the service: service tests never wait
        var delay = flowProperties.getPayments().getRetryDelay();
        if (delay != null && !delay.isZero() && !delay.isNegative()) {
            try {
                Thread.sleep(delay);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("interrupted while waiting for the payment retry delay", e);
            }
        }
        paymentsProcessingService.authorizePayment(mapper.asAuthorizePaymentInput(event));
    }

    @ApplicationModuleListener
    public void retryPayment(PaymentFailed event) {
        paymentsProcessingService.retryPayment(mapper.asRetryPaymentInput(event));
    }

    @ApplicationModuleListener
    public void voidPayment(PaymentCaptureFailed event) {
        paymentsProcessingService.voidPayment(mapper.asVoidPaymentInput(event));
    }
}
