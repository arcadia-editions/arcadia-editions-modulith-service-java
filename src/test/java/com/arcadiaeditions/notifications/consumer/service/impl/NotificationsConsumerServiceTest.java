package com.arcadiaeditions.notifications.consumer.service.impl;

import static org.assertj.core.api.Assertions.assertThat;

import com.arcadiaeditions.config.TestClock;
import com.arcadiaeditions.notifications.consumer.config.*;
import com.arcadiaeditions.notifications.consumer.domain.*;
import com.arcadiaeditions.notifications.consumer.domain.events.*;
import com.arcadiaeditions.notifications.consumer.events.InMemoryEventPublisher;
import com.arcadiaeditions.notifications.consumer.repository.jpa.inmemory.*;
import com.arcadiaeditions.notifications.consumer.service.dtos.*;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Acceptance Test for NotificationsConsumerService.
 */
class NotificationsConsumerServiceTest {

    private final Logger log = LoggerFactory.getLogger(getClass());

    ServicesInMemoryConfig context = new ServicesInMemoryConfig();
    NotificationsConsumerServiceImpl notificationsConsumerService = context.notificationsConsumerService();

    NotificationRepositoryInMemory notificationRepository = context.notificationRepository();

    InMemoryEventPublisher eventPublisher = context.eventPublisher();

    @BeforeEach
    void setUp() {
        context.reloadTestData();
    }

    private List<Notification> notificationsOf(String orderId, NotificationType type) {
        return notificationRepository.findAll().stream()
                .filter(n -> orderId.equals(n.getOrderId()) && type == n.getType())
                .toList();
    }

    /** PaymentCaptured for an order with no notification yet (ORD-006: first delivery) -> SENT, OrderConfirmationSent. */
    @Test
    void sendOrderConfirmationTest() {
        var input = new SendOrderConfirmationInput()
                .setOrderId("ORD-006")
                .setCustomerId("CUST-006")
                .setCustomerEmail("customer006@example.com");

        var notification = notificationsConsumerService.sendOrderConfirmation(input);

        assertThat(notification.getId()).isNotNull();
        assertThat(notification.getNotificationId()).startsWith("NOT-");
        assertThat(notification.getOrderId()).isEqualTo("ORD-006");
        assertThat(notification.getCustomerId()).isEqualTo("CUST-006");
        assertThat(notification.getType()).isEqualTo(NotificationType.ORDER_CONFIRMATION);
        assertThat(notification.getChannel()).isEqualTo(NotificationChannel.EMAIL);
        assertThat(notification.getRecipient()).isEqualTo("customer006@example.com");
        assertThat(notification.getSubject()).isEqualTo("Order confirmed");
        assertThat(notification.getBody()).isEqualTo("Your order ORD-006 is confirmed.");
        assertThat(notification.getStatus()).isEqualTo(NotificationStatus.SENT);
        assertThat(notification.getCreatedAt()).isEqualTo(TestClock.NOW);
        assertThat(notification.getSentAt()).isEqualTo(TestClock.NOW);
        assertThat(notification.getFailedAt()).isNull();
        assertThat(notification.getFailureReason()).isNull();
        assertThat(notificationsOf("ORD-006", NotificationType.ORDER_CONFIRMATION))
                .hasSize(1);

        var events = eventPublisher.getEvents();
        assertThat(events).hasSize(1);
        assertThat(events.get(0)).isInstanceOf(OrderConfirmationSent.class);
        var event = (OrderConfirmationSent) events.get(0);
        assertThat(event.getNotificationId()).isEqualTo(notification.getNotificationId());
        assertThat(event.getOrderId()).isEqualTo("ORD-006");
        assertThat(event.getCustomerId()).isEqualTo("CUST-006");
        assertThat(event.getChannel()).isEqualTo(NotificationChannel.EMAIL);
        assertThat(event.getRecipient()).isEqualTo("customer006@example.com");
        assertThat(event.getSentAt()).isEqualTo(TestClock.NOW);
    }

    /** Redelivery of PaymentCaptured for ORD-001 (ORDER_CONFIRMATION already SENT, NOT-001): no change, no event. */
    @Test
    void sendOrderConfirmationRedeliveryTest() {
        var input = new SendOrderConfirmationInput()
                .setOrderId("ORD-001")
                .setCustomerId("CUST-001")
                .setCustomerEmail("customer@example.com");

        var notification = notificationsConsumerService.sendOrderConfirmation(input);

        assertThat(notification.getNotificationId()).isEqualTo("NOT-001");
        assertThat(notification.getStatus()).isEqualTo(NotificationStatus.SENT);
        assertThat(notification.getSentAt()).isEqualTo(java.time.Instant.parse("2026-09-26T13:07:00Z"));
        assertThat(notificationsOf("ORD-001", NotificationType.ORDER_CONFIRMATION))
                .hasSize(1);
        assertThat(eventPublisher.getEvents()).isEmpty();
    }

    /** The same input twice: one Notification, one OrderConfirmationSent. */
    @Test
    void sendOrderConfirmationTwiceTest() {
        var input = new SendOrderConfirmationInput()
                .setOrderId("ORD-006")
                .setCustomerId("CUST-006")
                .setCustomerEmail("customer006@example.com");

        var first = notificationsConsumerService.sendOrderConfirmation(input);
        var second = notificationsConsumerService.sendOrderConfirmation(input);

        assertThat(second.getNotificationId()).isEqualTo(first.getNotificationId());
        assertThat(notificationsOf("ORD-006", NotificationType.ORDER_CONFIRMATION))
                .hasSize(1);
        assertThat(eventPublisher.getEvents()).hasSize(1);
    }

