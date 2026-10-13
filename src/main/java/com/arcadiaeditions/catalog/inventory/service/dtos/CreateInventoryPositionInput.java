package com.arcadiaeditions.catalog.inventory.service.dtos;

import com.arcadiaeditions.catalog.inventory.domain.*;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.math.*;
import java.time.*;
import java.util.*;

/**
 * CreateInventoryPositionInput.
 */
@org.jmolecules.ddd.annotation.ValueObject
public class CreateInventoryPositionInput implements Serializable {

    @NotNull
    private String sku;

    @NotNull
    private Integer initialQuantity;

    public String getSku() {
        return sku;
    }

    public CreateInventoryPositionInput setSku(String sku) {
        this.sku = sku;
        return this;
    }

    public Integer getInitialQuantity() {
        return initialQuantity;
    }

    public CreateInventoryPositionInput setInitialQuantity(Integer initialQuantity) {
        this.initialQuantity = initialQuantity;
        return this;
    }
}
