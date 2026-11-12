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

    /**
     * Constructor.
     */
    public InventoryServiceImpl(
            StockReservationRepository stockReservationRepository,
            InventoryPositionRepository inventoryPositionRepository,
            EventPublisher eventPublisher,
            Clock clock) {
        this.stockReservationRepository = stockReservationRepository;
        this.inventoryPositionRepository = inventoryPositionRepository;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
    }

    public ReserveStockResult reserveStock(ReserveStockInput input) {

        log.debug("Request reserveStock: {}", input);

        // lock one position per sku, in sku order (stable lock order), summing repeated skus
        var requested = new java.util.TreeMap<String, Integer>();
        input.getItems().forEach(item -> requested.merge(item.getSku(), item.getQuantity(), Integer::sum));
        var positions = new java.util.HashMap<String, InventoryPosition>();
        requested.forEach((sku, quantity) -> {
            var position = inventoryPositionRepository
                    .findBySkuForUpdate(sku)
                    .filter(p -> p.getAvailableQuantity() >= quantity)
                    .orElseThrow(() -> new UnsupportedOperationException(
                            "StockUnavailableResult branch is task T-14 (sku " + sku + ")"));
            positions.put(sku, position);
        });

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

        // TODO CUSTOM_REQUIRED: load the StockReservation(s) this transition applies to, e.g. by a correlation key in
        // the input
        List<StockReservation> stockReservations = List.of();
        stockReservations.forEach(existingStockReservation -> {
            StockReservationTransitions.ensureCanReleaseStock(existingStockReservation);
            inventoryServiceMapper.update(existingStockReservation, input);
            existingStockReservation.setStatus(ReservationStatus.RELEASED);
        });
        stockReservations = stockReservationRepository.saveAll(stockReservations);
        // emit events
        stockReservations.forEach(eventSource -> {
            var stockReleased = eventsMapper.asStockReleased(eventSource);
            eventPublisher.onStockReleased(stockReleased);
        });
        return stockReservations;
    }

    public List<StockReservation> confirmStockReservation(ConfirmStockReservationInput input) {

        log.debug("Request confirmStockReservation: {}", input);

        // TODO CUSTOM_REQUIRED: load the StockReservation(s) this transition applies to, e.g. by a correlation key in
        // the input
        List<StockReservation> stockReservations = List.of();
        stockReservations.forEach(existingStockReservation -> {
            StockReservationTransitions.ensureCanConfirmStockReservation(existingStockReservation);
            inventoryServiceMapper.update(existingStockReservation, input);
            existingStockReservation.setStatus(ReservationStatus.CONFIRMED);
        });
        stockReservations = stockReservationRepository.saveAll(stockReservations);
        // emit events
        stockReservations.forEach(eventSource -> {
            var stockReservationConfirmed = eventsMapper.asStockReservationConfirmed(eventSource);
            eventPublisher.onStockReservationConfirmed(stockReservationConfirmed);
        });
        return stockReservations;
    }
}
