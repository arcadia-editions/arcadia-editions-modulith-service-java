package com.arcadiaeditions.catalog.inventory.service.impl;

import com.arcadiaeditions.catalog.inventory.domain.*;
import com.arcadiaeditions.catalog.inventory.domain.events.*;
import com.arcadiaeditions.catalog.inventory.events.*;
import com.arcadiaeditions.catalog.inventory.repository.jpa.*;
import com.arcadiaeditions.catalog.inventory.service.*;
import com.arcadiaeditions.catalog.inventory.service.dtos.*;
import com.arcadiaeditions.catalog.inventory.service.impl.mappers.*;
import com.arcadiaeditions.common.BusinessIds;
import java.math.*;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;
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
            Clock clock,
            EventPublisher eventPublisher) {
        this.stockReservationRepository = stockReservationRepository;
        this.inventoryPositionRepository = inventoryPositionRepository;
        this.clock = clock;
        this.eventPublisher = eventPublisher;
    }

    public ReserveStockResult reserveStock(ReserveStockInput input) {

        log.debug("Request reserveStock: {}", input);

        // sum quantities per sku (duplicate SKUs in one request), keeping request order
        Map<String, Integer> requested = new LinkedHashMap<>();
        input.getItems().forEach(item -> requested.merge(item.getSku(), item.getQuantity(), Integer::sum));

        // lock the positions in SKU order (R-04); an unknown SKU has nothing available
        Map<String, InventoryPosition> positions = new HashMap<>();
        inventoryPositionRepository
                .findAllBySkuInOrderBySku(requested.keySet())
                .forEach(position -> positions.put(position.getSku(), position));

        // check every line first: all or nothing
        List<StockUnavailableLine> shortLines = new ArrayList<>();
        requested.forEach((sku, quantity) -> {
            var position = positions.get(sku);
            int available = position == null ? 0 : position.getAvailableQuantity();
            if (quantity > available) {
                shortLines.add(inventoryServiceMapper.asStockUnavailableLine(sku, quantity, available));
            }
        });
        if (!shortLines.isEmpty()) {
            var unavailable = inventoryServiceMapper.asStockUnavailableResult(input.getOrderId(), shortLines);
            return inventoryServiceMapper.asReserveStockResult(unavailable);
        }

        var reservedAt = Instant.now(clock);
        var expiresAt = reservedAt.plus(RESERVATION_TTL);
        List<StockReservation> reservations = new ArrayList<>();
        requested.forEach((sku, quantity) -> {
            var position = positions.get(sku);
            position.setAvailableQuantity(position.getAvailableQuantity() - quantity);
            position.setReservedQuantity(position.getReservedQuantity() + quantity);
            reservations.add(new StockReservation()
                    .setReservationId(BusinessIds.newId("RES"))
                    .setOrderId(input.getOrderId())
                    .setSku(sku)
                    .setQuantity(quantity)
                    .setStatus(ReservationStatus.RESERVED)
                    .setReservedAt(reservedAt)
                    .setExpiresAt(expiresAt));
        });
        inventoryPositionRepository.saveAll(positions.values());
        var saved = stockReservationRepository.saveAll(reservations);

        var reserved = inventoryServiceMapper.asStockReservedResult(input.getOrderId(), expiresAt, saved);
        return inventoryServiceMapper.asReserveStockResult(reserved);
    }

    public List<StockReservation> releaseStock(ReleaseStockInput input) {

        log.debug("Request releaseStock: {}", input);

        List<StockReservation> stockReservations = stockReservationRepository.findByOrderIdAndStatusIn(
                input.getOrderId(), List.of(ReservationStatus.RESERVED, ReservationStatus.CONFIRMED));
        if (stockReservations.isEmpty()) {
            return stockReservations; // already released (or nothing reserved): idempotent, no event
        }
        var releasedAt = Instant.now(clock);
        Map<String, InventoryPosition> positions = new HashMap<>();
        inventoryPositionRepository
                .findAllBySkuInOrderBySku(
                        stockReservations.stream().map(StockReservation::getSku).collect(Collectors.toSet()))
                .forEach(position -> positions.put(position.getSku(), position));
        stockReservations.forEach(existingStockReservation -> {
            StockReservationTransitions.ensureCanReleaseStock(existingStockReservation);
            var position = positions.get(existingStockReservation.getSku());
            int quantity = existingStockReservation.getQuantity();
            if (existingStockReservation.getStatus() == ReservationStatus.CONFIRMED) {
                position.setSoldQuantity(position.getSoldQuantity() - quantity); // Q-09: sold -> available
            } else {
                position.setReservedQuantity(position.getReservedQuantity() - quantity);
            }
            position.setAvailableQuantity(position.getAvailableQuantity() + quantity);
            existingStockReservation.setStatus(ReservationStatus.RELEASED);
            existingStockReservation.setReleasedAt(releasedAt);
            existingStockReservation.setReleaseReason(input.getReason());
        });
        inventoryPositionRepository.saveAll(positions.values());
        stockReservations = stockReservationRepository.saveAll(stockReservations);
        // emit one event for the order, not one per reservation
        eventPublisher.onStockReleased(eventsMapper.asStockReleased(stockReservations, input));
        return stockReservations;
    }

    public List<StockReservation> confirmStockReservation(ConfirmStockReservationInput input) {

        log.debug("Request confirmStockReservation: {}", input);

        List<StockReservation> stockReservations = stockReservationRepository.findByOrderIdAndStatusIn(
                input.getOrderId(), List.of(ReservationStatus.RESERVED));
        if (stockReservations.isEmpty()) {
            return stockReservations; // already confirmed (or nothing reserved): idempotent, no event
        }
        var confirmedAt = Instant.now(clock);
        Map<String, InventoryPosition> positions = new HashMap<>();
        inventoryPositionRepository
                .findAllBySkuInOrderBySku(
                        stockReservations.stream().map(StockReservation::getSku).collect(Collectors.toSet()))
                .forEach(position -> positions.put(position.getSku(), position));
        stockReservations.forEach(existingStockReservation -> {
            StockReservationTransitions.ensureCanConfirmStockReservation(existingStockReservation);
            existingStockReservation.setStatus(ReservationStatus.CONFIRMED);
            existingStockReservation.setConfirmedAt(confirmedAt);
            var position = positions.get(existingStockReservation.getSku());
            position.setReservedQuantity(position.getReservedQuantity() - existingStockReservation.getQuantity());
            position.setSoldQuantity(position.getSoldQuantity() + existingStockReservation.getQuantity());
        });
        inventoryPositionRepository.saveAll(positions.values());
        stockReservations = stockReservationRepository.saveAll(stockReservations);
        // emit one event for the order, not one per reservation
        eventPublisher.onStockReservationConfirmed(eventsMapper.asStockReservationConfirmed(stockReservations));
        return stockReservations;
    }
}
