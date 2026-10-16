package com.arcadiaeditions.fulfillment.shipping;

import com.arcadiaeditions.common.BusinessIds;
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

    private final FulfillmentDecision fulfillmentDecision;
    private final Clock clock;

    /**
     * Constructor.
     */
    public FulfillmentShippingServiceImpl(
            FulfillmentRepository fulfillmentRepository,
            EventPublisher eventPublisher,
            FulfillmentDecision fulfillmentDecision,
            Clock clock) {
        this.fulfillmentRepository = fulfillmentRepository;
        this.eventPublisher = eventPublisher;
        this.fulfillmentDecision = fulfillmentDecision;
        this.clock = clock;
    }

    public Fulfillment scheduleFulfillment(ScheduleFulfillmentInput input) {

        log.debug("Request scheduleFulfillment: {}", input);

        var existing = fulfillmentRepository.findByOrderId(input.getOrderId());
        if (existing.isPresent()) {
            // idempotent redelivery (X-01): return the current aggregate, emit nothing
            return existing.get();
        }

        var fulfillment = fulfillmentShippingServiceMapper.update(new Fulfillment(), input);
        fulfillment.setFulfillmentId(BusinessIds.newId("FUL"));

        switch (fulfillmentDecision.decide(input)) {
            case FulfillmentDecision.Scheduled scheduled -> {
                fulfillment.setStatus(FulfillmentStatus.SCHEDULED);
                fulfillment.setScheduledAt(Instant.now(clock));
                fulfillment = fulfillmentRepository.save(fulfillment);
                // emit events
                eventPublisher.onFulfillmentScheduled(eventsMapper.asFulfillmentScheduled(fulfillment));
            }
            case FulfillmentDecision.Failed failed -> {
                fulfillment.setStatus(FulfillmentStatus.FAILED);
                fulfillment.setFailedAt(Instant.now(clock));
                fulfillment.setFailureReason(failed.reason());
                fulfillment = fulfillmentRepository.save(fulfillment);
                // emit events
                eventPublisher.onFulfillmentFailed(eventsMapper.asFulfillmentFailed(fulfillment));
            }
        }
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
