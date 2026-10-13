package com.arcadiaeditions.catalog.products.dtos;

import com.arcadiaeditions.catalog.products.domain.*;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.math.*;
import java.time.*;
import java.util.*;

/**
 * CreateProductInput.
 */
@org.jmolecules.ddd.annotation.ValueObject
public class CreateProductInput implements Serializable {

    @NotNull
    private String sku;

    @NotNull
    private String name;

    private String description;

    @NotNull
    private BigDecimal price;

    @NotNull
    private String currency;

    @NotNull
    private Integer editionSize;

    @NotNull
    private InventoryTrackingMode trackingMode;

    private Instant launchAt;

    public String getSku() {
        return sku;
    }

    public CreateProductInput setSku(String sku) {
        this.sku = sku;
        return this;
    }

    public String getName() {
        return name;
    }

    public CreateProductInput setName(String name) {
        this.name = name;
        return this;
    }

    public String getDescription() {
        return description;
    }

    public CreateProductInput setDescription(String description) {
        this.description = description;
        return this;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public CreateProductInput setPrice(BigDecimal price) {
        this.price = price;
        return this;
    }

    public String getCurrency() {
        return currency;
    }

    public CreateProductInput setCurrency(String currency) {
        this.currency = currency;
        return this;
    }

    public Integer getEditionSize() {
        return editionSize;
    }

    public CreateProductInput setEditionSize(Integer editionSize) {
        this.editionSize = editionSize;
        return this;
    }

    public InventoryTrackingMode getTrackingMode() {
        return trackingMode;
    }

    public CreateProductInput setTrackingMode(InventoryTrackingMode trackingMode) {
        this.trackingMode = trackingMode;
        return this;
    }

    public Instant getLaunchAt() {
        return launchAt;
    }

    public CreateProductInput setLaunchAt(Instant launchAt) {
        this.launchAt = launchAt;
        return this;
    }
}
