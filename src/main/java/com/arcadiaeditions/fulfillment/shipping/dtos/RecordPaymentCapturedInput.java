package com.arcadiaeditions.fulfillment.shipping.dtos;

import com.arcadiaeditions.fulfillment.shipping.domain.*;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.math.*;
import java.time.*;
import java.util.*;

/**
 * RecordPaymentCapturedInput.
 */
@org.jmolecules.ddd.annotation.ValueObject
public class RecordPaymentCapturedInput implements Serializable {

    @NotNull
    private String orderId;

    @NotNull
    private Instant capturedAt;

    public String getOrderId() {
        return orderId;
    }

    public RecordPaymentCapturedInput setOrderId(String orderId) {
        this.orderId = orderId;
        return this;
    }

    public Instant getCapturedAt() {
        return capturedAt;
    }

    public RecordPaymentCapturedInput setCapturedAt(Instant capturedAt) {
        this.capturedAt = capturedAt;
        return this;
    }
}
