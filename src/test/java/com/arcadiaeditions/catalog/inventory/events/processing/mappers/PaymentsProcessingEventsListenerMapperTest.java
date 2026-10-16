package com.arcadiaeditions.catalog.inventory.events.processing.mappers;

import static org.assertj.core.api.Assertions.assertThat;

import com.arcadiaeditions.catalog.inventory.domain.StockReleaseReason;
import com.arcadiaeditions.payments.processing.domain.event.PaymentDeclined;
import com.arcadiaeditions.payments.processing.domain.event.PaymentRetryExhausted;
import com.arcadiaeditions.payments.processing.domain.event.PaymentVoided;
import org.junit.jupiter.api.Test;

class PaymentsProcessingEventsListenerMapperTest {

    private final PaymentsProcessingEventsListenerMapper mapper = PaymentsProcessingEventsListenerMapper.INSTANCE;

    @Test
    void paymentDeclinedMapsToConstantReasonWhateverTheProviderText() {
        var input = mapper.asReleaseStockInput(
                new PaymentDeclined().setOrderId("ORD-1").setReason("card_declined"));

        assertThat(input.getOrderId()).isEqualTo("ORD-1");
        assertThat(input.getReason()).isEqualTo(StockReleaseReason.PAYMENT_DECLINED);
    }

    @Test
    void paymentRetryExhaustedMapsToConstantReason() {
        var input = mapper.asReleaseStockInput(
                new PaymentRetryExhausted().setOrderId("ORD-2").setReason("provider timeout"));

        assertThat(input.getOrderId()).isEqualTo("ORD-2");
        assertThat(input.getReason()).isEqualTo(StockReleaseReason.PAYMENT_RETRY_EXHAUSTED);
    }

    @Test
    void paymentVoidedMapsToConstantReason() {
        var input = mapper.asReleaseStockInput(
                new PaymentVoided().setOrderId("ORD-3").setReason("fulfillment failed"));

        assertThat(input.getOrderId()).isEqualTo("ORD-3");
        assertThat(input.getReason()).isEqualTo(StockReleaseReason.PAYMENT_VOIDED);
    }
}
