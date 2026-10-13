package com.arcadiaeditions.payments.processing.domain;

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
@Table(name = "payment")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@org.jmolecules.ddd.annotation.AggregateRoot
public class Payment implements Serializable {

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
    @Column(name = "customer_id", nullable = false)
    private String customerId;

    @NotNull
    @Column(name = "payment_method_id", nullable = false)
    private String paymentMethodId;

    /**
     * Carried so PaymentCaptured can drive the order confirmation e-mail.
     */
    @NotNull
    @Column(name = "customer_email", nullable = false)
    private String customerEmail;

    @NotNull
    @Column(name = "amount", nullable = false)
    private BigDecimal amount;

    @NotNull
    @Column(name = "currency", nullable = false)
    private String currency;

    @NotNull
    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private PaymentStatus status = PaymentStatus.AUTHORIZATION_REQUESTED;

    @Column(name = "provider_reference")
    private String providerReference;

    @NotNull
    @Column(name = "attempt_count", nullable = false)
    private Integer attemptCount;

    @Column(name = "authorized_at")
    private Instant authorizedAt;

    @Column(name = "declined_at")
    private Instant declinedAt;

    @Column(name = "failed_at")
    private Instant failedAt;

    @Column(name = "captured_at")
    private Instant capturedAt;

    @Column(name = "voided_at")
    private Instant voidedAt;

    @Column(name = "failure_reason")
    private String failureReason;

    public Long getId() {
        return id;
    }

    public Payment setId(Long id) {
        this.id = id;
        return this;
    }

    public Integer getVersion() {
        return version;
    }

    public Payment setVersion(Integer version) {
        this.version = version;
        return this;
    }

    public String getOrderId() {
        return orderId;
    }

    public Payment setOrderId(String orderId) {
        this.orderId = orderId;
        return this;
    }

    public String getCustomerId() {
        return customerId;
    }

    public Payment setCustomerId(String customerId) {
        this.customerId = customerId;
        return this;
    }

    public String getPaymentMethodId() {
        return paymentMethodId;
    }

    public Payment setPaymentMethodId(String paymentMethodId) {
        this.paymentMethodId = paymentMethodId;
        return this;
    }

    public String getCustomerEmail() {
        return customerEmail;
    }

    public Payment setCustomerEmail(String customerEmail) {
        this.customerEmail = customerEmail;
        return this;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public Payment setAmount(BigDecimal amount) {
        this.amount = amount;
        return this;
    }

    public String getCurrency() {
        return currency;
    }

    public Payment setCurrency(String currency) {
        this.currency = currency;
        return this;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public Payment setStatus(PaymentStatus status) {
        this.status = status;
        return this;
    }

    public String getProviderReference() {
        return providerReference;
    }

    public Payment setProviderReference(String providerReference) {
        this.providerReference = providerReference;
        return this;
    }

    public Integer getAttemptCount() {
        return attemptCount;
    }

    public Payment setAttemptCount(Integer attemptCount) {
        this.attemptCount = attemptCount;
        return this;
    }

    public Instant getAuthorizedAt() {
        return authorizedAt;
    }

    public Payment setAuthorizedAt(Instant authorizedAt) {
        this.authorizedAt = authorizedAt;
        return this;
    }

    public Instant getDeclinedAt() {
        return declinedAt;
    }

    public Payment setDeclinedAt(Instant declinedAt) {
        this.declinedAt = declinedAt;
        return this;
    }

    public Instant getFailedAt() {
        return failedAt;
    }

    public Payment setFailedAt(Instant failedAt) {
        this.failedAt = failedAt;
        return this;
    }

    public Instant getCapturedAt() {
        return capturedAt;
    }

    public Payment setCapturedAt(Instant capturedAt) {
        this.capturedAt = capturedAt;
        return this;
    }

    public Instant getVoidedAt() {
        return voidedAt;
    }

    public Payment setVoidedAt(Instant voidedAt) {
        this.voidedAt = voidedAt;
        return this;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public Payment setFailureReason(String failureReason) {
        this.failureReason = failureReason;
        return this;
    }

    /* https://vladmihalcea.com/the-best-way-to-implement-equals-hashcode-and-tostring-with-jpa-and-hibernate/ */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Payment)) {
            return false;
        }
        Payment other = (Payment) o;
        return getId() != null && getId().equals(other.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
