package com.arcadiaeditions.catalog.products.dtos;

import com.arcadiaeditions.catalog.products.domain.*;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.math.*;
import java.time.*;
import java.util.*;

/**
 * ProductPublishedResponse.
 */
@org.jmolecules.ddd.annotation.ValueObject
public class ProductPublishedResponse implements Serializable {

    private String productId;

    private String sku;

    private Instant publishedAt;

    public String getProductId() {
        return productId;
    }

    public ProductPublishedResponse setProductId(String productId) {
        this.productId = productId;
        return this;
    }

    public String getSku() {
        return sku;
    }

    public ProductPublishedResponse setSku(String sku) {
        this.sku = sku;
        return this;
    }

    public Instant getPublishedAt() {
        return publishedAt;
    }

    public ProductPublishedResponse setPublishedAt(Instant publishedAt) {
        this.publishedAt = publishedAt;
        return this;
    }
}
