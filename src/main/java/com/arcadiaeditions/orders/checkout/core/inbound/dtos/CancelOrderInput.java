package com.arcadiaeditions.orders.checkout.core.inbound.dtos;

import com.arcadiaeditions.orders.checkout.core.domain.*;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.math.*;
import java.time.*;
import java.util.*;

/**
 * CancelOrderInput.
 */
@org.jmolecules.ddd.annotation.ValueObject
public class CancelOrderInput implements Serializable {

    @NotNull
    private String reason;

    public String getReason() {
        return reason;
    }

    public CancelOrderInput setReason(String reason) {
        this.reason = reason;
        return this;
    }
}