    /** Idempotency is per order and type: ORD-013 has an ORDER_CANCELLED notification, not a confirmation. */
    @Test
    void sendOrderConfirmationOtherTypeExistsTest() {
        var input = new SendOrderConfirmationInput()
                .setOrderId("ORD-013")
                .setCustomerId("CUST-013")
                .setCustomerEmail("customer013@example.com");

        var notification = notificationsConsumerService.sendOrderConfirmation(input);

        assertThat(notification.getNotificationId()).isNotEqualTo("NOT-002");
        assertThat(notification.getType()).isEqualTo(NotificationType.ORDER_CONFIRMATION);
        assertThat(notification.getStatus()).isEqualTo(NotificationStatus.SENT);
        assertThat(notificationsOf("ORD-013", NotificationType.ORDER_CONFIRMATION))
                .hasSize(1);
        assertThat(notificationsOf("ORD-013", NotificationType.ORDER_CANCELLED)).hasSize(1);
        assertThat(eventPublisher.getEvents()).hasSize(1);
        assertThat(eventPublisher.getEvents().get(0)).isInstanceOf(OrderConfirmationSent.class);
    }

    private SendStockUnavailableNotificationInput stockInput(String orderId, String customerId, String email) {
        return new SendStockUnavailableNotificationInput()
                .setOrderId(orderId)
                .setCustomerId(customerId)
                .setCustomerEmail(email)
                .setUnavailableItems(List.of(
                        new StockUnavailableItemInput()
                                .setSku("SKU-004")
                                .setRequestedQuantity(2)
                                .setAvailableQuantity(0),
                        new StockUnavailableItemInput()
                                .setSku("SKU-003")
                                .setRequestedQuantity(1)
                                .setAvailableQuantity(0)));
    }

    /** StockUnavailable for an order with no notification yet (ORD-006) -> STOCK_UNAVAILABLE SENT with the skus. */
    @Test
    void sendStockUnavailableNotificationTest() {
        var notification = notificationsConsumerService.sendStockUnavailableNotification(
                stockInput("ORD-006", "CUST-006", "customer006@example.com"));

        assertThat(notification.getId()).isNotNull();
        assertThat(notification.getNotificationId()).startsWith("NOT-");
        assertThat(notification.getOrderId()).isEqualTo("ORD-006");
        assertThat(notification.getCustomerId()).isEqualTo("CUST-006");
        assertThat(notification.getType()).isEqualTo(NotificationType.STOCK_UNAVAILABLE);
        assertThat(notification.getChannel()).isEqualTo(NotificationChannel.EMAIL);
        assertThat(notification.getRecipient()).isEqualTo("customer006@example.com");
        assertThat(notification.getSkus()).containsExactly("SKU-004", "SKU-003");
        assertThat(notification.getStatus()).isEqualTo(NotificationStatus.SENT);
        assertThat(notification.getCreatedAt()).isEqualTo(TestClock.NOW);
        assertThat(notification.getSentAt()).isEqualTo(TestClock.NOW);
        assertThat(notificationsOf("ORD-006", NotificationType.STOCK_UNAVAILABLE))
                .hasSize(1);

        var events = eventPublisher.getEvents();
        assertThat(events).hasSize(1);
        assertThat(events.get(0)).isInstanceOf(StockUnavailableNotificationSent.class);
        var event = (StockUnavailableNotificationSent) events.get(0);
        assertThat(event.getNotificationId()).isEqualTo(notification.getNotificationId());
        assertThat(event.getOrderId()).isEqualTo("ORD-006");
        assertThat(event.getCustomerId()).isEqualTo("CUST-006");
        assertThat(event.getSkus()).containsExactly("SKU-004", "SKU-003");
        assertThat(event.getChannel()).isEqualTo(NotificationChannel.EMAIL);
        assertThat(event.getRecipient()).isEqualTo("customer006@example.com");
        assertThat(event.getSentAt()).isEqualTo(TestClock.NOW);
    }

    /** Redelivery of StockUnavailable for ORD-020 (STOCK_UNAVAILABLE already SENT, NOT-006): no change, no event. */
    @Test
    void sendStockUnavailableNotificationRedeliveryTest() {
        var notification = notificationsConsumerService.sendStockUnavailableNotification(
                stockInput("ORD-020", "CUST-020", "customer020@example.com"));

        assertThat(notification.getNotificationId()).isEqualTo("NOT-006");
        assertThat(notification.getStatus()).isEqualTo(NotificationStatus.SENT);
        assertThat(notificationsOf("ORD-020", NotificationType.STOCK_UNAVAILABLE))
                .hasSize(1);
        assertThat(eventPublisher.getEvents()).isEmpty();
    }

    /** The same input twice: one Notification, one StockUnavailableNotificationSent. */
    @Test
    void sendStockUnavailableNotificationTwiceTest() {
        var input = stockInput("ORD-006", "CUST-006", "customer006@example.com");

        var first = notificationsConsumerService.sendStockUnavailableNotification(input);
        var second = notificationsConsumerService.sendStockUnavailableNotification(input);

        assertThat(second.getNotificationId()).isEqualTo(first.getNotificationId());
        assertThat(notificationsOf("ORD-006", NotificationType.STOCK_UNAVAILABLE))
                .hasSize(1);
        assertThat(eventPublisher.getEvents()).hasSize(1);
    }

    @Test
    void sendOrderCancelledNotificationTest() { // TODO T-21
    }
}
