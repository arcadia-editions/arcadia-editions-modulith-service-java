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
     *
     *
     * With Events: [[FulfillmentScheduled, FulfillmentFailed]].
     *
     */
    public Fulfillment scheduleFulfillment(ScheduleFulfillmentInput input);
    /**
     *
     *
     * With Events: [FulfillmentReadyToShip].
     *
     */
    public Fulfillment markReadyToShip(Long id);
}
