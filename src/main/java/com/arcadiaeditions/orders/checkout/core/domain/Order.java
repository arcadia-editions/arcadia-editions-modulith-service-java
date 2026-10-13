package com.arcadiaeditions.orders.checkout.core.domain;

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
@Table(name = "checkout_order")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@org.jmolecules.ddd.annotation.AggregateRoot
public class Order implements Serializable {

    @java.io.Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    @org.jmolecules.ddd.annotation.Identity
    private Long id;

    @Version
    private Integer version;

    @NotNull
    @Column(name = "order_id", nullable = false, unique = true)
    private String orderId;

    @NotNull
    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private OrderStatus status = OrderStatus.CREATED;

    /**
     * Customer snapshot taken at checkout (there is no customers bounded context yet).
     */
    @NotNull
    @Column(name = "customer_id", nullable = false)
    private String customerId;

    @NotNull
    @Size(max = 254)
    @Column(name = "customer_email", nullable = false, length = 254)
    private String customerEmail;

    @NotNull
    @Column(name = "payment_method_id", nullable = false)
    private String paymentMethodId;

    @NotNull
    @Size(min = 1)
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
    @Column(name = "items")
    private List<OrderItem> items = new ArrayList<>();

    @NotNull
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
    @Column(name = "shipping_address")
    private ShippingAddress shippingAddress;

    @NotNull
    @Column(name = "total_amount", nullable = false)
    private BigDecimal totalAmount;

    @NotNull
    @Size(max = 3)
    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "confirmed_at")
    private Instant confirmedAt;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Column(name = "cancellation_reason")
    private String cancellationReason;

    public Long getId() {
        return id;
    }

    public Order setId(Long id) {
        this.id = id;
        return this;
    }

    public Integer getVersion() {
        return version;
    }

    public Order setVersion(Integer version) {
        this.version = version;
        return this;
    }

    public String getOrderId() {
        return orderId;
    }

    public Order setOrderId(String orderId) {
        this.orderId = orderId;
        return this;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public Order setStatus(OrderStatus status) {
        this.status = status;
        return this;
    }

    public String getCustomerId() {
        return customerId;
    }

    public Order setCustomerId(String customerId) {
        this.customerId = customerId;
        return this;
    }

    public String getCustomerEmail() {
        return customerEmail;
    }

    public Order setCustomerEmail(String customerEmail) {
        this.customerEmail = customerEmail;
        return this;
    }

    public String getPaymentMethodId() {
        return paymentMethodId;
    }

    public Order setPaymentMethodId(String paymentMethodId) {
        this.paymentMethodId = paymentMethodId;
        return this;
    }

    public List<OrderItem> getItems() {
        return items;
    }

    public Order setItems(List<OrderItem> items) {
        this.items = items;
        return this;
    }

    public ShippingAddress getShippingAddress() {
        return shippingAddress;
    }

    public Order setShippingAddress(ShippingAddress shippingAddress) {
        this.shippingAddress = shippingAddress;
        return this;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public Order setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
        return this;
    }

    public String getCurrency() {
        return currency;
    }

    public Order setCurrency(String currency) {
        this.currency = currency;
        return this;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Order setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
        return this;
    }

    public Instant getConfirmedAt() {
        return confirmedAt;
    }

    public Order setConfirmedAt(Instant confirmedAt) {
        this.confirmedAt = confirmedAt;
        return this;
    }

    public Instant getCancelledAt() {
        return cancelledAt;
    }

    public Order setCancelledAt(Instant cancelledAt) {
        this.cancelledAt = cancelledAt;
        return this;
    }

    public String getCancellationReason() {
        return cancellationReason;
    }

    public Order setCancellationReason(String cancellationReason) {
        this.cancellationReason = cancellationReason;
        return this;
    }

    /* https://vladmihalcea.com/the-best-way-to-implement-equals-hashcode-and-tostring-with-jpa-and-hibernate/ */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Order)) {
            return false;
        }
        Order other = (Order) o;
        return getId() != null && getId().equals(other.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
