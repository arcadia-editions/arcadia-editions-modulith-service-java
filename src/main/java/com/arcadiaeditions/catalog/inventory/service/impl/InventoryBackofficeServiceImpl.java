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
 * Service Implementation for managing [InventoryPosition].
 */
@Service
@Transactional
@org.springframework.modulith.NamedInterface("InventoryBackofficeService")
@org.jspecify.annotations.NullMarked
@org.jmolecules.architecture.layered.ApplicationLayer
public class InventoryBackofficeServiceImpl implements InventoryBackofficeService {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final InventoryBackofficeServiceMapper inventoryBackofficeServiceMapper =
            InventoryBackofficeServiceMapper.INSTANCE;

    private final InventoryPositionRepository inventoryPositionRepository;

    private final EventsMapper eventsMapper = EventsMapper.INSTANCE;
    private final EventPublisher eventPublisher;

    /**
     * Constructor.
     */
    public InventoryBackofficeServiceImpl(
            InventoryPositionRepository inventoryPositionRepository, EventPublisher eventPublisher) {
        this.inventoryPositionRepository = inventoryPositionRepository;
        this.eventPublisher = eventPublisher;
    }

    public InventoryPosition createInventoryPosition(CreateInventoryPositionInput input) {

        log.debug("[CRUD] Request to save InventoryPosition: {}", input);
        var inventoryPosition = inventoryBackofficeServiceMapper.update(new InventoryPosition(), input);
        inventoryPosition = inventoryPositionRepository.save(inventoryPosition);
        // TODO: may need to reload the entity to fetch relationships 'mapped by id'
        return inventoryPosition;
    }

    public InventoryPosition receiveStock(ReceiveStockInput input) {

        log.debug("Request receiveStock: {}", input);

        var inventoryPosition = inventoryBackofficeServiceMapper.update(new InventoryPosition(), input);
        // TODO: implement this method
        // emit events
        var stockReceived = eventsMapper.asStockReceived(inventoryPosition);
        eventPublisher.onStockReceived(stockReceived);
        return inventoryPosition;
    }

    public InventoryPosition adjustStock(AdjustStockInput input) {

        log.debug("Request adjustStock: {}", input);

        var inventoryPosition = inventoryBackofficeServiceMapper.update(new InventoryPosition(), input);
        // TODO: implement this method
        // emit events
        var stockAdjusted = eventsMapper.asStockAdjusted(inventoryPosition);
        eventPublisher.onStockAdjusted(stockAdjusted);
        return inventoryPosition;
    }
}
