package com.arcadiaeditions.fulfillment.shipping;

import com.arcadiaeditions.fulfillment.shipping.domain.*;
import com.arcadiaeditions.fulfillment.shipping.dtos.*;
import java.math.*;
import java.time.*;
import java.util.*;

/**
 * Inbound Service Port for managing [Fulfillment].
 */
@org.springframework.modulith.NamedInterface("FulfillmentShippingService")
@org.jspecify.annotations.NullMarked
public interface FulfillmentShippingService {

    /**
     * No-op, no event, when a Fulfillment exists for orderId (redelivery).
     *
     * With Events: [[FulfillmentScheduled, FulfillmentFailed]].
     *
     */
    public Fulfillment scheduleFulfillment(ScheduleFulfillmentInput input);
    /**
     * Money moved: the warehouse may now prepare the parcel. Sets paymentCapturedAt; no state change,
     * no event. No-op when there is no Fulfillment, it is not SCHEDULED, or paymentCapturedAt is set.
     *
     */
    public Optional<Fulfillment> recordPaymentCaptured(RecordPaymentCapturedInput input);
    /**
     * Warehouse action. Refused (no transition, no event) until paymentCapturedAt is set.
     *
     * With Events: [FulfillmentReadyToShip].
     *
     */
    public Fulfillment markReadyToShip(Long id);
    /**
     * No-op, no event, when there is no Fulfillment for orderId or it is FAILED or CANCELLED.
     *
     * With Events: [FulfillmentCancelled].
     *
     */
    public Optional<Fulfillment> cancelFulfillment(CancelFulfillmentInput input);
}
