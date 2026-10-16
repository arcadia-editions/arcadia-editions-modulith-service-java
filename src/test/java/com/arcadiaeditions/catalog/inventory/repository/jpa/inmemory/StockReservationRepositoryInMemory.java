package com.arcadiaeditions.catalog.inventory.repository.jpa.inmemory;

import com.arcadiaeditions.catalog.inventory.domain.*;
import com.arcadiaeditions.catalog.inventory.repository.jpa.StockReservationRepository;
import java.math.*;
import java.time.*;
import java.util.*;

public class StockReservationRepositoryInMemory extends InMemoryJpaRepository<StockReservation>
        implements StockReservationRepository {

    @Override
    public List<StockReservation> findByOrderIdAndStatusIn(String orderId, Collection<ReservationStatus> statuses) {
        return getEntities().values().stream()
                .filter(reservation -> Objects.equals(orderId, reservation.getOrderId()))
                .filter(reservation -> statuses.contains(reservation.getStatus()))
                .toList();
    }
}
