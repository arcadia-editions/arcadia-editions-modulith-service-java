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

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final InventoryServiceMapper inventoryServiceMapper = InventoryServiceMapper.INSTANCE;

    private final StockReservationRepository stockReservationRepository;

    private final EventsMapper eventsMapper = EventsMapper.INSTANCE;
    private final EventPublisher eventPublisher;

    /**
     * Constructor.
     */
    public InventoryServiceImpl(StockReservationRepository stockReservationRepository, EventPublisher eventPublisher) {
        this.stockReservationRepository = stockReservationRepository;
        this.eventPublisher = eventPublisher;
    }

    public ReserveStockResult reserveStock(ReserveStockInput input) {

        log.debug("Request reserveStock: {}", input);

        var stockReservation = inventoryServiceMapper.update(new StockReservation(), input);
        // TODO: implement this method
        return inventoryServiceMapper.asReserveStockResult(stockReservation);
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
