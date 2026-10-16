package com.arcadiaeditions.fulfillment.shipping;

import com.arcadiaeditions.fulfillment.shipping.dtos.ScheduleFulfillmentInput;

/**
 * Port to the external fulfillment/carrier decision (gap report X-03, research R-06).
 */
public interface FulfillmentDecision {

    Decision decide(ScheduleFulfillmentInput input);

    sealed interface Decision {}

    record Scheduled() implements Decision {}

    record Failed(String reason) implements Decision {}
}
