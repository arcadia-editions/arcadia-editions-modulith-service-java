package com.arcadiaeditions.fulfillment.shipping.events;

import com.arcadiaeditions.fulfillment.shipping.domain.*;
import com.arcadiaeditions.fulfillment.shipping.domain.events.*;

public interface EventPublisher {

    void onFulfillmentScheduled(FulfillmentScheduled event);

    void onFulfillmentReadyToShip(FulfillmentReadyToShip event);

    void onFulfillmentFailed(FulfillmentFailed event);
}
