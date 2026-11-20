package com.arcadiaeditions.catalog.inventory.service.impl;

import com.arcadiaeditions.catalog.inventory.domain.*;
import com.arcadiaeditions.catalog.inventory.domain.events.*;
import com.arcadiaeditions.catalog.inventory.events.*;
import com.arcadiaeditions.catalog.inventory.repository.jpa.*;
import com.arcadiaeditions.catalog.inventory.service.*;
import com.arcadiaeditions.catalog.inventory.service.dtos.*;
import com.arcadiaeditions.catalog.inventory.service.impl.mappers.*;
import java.math.*;
import java.time.*;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing [StockReservation].
 */
@Service
@Transactional
@org.springframework.modulith.NamedInterface("InventoryService")
@org.jspecify.annotations.NullMarked
@org.jmolecules.architecture.layered.ApplicationLayer
public class InventoryServiceImpl implements InventoryService {

    private static final Duration RESERVATION_TTL = Duration.ofMinutes(10);

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final InventoryServiceMapper inventoryServiceMapper = InventoryServiceMapper.INSTANCE;

    private final StockReservationRepository stockReservationRepository;

    private final InventoryPositionRepository inventoryPositionRepository;
    private final Clock clock;

    private final EventsMapper eventsMapper = EventsMapper.INSTANCE;
    private final EventPublisher eventPublisher;

    private final com.arcadiaeditions.payments.processing.application.port.driving.PaymentsProcessingService
            paymentsProcessingService;

    /**
     * Constructor.
     */
    public InventoryServiceImpl(
            StockReservationRepository stockReservationRepository,
            InventoryPositionRepository inventoryPositionRepository,
            EventPublisher eventPublisher,
            com.arcadiaeditions.payments.processing.application.port.driving.PaymentsProcessingService
                    paymentsProcessingService,
            Clock clock) {
        this.stockReservationRepository = stockReservationRepository;
        this.inventoryPositionRepository = inventoryPositionRepository;
        this.eventPublisher = eventPublisher;
        this.paymentsProcessingService = paymentsProcessingService;
        this.clock = clock;
    }

    public ReserveStockResult reserveStock(ReserveStockInput input) {

        log.debug("Request reserveStock: {}", input);

        // lock one position per sku, in sku order (stable lock order), summing repeated skus
        var requested = new java.util.TreeMap<String, Integer>();
        input.getItems().forEach(item -> requested.merge(item.getSku(), item.getQuantity(), Integer::sum));
        var positions = new java.util.HashMap<String, InventoryPosition>();
        var unavailable = new StockUnavailableResult().setOrderId(input.getOrderId());
        requested.forEach((sku, quantity) -> {
            var position = inventoryPositionRepository.findBySkuForUpdate(sku);
            int available = position.map(InventoryPosition::getAvailableQuantity).orElse(0);
            if (available >= quantity) {
                positions.put(sku, position.get());
            } else {
                unavailable.addUnavailableItems(new StockUnavailableLine()
                        .setSku(sku)
                        .setRequestedQuantity(quantity)
                        .setAvailableQuantity(available));
            }
        });
        if (!unavailable.getUnavailableItems().isEmpty()) {
            // all or nothing: no reservation, no counter change
            return new ReserveStockResult().setOutcome(ReserveStockOutcome.UNAVAILABLE).setUnavailable(unavailable);
        }

        var reservedAt = clock.instant();
        var expiresAt = reservedAt.plus(RESERVATION_TTL);
        var reservations = input.getItems().stream()
                .map(item -> new StockReservation()
                        .setReservationId(com.arcadiaeditions.common.BusinessIds.newId("RES"))
                        .setOrderId(input.getOrderId())
                        .setSku(item.getSku())
                        .setQuantity(item.getQuantity())
                        .setStatus(ReservationStatus.RESERVED)
                        .setReservedAt(reservedAt)
                        .setExpiresAt(expiresAt))
                .toList();
        requested.forEach((sku, quantity) -> {
            var position = positions.get(sku);
            position.setAvailableQuantity(position.getAvailableQuantity() - quantity);
            position.setReservedQuantity(position.getReservedQuantity() + quantity);
            inventoryPositionRepository.save(position);
        });
        var saved = stockReservationRepository.saveAll(reservations);

        var reserved = new StockReservedResult().setOrderId(input.getOrderId()).setExpiresAt(expiresAt);
        saved.forEach(r -> reserved.addReservations(new StockReservedLine()
                .setReservationId(r.getReservationId())
                .setSku(r.getSku())
                .setQuantity(r.getQuantity())
                .setReservedAt(r.getReservedAt())));
        return new ReserveStockResult().setOutcome(ReserveStockOutcome.RESERVED).setReserved(reserved);
    }

