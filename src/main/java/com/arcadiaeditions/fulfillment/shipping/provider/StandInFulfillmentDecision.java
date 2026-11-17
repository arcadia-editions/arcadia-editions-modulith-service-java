package com.arcadiaeditions.fulfillment.shipping.provider;

import com.arcadiaeditions.fulfillment.shipping.FulfillmentDecision;
import org.springframework.stereotype.Component;

/**
 * Deterministic stand-in for the warehouse/carrier decision (data.sql header assumptions): shipping postal code
 * 00000 fails with "carrier_unavailable"; any other value is scheduled.
 */
@Component
@org.jmolecules.architecture.hexagonal.SecondaryAdapter
public class StandInFulfillmentDecision implements FulfillmentDecision {

    public static final String POSTAL_CARRIER_UNAVAILABLE = "00000";
    public static final String CARRIER_UNAVAILABLE = "carrier_unavailable";

    @Override
    public DecisionResult decide(DecisionRequest request) {
        if (POSTAL_CARRIER_UNAVAILABLE.equals(request.postalCode())) {
            return DecisionResult.failed(CARRIER_UNAVAILABLE);
        }
        return DecisionResult.scheduledResult();
    }
}
