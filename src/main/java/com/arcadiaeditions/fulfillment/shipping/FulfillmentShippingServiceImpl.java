package com.arcadiaeditions.fulfillment.shipping;

import com.arcadiaeditions.fulfillment.shipping.domain.*;
import com.arcadiaeditions.fulfillment.shipping.domain.events.*;
import com.arcadiaeditions.fulfillment.shipping.dtos.*;
import com.arcadiaeditions.fulfillment.shipping.events.*;
import com.arcadiaeditions.fulfillment.shipping.mappers.*;
import java.math.*;
import java.time.*;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing [Fulfillment].
 */
@Service
@Transactional
@org.springframework.modulith.NamedInterface("FulfillmentShippingService")
@org.jspecify.annotations.NullMarked
public class FulfillmentShippingServiceImpl implements FulfillmentShippingService {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final FulfillmentShippingServiceMapper fulfillmentShippingServiceMapper =
            FulfillmentShippingServiceMapper.INSTANCE;

    private final FulfillmentRepository fulfillmentRepository;

    private final EventsMapper eventsMapper = EventsMapper.INSTANCE;
    private final EventPublisher eventPublisher;

    /**
     * Constructor.
     */
    public FulfillmentShippingServiceImpl(FulfillmentRepository fulfillmentRepository, EventPublisher eventPublisher) {
        this.fulfillmentRepository = fulfillmentRepository;
        this.eventPublisher = eventPublisher;
    }

    public Fulfillment scheduleFulfillment(ScheduleFulfillmentInput input) {

        log.debug("Request scheduleFulfillment: {}", input);

        var fulfillment = fulfillmentShippingServiceMapper.update(new Fulfillment(), input);
        // TODO: implement this method
        // emit events
        var fulfillmentScheduled = eventsMapper.asFulfillmentScheduled(fulfillment);
        eventPublisher.onFulfillmentScheduled(fulfillmentScheduled);
        var fulfillmentFailed = eventsMapper.asFulfillmentFailed(fulfillment);
        eventPublisher.onFulfillmentFailed(fulfillmentFailed);
        return fulfillment;
    }

    public Fulfillment markReadyToShip(Long id) {

        log.debug("Request markReadyToShip: {}", id);

        var fulfillment = fulfillmentRepository
                .findById(id)
                .map(existingFulfillment -> {
                    FulfillmentTransitions.ensureCanMarkReadyToShip(existingFulfillment);
                    existingFulfillment.setStatus(FulfillmentStatus.READY_TO_SHIP);
                    return existingFulfillment;
                })
                .map(fulfillmentRepository::save)
                .orElseThrow();
        // emit events
        var fulfillmentReadyToShip = eventsMapper.asFulfillmentReadyToShip(fulfillment);
        eventPublisher.onFulfillmentReadyToShip(fulfillmentReadyToShip);
        return fulfillment;
    }
}