    public List<StockReservation> releaseStock(ReleaseStockInput input) {

        log.debug("Request releaseStock: {}", input);

        // only RESERVED or CONFIRMED reservations of the order; already released ones (redelivery) are left alone
        var releasable = stockReservationRepository.findByOrderId(input.getOrderId()).stream()
                .filter(r -> r.getStatus() == ReservationStatus.RESERVED || r.getStatus() == ReservationStatus.CONFIRMED)
                .toList();
        releasable.forEach(StockReservationTransitions::ensureCanReleaseStock);
        return release(input.getOrderId(), input.getReason(), releasable);
    }

    /** Releases the given reservations of the order, puts their stock back and emits one StockReleased. */
    private List<StockReservation> release(
            String orderId, StockReleaseReason reason, List<StockReservation> releasable) {
        if (releasable.isEmpty()) {
            return List.of();
        }

        var releasedAt = clock.instant();
        // back to available: RESERVED leaves reserved, CONFIRMED leaves sold
        var reservedBack = new java.util.TreeMap<String, Integer>();
        var soldBack = new java.util.TreeMap<String, Integer>();
        releasable.forEach(reservation -> {
            var counter = reservation.getStatus() == ReservationStatus.CONFIRMED ? soldBack : reservedBack;
            counter.merge(reservation.getSku(), reservation.getQuantity(), Integer::sum);
            reservation
                    .setStatus(ReservationStatus.RELEASED)
                    .setReleasedAt(releasedAt)
                    .setReleaseReason(reason);
        });
        var skus = new java.util.TreeSet<String>();
        skus.addAll(reservedBack.keySet());
        skus.addAll(soldBack.keySet());
        skus.forEach(sku -> {
            var position = inventoryPositionRepository
                    .findBySkuForUpdate(sku)
                    .orElseThrow(() -> new IllegalStateException("No inventory position for sku " + sku));
            int fromReserved = reservedBack.getOrDefault(sku, 0);
            int fromSold = soldBack.getOrDefault(sku, 0);
            position.setAvailableQuantity(position.getAvailableQuantity() + fromReserved + fromSold);
            position.setReservedQuantity(position.getReservedQuantity() - fromReserved);
            position.setSoldQuantity(position.getSoldQuantity() - fromSold);
            inventoryPositionRepository.save(position);
        });

        var saved = stockReservationRepository.saveAll(releasable);

        var event = new StockReleased()
                .setOrderId(orderId)
                .setReason(reason)
                .setReleasedAt(releasedAt)
                .setItems(saved.stream()
                        .map(r -> new StockReleasedItem()
                                .setReservationId(r.getReservationId())
                                .setSku(r.getSku())
                                .setQuantity(r.getQuantity()))
                        .toList());
        eventPublisher.onStockReleased(event);
        return saved;
    }

