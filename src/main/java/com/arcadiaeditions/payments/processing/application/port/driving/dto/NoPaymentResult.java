package com.arcadiaeditions.payments.processing.application.port.driving.dto;

import com.arcadiaeditions.payments.processing.domain.*;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.math.*;
import java.time.*;
import java.util.*;

/**
 * NoPaymentResult.
 */
@org.jmolecules.ddd.annotation.ValueObject
public class NoPaymentResult implements Serializable {

    private String orderId;

    public String getOrderId() {
        return orderId;
    }

    public NoPaymentResult setOrderId(String orderId) {
        this.orderId = orderId;
        return this;
    }
}
