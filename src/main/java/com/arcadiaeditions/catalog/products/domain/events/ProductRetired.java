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
@org.springframework.modulith.NamedInterface("events.ProductRetired")
@org.jmolecules.event.annotation.DomainEvent
public class ProductRetired implements Serializable {

    @java.io.Serial
    private static final long serialVersionUID = 1L;

    private String productId;

    private String sku;

    private Instant retiredAt;

    public String getProductId() {
        return productId;
    }

    public ProductRetired setProductId(String productId) {
        this.productId = productId;
        return this;
    }

    public String getSku() {
        return sku;
    }

    public ProductRetired setSku(String sku) {
        this.sku = sku;
        return this;
    }

    public Instant getRetiredAt() {
        return retiredAt;
    }

    public ProductRetired setRetiredAt(Instant retiredAt) {
        this.retiredAt = retiredAt;
        return this;
    }
}
