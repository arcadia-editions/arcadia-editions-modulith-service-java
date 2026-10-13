package com.arcadiaeditions.catalog.products.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.math.*;
import java.time.*;
import java.util.*;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

/**
 *
 */
@Entity
@Table(name = "product")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@org.jmolecules.ddd.annotation.AggregateRoot
public class Product implements Serializable {

    @java.io.Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    @org.jmolecules.ddd.annotation.Identity
    private Long id;

    @Version
    private Integer version;

    @NotNull
    @Column(name = "sku", nullable = false, unique = true)
    private String sku;

    @NotNull
    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description")
    private String description;

    @NotNull
    @Column(name = "price", nullable = false)
    private BigDecimal price;

    @NotNull
    @Column(name = "currency", nullable = false)
    private String currency;

    @NotNull
    @Column(name = "edition_size", nullable = false)
    private Integer editionSize;

    @NotNull
    @Column(name = "tracking_mode", nullable = false)
    @Enumerated(EnumType.STRING)
    private InventoryTrackingMode trackingMode;

    @Column(name = "launch_at")
    private Instant launchAt;

    @NotNull
    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private ProductStatus status = ProductStatus.DRAFT;

    public Long getId() {
        return id;
    }

    public Product setId(Long id) {
        this.id = id;
        return this;
    }

    public Integer getVersion() {
        return version;
    }

    public Product setVersion(Integer version) {
        this.version = version;
        return this;
    }

    public String getSku() {
        return sku;
    }

    public Product setSku(String sku) {
        this.sku = sku;
        return this;
    }

    public String getName() {
        return name;
    }

    public Product setName(String name) {
        this.name = name;
        return this;
    }

    public String getDescription() {
        return description;
    }

    public Product setDescription(String description) {
        this.description = description;
        return this;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public Product setPrice(BigDecimal price) {
        this.price = price;
        return this;
    }

    public String getCurrency() {
        return currency;
    }

    public Product setCurrency(String currency) {
        this.currency = currency;
        return this;
    }

    public Integer getEditionSize() {
        return editionSize;
    }

    public Product setEditionSize(Integer editionSize) {
        this.editionSize = editionSize;
        return this;
    }

    public InventoryTrackingMode getTrackingMode() {
        return trackingMode;
    }

    public Product setTrackingMode(InventoryTrackingMode trackingMode) {
        this.trackingMode = trackingMode;
        return this;
    }

    public Instant getLaunchAt() {
        return launchAt;
    }

    public Product setLaunchAt(Instant launchAt) {
        this.launchAt = launchAt;
        return this;
    }

    public ProductStatus getStatus() {
        return status;
    }

    public Product setStatus(ProductStatus status) {
        this.status = status;
        return this;
    }

    /* https://vladmihalcea.com/the-best-way-to-implement-equals-hashcode-and-tostring-with-jpa-and-hibernate/ */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Product)) {
            return false;
        }
        Product other = (Product) o;
        return getId() != null && getId().equals(other.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
