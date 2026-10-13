package com.arcadiaeditions.catalog.products.domain.events;

import com.arcadiaeditions.catalog.products.domain.*;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.math.*;
import java.time.*;
import java.util.*;

/**
 *
 */
@org.springframework.modulith.NamedInterface("events.ProductPublished")
@org.jmolecules.event.annotation.DomainEvent
public class ProductPublished implements Serializable {

    @java.io.Serial
    private static final long serialVersionUID = 1L;

    private String productId;

    private String sku;

    private String name;

    private BigDecimal price;

    private String currency;

    private Integer editionSize;

    private InventoryTrackingMode trackingMode;

    private Instant launchAt;

    private Instant publishedAt;

    public String getProductId() {
        return productId;
    }

    public ProductPublished setProductId(String productId) {
        this.productId = productId;
        return this;
    }

    public String getSku() {
        return sku;
    }

    public ProductPublished setSku(String sku) {
        this.sku = sku;
        return this;
    }

    public String getName() {
        return name;
    }

    public ProductPublished setName(String name) {
        this.name = name;
        return this;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public ProductPublished setPrice(BigDecimal price) {
        this.price = price;
        return this;
    }

    public String getCurrency() {
        return currency;
    }

    public ProductPublished setCurrency(String currency) {
        this.currency = currency;
        return this;
    }

    public Integer getEditionSize() {
        return editionSize;
    }

    public ProductPublished setEditionSize(Integer editionSize) {
        this.editionSize = editionSize;
        return this;
    }

    public InventoryTrackingMode getTrackingMode() {
        return trackingMode;
    }

    public ProductPublished setTrackingMode(InventoryTrackingMode trackingMode) {
        this.trackingMode = trackingMode;
        return this;
    }

    public Instant getLaunchAt() {
        return launchAt;
    }

    public ProductPublished setLaunchAt(Instant launchAt) {
        this.launchAt = launchAt;
        return this;
    }

    public Instant getPublishedAt() {
        return publishedAt;
    }

    public ProductPublished setPublishedAt(Instant publishedAt) {
        this.publishedAt = publishedAt;
        return this;
    }
}