    public ConfirmStockReservationResult confirmStockReservation(ConfirmStockReservationInput input) {
        var held = stockReservationRepository.findByOrderId(input.getOrderId()).stream()
                .filter(r -> r.getStatus() == ReservationStatus.RESERVED || r.getStatus() == ReservationStatus.CONFIRMED)
                .toList();
        if (held.isEmpty()) {
            // e.g. released by the reservation timer: no change, no event
            return new ConfirmStockReservationResult()
                    .setOutcome(ConfirmStockReservationOutcome.NOT_HELD)
                    .setNotHeld(new StockReservationNotHeldResult().setOrderId(input.getOrderId()));
        }
        var confirmed = confirmReservedStock(input);
        // redelivery: nothing to confirm now, answer with the original confirmation time
        var confirmedAt = (confirmed.isEmpty() ? held : confirmed).get(0).getConfirmedAt();
        return new ConfirmStockReservationResult()
                .setOutcome(ConfirmStockReservationOutcome.CONFIRMED)
                .setConfirmed(new StockReservationConfirmedResult()
                        .setOrderId(input.getOrderId())
                        .setConfirmedAt(confirmedAt));
    }

    private List<StockReservation> confirmReservedStock(ConfirmStockReservationInput input) {

        log.debug("Request confirmStockReservation: {}", input);

        // only the RESERVED reservations of the order; already confirmed ones (redelivery) are left alone
        var reserved = stockReservationRepository.findByOrderId(input.getOrderId()).stream()
                .filter(r -> r.getStatus() == ReservationStatus.RESERVED)
                .toList();
        if (reserved.isEmpty()) {
            return List.of();
        }

        var confirmedAt = clock.instant();
        reserved.forEach(reservation -> {
            StockReservationTransitions.ensureCanConfirmStockReservation(reservation);
            reservation.setStatus(ReservationStatus.CONFIRMED).setConfirmedAt(confirmedAt);
        });

        // reserved moves to sold, one locked position per sku in sku order
        var quantities = new java.util.TreeMap<String, Integer>();
        reserved.forEach(r -> quantities.merge(r.getSku(), r.getQuantity(), Integer::sum));
        quantities.forEach((sku, quantity) -> {
            var position = inventoryPositionRepository
                    .findBySkuForUpdate(sku)
                    .orElseThrow(() -> new IllegalStateException("No inventory position for sku " + sku));
            position.setReservedQuantity(position.getReservedQuantity() - quantity);
            position.setSoldQuantity(position.getSoldQuantity() + quantity);
            inventoryPositionRepository.save(position);
        });

        var saved = stockReservationRepository.saveAll(reserved);

        var event = new StockReservationConfirmed()
                .setOrderId(input.getOrderId())
                .setConfirmedAt(confirmedAt)
                .setItems(saved.stream()
                        .map(r -> new StockReservationConfirmedItem()
                                .setReservationId(r.getReservationId())
                                .setSku(r.getSku())
                                .setQuantity(r.getQuantity()))
                        .toList());
        eventPublisher.onStockReservationConfirmed(event);
        return saved;
    }

    public List<StockReservation> expireReservation(ExpireReservationInput input) {

        log.debug("Request expireReservation: {}", input);

        // only the RESERVED reservations of the order; nothing RESERVED (late fire, redelivery) is a no-op
        var reserved = stockReservationRepository.findByOrderId(input.getOrderId()).stream()
                .filter(r -> r.getStatus() == ReservationStatus.RESERVED)
                .toList();
        if (reserved.isEmpty()) {
            return List.of();
        }

        // released only when no payment has started; once started (even in flight) payment owns the end
        var paymentStatus = paymentsProcessingService.getPaymentStatus(
                new com.arcadiaeditions.payments.processing.application.port.driving.dto.GetPaymentStatusInput()
                        .setOrderId(input.getOrderId()));
        if (paymentStatus.getOutcome()
                != com.arcadiaeditions.payments.processing.domain.GetPaymentStatusOutcome.NO_PAYMENT) {
            return List.of();
        }

        reserved.forEach(StockReservationTransitions::ensureCanExpireReservation);
        return release(input.getOrderId(), StockReleaseReason.RESERVATION_EXPIRED, reserved);
    }
}
