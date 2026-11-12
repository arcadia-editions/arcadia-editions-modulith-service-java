package com.arcadiaeditions.catalog.inventory.service;

import com.arcadiaeditions.catalog.inventory.domain.*;
import com.arcadiaeditions.catalog.inventory.service.dtos.*;
import java.math.*;
import java.time.*;
import java.util.*;

/**
 * Inbound Service Port for managing [StockReservation].
 */
@org.springframework.modulith.NamedInterface("InventoryService")
@org.jspecify.annotations.NullMarked
@org.jmolecules.architecture.layered.ApplicationLayer
public interface InventoryService {

    /**
     *
     *
     */
    public ReserveStockResult reserveStock(ReserveStockInput input);
    /**
     *
     *
     * With Events: [StockReleased].
     *
     */
    public List<StockReservation> releaseStock(ReleaseStockInput input);
    /**
     *
     *
     * With Events: [StockReservationConfirmed].
     *
     */
    public List<StockReservation> confirmStockReservation(ConfirmStockReservationInput input);
}
