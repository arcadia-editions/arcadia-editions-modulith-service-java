package com.arcadiaeditions.catalog.inventory.repository.jpa.inmemory;

import com.arcadiaeditions.catalog.inventory.domain.*;
import com.arcadiaeditions.catalog.inventory.repository.jpa.InventoryPositionRepository;
import java.math.*;
import java.time.*;
import java.util.*;

public class InventoryPositionRepositoryInMemory extends InMemoryJpaRepository<InventoryPosition>
        implements InventoryPositionRepository {

    @Override
    public List<InventoryPosition> findAllBySkuInOrderBySku(Collection<String> skus) {
        return getEntities().values().stream()
                .filter(position -> skus.contains(position.getSku()))
                .sorted(Comparator.comparing(InventoryPosition::getSku))
                .toList();
    }
}
