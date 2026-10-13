package com.arcadiaeditions.catalog.inventory.service;

import com.arcadiaeditions.catalog.inventory.domain.*;
import com.arcadiaeditions.catalog.inventory.service.dtos.*;
import java.math.*;
import java.time.*;
import java.util.*;

/**
 * Inbound Service Port for managing [InventoryPosition].
 */
@org.springframework.modulith.NamedInterface("InventoryBackofficeService")
@org.jspecify.annotations.NullMarked
@org.jmolecules.architecture.layered.ApplicationLayer
public interface InventoryBackofficeService {

    /**
     *
     *
     */
    public InventoryPosition createInventoryPosition(CreateInventoryPositionInput input);
    /**
     *
     *
     * With Events: [StockReceived].
     *
     */
    public InventoryPosition receiveStock(ReceiveStockInput input);
    /**
     *
     *
     * With Events: [StockAdjusted].
     *
     */
    public InventoryPosition adjustStock(AdjustStockInput input);
}
