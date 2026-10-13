package com.arcadiaeditions.payments.processing.adapter.in.event.processing;

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

    public PaymentsProcessingServiceEventsListener(PaymentsProcessingService paymentsProcessingService) {
        this.paymentsProcessingService = paymentsProcessingService;
    }

    @ApplicationModuleListener
    public void retryPayment(PaymentFailed event) {
        // TODO CUSTOM_REQUIRED: resolve the Payment id for PaymentFailed (no event field is assumed to carry it)
        Long id = null;
        paymentsProcessingService.retryPayment(id, mapper.asRetryPaymentInput(event));
    }

    @ApplicationModuleListener
    public void voidPayment(PaymentCaptureFailed event) {
        // TODO CUSTOM_REQUIRED: resolve the Payment id for PaymentCaptureFailed (no event field is assumed to carry it)
        Long id = null;
        paymentsProcessingService.voidPayment(id, mapper.asVoidPaymentInput(event));
    }
}
