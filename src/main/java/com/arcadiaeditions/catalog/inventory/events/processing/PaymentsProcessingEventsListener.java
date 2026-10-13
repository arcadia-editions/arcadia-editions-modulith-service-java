package com.arcadiaeditions.catalog.inventory.events.processing;

import com.arcadiaeditions.catalog.inventory.domain.events.*;
import com.arcadiaeditions.catalog.inventory.events.processing.mappers.PaymentsProcessingEventsListenerMapper;
import com.arcadiaeditions.catalog.inventory.service.*;
import com.arcadiaeditions.catalog.inventory.service.dtos.*;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

/**
 * Internal domain event listeners for PaymentsProcessingZdl.
 *
 * This class is generated once and can be customized safely.
 */
@Component
@org.jmolecules.architecture.layered.InterfaceLayer
public class PaymentsProcessingEventsListener {
    private final InventoryService inventoryService;
    private final PaymentsProcessingEventsListenerMapper mapper = PaymentsProcessingEventsListenerMapper.INSTANCE;

    public PaymentsProcessingEventsListener(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @ApplicationModuleListener
    public void onPaymentDeclined(com.arcadiaeditions.payments.processing.domain.event.PaymentDeclined event) {
        inventoryService.releaseStock(mapper.asReleaseStockInput(event));
    }

    @ApplicationModuleListener
    public void onPaymentRetryExhausted(
            com.arcadiaeditions.payments.processing.domain.event.PaymentRetryExhausted event) {
        inventoryService.releaseStock(mapper.asReleaseStockInput(event));
    }

    @ApplicationModuleListener
    public void onPaymentVoided(com.arcadiaeditions.payments.processing.domain.event.PaymentVoided event) {
        inventoryService.releaseStock(mapper.asReleaseStockInput(event));
    }
}
