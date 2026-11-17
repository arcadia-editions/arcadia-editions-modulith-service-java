package com.arcadiaeditions.fulfillment.shipping;

import java.util.List;

/**
 * Fulfillment decision port: whether picking, packing and dispatch can be arranged for a confirmed order
 * (warehouse, carrier).
 */
@org.jmolecules.architecture.hexagonal.SecondaryPort
public interface FulfillmentDecision {

    DecisionResult decide(DecisionRequest request);

    record DecisionRequest(String orderId, String postalCode, String country, List<Item> items) {}

    record Item(String sku, Integer quantity) {}

    /** {@code scheduled} false carries the {@code reason}. */
    record DecisionResult(boolean scheduled, String reason) {

        public static DecisionResult scheduledResult() {
            return new DecisionResult(true, null);
        }

        public static DecisionResult failed(String reason) {
            return new DecisionResult(false, reason);
        }
    }
}
