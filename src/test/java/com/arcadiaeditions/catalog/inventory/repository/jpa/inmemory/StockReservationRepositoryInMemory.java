package com.arcadiaeditions.catalog.inventory.repository.jpa.inmemory;

import com.arcadiaeditions.catalog.inventory.domain.*;
import com.arcadiaeditions.catalog.inventory.repository.jpa.StockReservationRepository;
import java.math.*;
import java.time.*;
import java.util.*;

public class StockReservationRepositoryInMemory extends InMemoryJpaRepository<StockReservation>
        implements StockReservationRepository {}
