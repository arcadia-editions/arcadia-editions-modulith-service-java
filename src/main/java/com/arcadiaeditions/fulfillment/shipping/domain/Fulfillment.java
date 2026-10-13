package com.arcadiaeditions.fulfillment.shipping.domain;

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
@Table(name = "fulfillment")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@org.jmolecules.ddd.annotation.AggregateRoot
public class Fulfillment implements Serializable {

    @java.io.Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    @org.jmolecules.ddd.annotation.Identity
    private Long id;

    @Version
    private Integer version;

    @NotNull
    @Column(name = "fulfillment_id", nullable = false, unique = true)
    private String fulfillmentId;

    @NotNull
    @Column(name = "order_id", nullable = false, unique = true)
    private String orderId;

    @NotNull
    @Column(name = "customer_id", nullable = false)
    private String customerId;

    @NotNull
    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private FulfillmentStatus status = FulfillmentStatus.SCHEDULED;

    @NotNull
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
    @Column(name = "shipping_address")
    private ShippingAddress shippingAddress;

    @NotNull
    @Size(min = 1)
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
    @Column(name = "items")
    private List<FulfillmentItem> items = new ArrayList<>();

    @Column(name = "carrier_code")
    private String carrierCode;

    @Column(name = "tracking_number")
    private String trackingNumber;

    @Column(name = "scheduled_at")
    private Instant scheduledAt;

    @Column(name = "ready_at")
    private Instant readyAt;

    @Column(name = "failed_at")
    private Instant failedAt;

    @Column(name = "failure_reason")
    private String failureReason;

    public Long getId() {
        return id;
    }

    public Fulfillment setId(Long id) {
        this.id = id;
        return this;
    }

    public Integer getVersion() {
        return version;
    }

    public Fulfillment setVersion(Integer version) {
        this.version = version;
        return this;
    }

    public String getFulfillmentId() {
        return fulfillmentId;
    }

    public Fulfillment setFulfillmentId(String fulfillmentId) {
        this.fulfillmentId = fulfillmentId;
        return this;
    }

    public String getOrderId() {
        return orderId;
    }

    public Fulfillment setOrderId(String orderId) {
        this.orderId = orderId;
        return this;
    }

    public String getCustomerId() {
        return customerId;
    }

    public Fulfillment setCustomerId(String customerId) {
        this.customerId = customerId;
        return this;
    }

    public FulfillmentStatus getStatus() {
        return status;
    }

    public Fulfillment setStatus(FulfillmentStatus status) {
        this.status = status;
        return this;
    }

    public ShippingAddress getShippingAddress() {
        return shippingAddress;
    }

    public Fulfillment setShippingAddress(ShippingAddress shippingAddress) {
        this.shippingAddress = shippingAddress;
        return this;
    }

    public List<FulfillmentItem> getItems() {
        return items;
    }

    public Fulfillment setItems(List<FulfillmentItem> items) {
        this.items = items;
        return this;
    }

    public String getCarrierCode() {
        return carrierCode;
    }

    public Fulfillment setCarrierCode(String carrierCode) {
        this.carrierCode = carrierCode;
        return this;
    }

    public String getTrackingNumber() {
        return trackingNumber;
    }

    public Fulfillment setTrackingNumber(String trackingNumber) {
        this.trackingNumber = trackingNumber;
        return this;
    }

    public Instant getScheduledAt() {
        return scheduledAt;
    }

    public Fulfillment setScheduledAt(Instant scheduledAt) {
        this.scheduledAt = scheduledAt;
        return this;
    }

    public Instant getReadyAt() {
        return readyAt;
    }

    public Fulfillment setReadyAt(Instant readyAt) {
        this.readyAt = readyAt;
        return this;
    }

    public Instant getFailedAt() {
        return failedAt;
    }

    public Fulfillment setFailedAt(Instant failedAt) {
        this.failedAt = failedAt;
        return this;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public Fulfillment setFailureReason(String failureReason) {
        this.failureReason = failureReason;
        return this;
    }

    /* https://vladmihalcea.com/the-best-way-to-implement-equals-hashcode-and-tostring-with-jpa-and-hibernate/ */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Fulfillment)) {
            return false;
        }
        Fulfillment other = (Fulfillment) o;
        return getId() != null && getId().equals(other.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
