package com.arcadiaeditions.payments.processing.adapter.in.event.processing;

import com.arcadiaeditions.config.FlowProperties;
import com.arcadiaeditions.payments.processing.adapter.in.event.processing.mapper.PaymentsProcessingServiceEventsListenerMapper;
import com.arcadiaeditions.payments.processing.application.port.in.*;
import com.arcadiaeditions.payments.processing.application.port.in.dto.*;
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

    private final FlowProperties flowProperties;

    public PaymentsProcessingServiceEventsListener(
            PaymentsProcessingService paymentsProcessingService, FlowProperties flowProperties) {
        this.paymentsProcessingService = paymentsProcessingService;
        this.flowProperties = flowProperties;
    }

    @ApplicationModuleListener
    public void authorizePayment(PaymentRetried event) {
        // R-05: wait payments.retry-delay before the next attempt (skipped when zero)
        var retryDelay = flowProperties.payments().retryDelay();
        if (!retryDelay.isZero() && !retryDelay.isNegative()) {
            try {
                Thread.sleep(retryDelay);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Interrupted while waiting to retry payment " + event.getOrderId(), e);
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
