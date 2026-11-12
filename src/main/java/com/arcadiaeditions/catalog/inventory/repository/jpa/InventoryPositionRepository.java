package com.arcadiaeditions.catalog.inventory.repository.jpa;

import com.arcadiaeditions.catalog.inventory.domain.*;
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

    /** Loads the position of the sku under a write lock (reserveStock moves available to reserved). */
    @Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from InventoryPosition p where p.sku = :sku")
    Optional<InventoryPosition> findBySkuForUpdate(@org.springframework.data.repository.query.Param("sku") String sku);
}
