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
     * The type of the triggering event sets the reason. No-op, no event, when already RELEASED.
     *
     * With Events: [StockReleased].
     *
     */
    public List<StockReservation> releaseStock(ReleaseStockInput input);
    /**
     * Reservation timer, 10 minutes after OrderCreated. No-op, no event, when no line of the order
     * is RESERVED. Releases them (RESERVATION_EXPIRED) only when payments answers NO_PAYMENT;
     * on STARTED, payment owns the end and nothing happens.
     *
     * With Events: [StockReleased].
     *
     */
    public List<StockReservation> expireReservation(ExpireReservationInput input);
    /**
     *
     *
     * With Events: [StockReservationConfirmed].
     *
     */
    public ConfirmStockReservationResult confirmStockReservation(ConfirmStockReservationInput input);
}
