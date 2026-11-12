package com.arcadiaeditions.notifications.consumer.domain;

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
@Table(name = "notification")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@org.jmolecules.ddd.annotation.AggregateRoot
@org.jmolecules.architecture.layered.DomainLayer
public class Notification implements Serializable {

    @java.io.Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    @org.jmolecules.ddd.annotation.Identity
    private Long id;

    @Version
    private Integer version;

    @NotNull
    @Column(name = "notification_id", nullable = false, unique = true)
    private String notificationId;

    @NotNull
    @Column(name = "customer_id", nullable = false)
    private String customerId;

    @Column(name = "order_id")
    private String orderId;

    @Column(name = "skus")
    private List<String> skus = new ArrayList<>();

    @NotNull
    @Column(name = "type", nullable = false)
    @Enumerated(EnumType.STRING)
    private NotificationType type;

    @NotNull
    @Column(name = "channel", nullable = false)
    @Enumerated(EnumType.STRING)
    private NotificationChannel channel;

    @NotNull
    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private NotificationStatus status = NotificationStatus.PENDING;

    @NotNull
    @Column(name = "subject", nullable = false)
    private String subject;

    @NotNull
    @Column(name = "body", nullable = false)
    private String body;

    @NotNull
    @Column(name = "recipient", nullable = false)
    private String recipient;

    @NotNull
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "sent_at")
    private Instant sentAt;

    @Column(name = "failed_at")
    private Instant failedAt;

    @Column(name = "failure_reason")
    private String failureReason;

    public Long getId() {
        return id;
    }

    public Notification setId(Long id) {
        this.id = id;
        return this;
    }

    public Integer getVersion() {
        return version;
    }

    public Notification setVersion(Integer version) {
        this.version = version;
        return this;
    }

    public String getNotificationId() {
        return notificationId;
    }

    public Notification setNotificationId(String notificationId) {
        this.notificationId = notificationId;
        return this;
    }

    public String getCustomerId() {
        return customerId;
    }

    public Notification setCustomerId(String customerId) {
        this.customerId = customerId;
        return this;
    }

    public String getOrderId() {
        return orderId;
    }

    public Notification setOrderId(String orderId) {
        this.orderId = orderId;
        return this;
    }

    public List<String> getSkus() {
        return skus;
    }

    public Notification setSkus(List<String> skus) {
        this.skus = skus;
        return this;
    }

    public NotificationType getType() {
        return type;
    }

    public Notification setType(NotificationType type) {
        this.type = type;
        return this;
    }

    public NotificationChannel getChannel() {
        return channel;
    }

    public Notification setChannel(NotificationChannel channel) {
        this.channel = channel;
        return this;
    }

    public NotificationStatus getStatus() {
        return status;
    }

    public Notification setStatus(NotificationStatus status) {
        this.status = status;
        return this;
    }

    public String getSubject() {
        return subject;
    }

    public Notification setSubject(String subject) {
        this.subject = subject;
        return this;
    }

    public String getBody() {
        return body;
    }

    public Notification setBody(String body) {
        this.body = body;
        return this;
    }

    public String getRecipient() {
        return recipient;
    }

    public Notification setRecipient(String recipient) {
        this.recipient = recipient;
        return this;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Notification setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
        return this;
    }

    public Instant getSentAt() {
        return sentAt;
    }

    public Notification setSentAt(Instant sentAt) {
        this.sentAt = sentAt;
        return this;
    }

    public Instant getFailedAt() {
        return failedAt;
    }

    public Notification setFailedAt(Instant failedAt) {
        this.failedAt = failedAt;
        return this;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public Notification setFailureReason(String failureReason) {
        this.failureReason = failureReason;
        return this;
    }

    /* https://vladmihalcea.com/the-best-way-to-implement-equals-hashcode-and-tostring-with-jpa-and-hibernate/ */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Notification)) {
            return false;
        }
        Notification other = (Notification) o;
        return getId() != null && getId().equals(other.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
