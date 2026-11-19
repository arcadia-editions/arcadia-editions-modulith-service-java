package com.arcadiaeditions.fulfillment.shipping;

import com.arcadiaeditions.fulfillment.shipping.domain.*;
import com.arcadiaeditions.fulfillment.shipping.domain.events.*;
import com.arcadiaeditions.fulfillment.shipping.dtos.*;
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

        // one Fulfillment per order: OrderConfirmed redelivered changes nothing and emits nothing
        var existing = fulfillmentRepository.findByOrderId(input.getOrderId());
        if (existing.isPresent()) {
            log.debug(
                    "scheduleFulfillment: fulfillment of order {} already {}",
                    input.getOrderId(),
                    existing.get().getStatus());
            return existing.get();
        }

        var fulfillment = fulfillmentShippingServiceMapper.update(new Fulfillment(), input);
        fulfillment.setFulfillmentId(com.arcadiaeditions.common.BusinessIds.newId("FUL"));

        var shippingAddress = input.getShippingAddress();
        var result = fulfillmentDecision.decide(new FulfillmentDecision.DecisionRequest(
                input.getOrderId(),
                shippingAddress != null ? shippingAddress.getPostalCode() : null,
                shippingAddress != null ? shippingAddress.getCountry() : null,
                input.getItems().stream()
                        .map(item -> new FulfillmentDecision.Item(item.getSku(), item.getQuantity()))
                        .toList()));

        if (!result.scheduled()) {
            fulfillment.setStatus(FulfillmentStatus.FAILED);
            fulfillment.setFailedAt(Instant.now(clock));
            fulfillment.setFailureReason(result.reason());
            fulfillment = fulfillmentRepository.save(fulfillment);
            eventPublisher.onFulfillmentFailed(eventsMapper.asFulfillmentFailed(fulfillment));
            return fulfillment;
        }

        fulfillment.setStatus(FulfillmentStatus.SCHEDULED);
        fulfillment.setScheduledAt(Instant.now(clock));
        fulfillment = fulfillmentRepository.save(fulfillment);
        eventPublisher.onFulfillmentScheduled(eventsMapper.asFulfillmentScheduled(fulfillment));
        return fulfillment;
    }

    public Fulfillment markReadyToShip(Long id) {

        log.debug("Request markReadyToShip: {}", id);

        var fulfillment = fulfillmentRepository
                .findById(id)
                .map(existingFulfillment -> {
                    FulfillmentTransitions.ensureCanMarkReadyToShip(existingFulfillment);
                    // the warehouse ships only after capture: refused, no transition, no event
                    if (existingFulfillment.getPaymentCapturedAt() == null) {
                        throw new PaymentNotCapturedException(existingFulfillment.getFulfillmentId());
                    }
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

    public Optional<Fulfillment> recordPaymentCaptured(RecordPaymentCapturedInput input) {

        log.debug("Request recordPaymentCaptured: {}", input);

        // no state change, no event; no-op when there is no Fulfillment, it is not SCHEDULED or already recorded
        return fulfillmentRepository
                .findByOrderId(input.getOrderId())
                .filter(f -> f.getStatus() == FulfillmentStatus.SCHEDULED)
                .filter(f -> f.getPaymentCapturedAt() == null)
                .map(f -> {
                    f.setPaymentCapturedAt(input.getCapturedAt());
                    return fulfillmentRepository.save(f);
                });
    }

    // baseline/place-order-flow-3 compile stub: implemented by a task of docs/tasks.md (no-op until then)
    public Optional<Fulfillment> cancelFulfillment(CancelFulfillmentInput input) {
        return Optional.empty();
    }
}
