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
@Table(name = "stock_reservation")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@org.jmolecules.ddd.annotation.AggregateRoot
@org.jmolecules.architecture.layered.DomainLayer
public class StockReservation implements Serializable {

    @java.io.Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    @org.jmolecules.ddd.annotation.Identity
    private Long id;

    @Version
    private Integer version;

    @NotNull
    @Column(name = "reservation_id", nullable = false, unique = true)
    private String reservationId;

    /**
     * One reservation per order line: an order has as many reservations as items.
     */
    @NotNull
    @Column(name = "order_id", nullable = false)
    private String orderId;

    @NotNull
    @Column(name = "sku", nullable = false)
    private String sku;

    @NotNull
    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @NotNull
    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private ReservationStatus status = ReservationStatus.RESERVED;

    @NotNull
    @Column(name = "reserved_at", nullable = false)
    private Instant reservedAt;

    @NotNull
    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "confirmed_at")
    private Instant confirmedAt;

    @Column(name = "released_at")
    private Instant releasedAt;

    @Column(name = "release_reason")
    @Enumerated(EnumType.STRING)
    private StockReleaseReason releaseReason;

    public Long getId() {
        return id;
    }

    public StockReservation setId(Long id) {
        this.id = id;
        return this;
    }

    public Integer getVersion() {
        return version;
    }

    public StockReservation setVersion(Integer version) {
        this.version = version;
        return this;
    }

    public String getReservationId() {
        return reservationId;
    }

    public StockReservation setReservationId(String reservationId) {
        this.reservationId = reservationId;
        return this;
    }

    public String getOrderId() {
        return orderId;
    }

    public StockReservation setOrderId(String orderId) {
        this.orderId = orderId;
        return this;
    }

    public String getSku() {
        return sku;
    }

    public StockReservation setSku(String sku) {
        this.sku = sku;
        return this;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public StockReservation setQuantity(Integer quantity) {
        this.quantity = quantity;
        return this;
    }

    public ReservationStatus getStatus() {
        return status;
    }

    public StockReservation setStatus(ReservationStatus status) {
        this.status = status;
        return this;
    }

    public Instant getReservedAt() {
        return reservedAt;
    }

    public StockReservation setReservedAt(Instant reservedAt) {
        this.reservedAt = reservedAt;
        return this;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public StockReservation setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
        return this;
    }

    public Instant getConfirmedAt() {
        return confirmedAt;
    }

    public StockReservation setConfirmedAt(Instant confirmedAt) {
        this.confirmedAt = confirmedAt;
        return this;
    }

    public Instant getReleasedAt() {
        return releasedAt;
    }

    public StockReservation setReleasedAt(Instant releasedAt) {
        this.releasedAt = releasedAt;
        return this;
    }

    public StockReleaseReason getReleaseReason() {
        return releaseReason;
    }

    public StockReservation setReleaseReason(StockReleaseReason releaseReason) {
        this.releaseReason = releaseReason;
        return this;
    }

    /* https://vladmihalcea.com/the-best-way-to-implement-equals-hashcode-and-tostring-with-jpa-and-hibernate/ */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof StockReservation)) {
            return false;
        }
        StockReservation other = (StockReservation) o;
        return getId() != null && getId().equals(other.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
