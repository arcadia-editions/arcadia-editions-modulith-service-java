package com.arcadiaeditions.fulfillment.shipping.dtos;

import com.arcadiaeditions.fulfillment.shipping.domain.*;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.math.*;
import java.time.*;
import java.util.*;

/**
 * CancelFulfillmentInput.
 */
@org.jmolecules.ddd.annotation.ValueObject
public class CancelFulfillmentInput implements Serializable {

    @NotNull
    private String orderId;

    @NotNull
    private String reason;

    public String getOrderId() {
        return orderId;
    }

    public CancelFulfillmentInput setOrderId(String orderId) {
        this.orderId = orderId;
        return this;
    }

    public String getReason() {
        return reason;
    }

    public CancelFulfillmentInput setReason(String reason) {
        this.reason = reason;
        return this;
    }
}
