package com.arcadiaeditions.fulfillment.shipping;

import com.arcadiaeditions.fulfillment.shipping.dtos.ScheduleFulfillmentInput;
import org.springframework.stereotype.Component;

/**
 * Deterministic stand-in for the external fulfillment decision (research R-06, contracts/stand-in-adapters.md).
 *
 * <p>{@code shippingAddress.postalCode} = {@code 00000} fails with {@code carrier_unavailable}; anything else
 * (including a missing address) is scheduled.
 */
@Component
public class StandInFulfillmentDecision implements FulfillmentDecision {

    static final String UNAVAILABLE_POSTAL_CODE = "00000";

    @Override
    public Decision decide(ScheduleFulfillmentInput input) {
        var address = input != null ? input.getShippingAddress() : null;
        if (address != null && UNAVAILABLE_POSTAL_CODE.equals(address.getPostalCode())) {
            return new Failed("carrier_unavailable");
        }
        return new Scheduled();
    }
}
