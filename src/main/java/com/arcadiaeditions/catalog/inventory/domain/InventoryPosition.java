package com.arcadiaeditions.catalog.inventory.domain;

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
@Table(name = "inventory_position")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@org.jmolecules.ddd.annotation.AggregateRoot
@org.jmolecules.architecture.layered.DomainLayer
public class InventoryPosition implements Serializable {

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
    @Column(name = "available_quantity", nullable = false)
    private Integer availableQuantity;

    @NotNull
    @Column(name = "reserved_quantity", nullable = false)
    private Integer reservedQuantity;

    @NotNull
    @Column(name = "sold_quantity", nullable = false)
    private Integer soldQuantity;

    public Long getId() {
        return id;
    }

    public InventoryPosition setId(Long id) {
        this.id = id;
        return this;
    }

    public Integer getVersion() {
        return version;
    }

    public InventoryPosition setVersion(Integer version) {
        this.version = version;
        return this;
    }

    public String getSku() {
        return sku;
    }

    public InventoryPosition setSku(String sku) {
        this.sku = sku;
        return this;
    }

    public Integer getAvailableQuantity() {
        return availableQuantity;
    }

    public InventoryPosition setAvailableQuantity(Integer availableQuantity) {
        this.availableQuantity = availableQuantity;
        return this;
    }

    public Integer getReservedQuantity() {
        return reservedQuantity;
    }

    public InventoryPosition setReservedQuantity(Integer reservedQuantity) {
        this.reservedQuantity = reservedQuantity;
        return this;
    }

    public Integer getSoldQuantity() {
        return soldQuantity;
    }

    public InventoryPosition setSoldQuantity(Integer soldQuantity) {
        this.soldQuantity = soldQuantity;
        return this;
    }

    /* https://vladmihalcea.com/the-best-way-to-implement-equals-hashcode-and-tostring-with-jpa-and-hibernate/ */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof InventoryPosition)) {
            return false;
        }
        InventoryPosition other = (InventoryPosition) o;
        return getId() != null && getId().equals(other.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
