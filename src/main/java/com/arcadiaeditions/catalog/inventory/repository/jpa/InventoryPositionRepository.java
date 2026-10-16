package com.arcadiaeditions.catalog.inventory.repository.jpa;

import com.arcadiaeditions.catalog.inventory.domain.*;
import jakarta.persistence.LockModeType;
import java.math.*;
import java.time.*;
import java.util.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the InventoryPosition entity.
 */
@SuppressWarnings("unused")
@Repository
@org.jmolecules.ddd.annotation.Repository
@org.jmolecules.architecture.layered.InfrastructureLayer
public interface InventoryPositionRepository extends JpaRepository<InventoryPosition, Long> {

    /**
     * Locks (pessimistic write) the positions of the given SKUs, ordered by SKU so that
     * concurrent multi-line reservations acquire locks in the same order (no deadlocks).
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<InventoryPosition> findAllBySkuInOrderBySku(Collection<String> skus);
}
