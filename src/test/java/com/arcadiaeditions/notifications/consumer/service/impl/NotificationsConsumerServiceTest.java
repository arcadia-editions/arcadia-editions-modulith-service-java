package com.arcadiaeditions.notifications.consumer.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import com.arcadiaeditions.notifications.consumer.config.*;
import com.arcadiaeditions.notifications.consumer.domain.*;
import com.arcadiaeditions.notifications.consumer.domain.events.*;
import com.arcadiaeditions.notifications.consumer.events.InMemoryEventPublisher;
import com.arcadiaeditions.notifications.consumer.repository.jpa.*;
import com.arcadiaeditions.notifications.consumer.repository.jpa.inmemory.*;
import com.arcadiaeditions.notifications.consumer.service.*;
import com.arcadiaeditions.notifications.consumer.service.dtos.*;
import com.arcadiaeditions.notifications.consumer.service.impl.mappers.*;
import java.time.*;
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

    static final Instant NOW = Instant.parse("2026-10-14T10:00:00Z");

    @BeforeEach
    void setUp() {
        context.reloadTestData();
    }

    @Test
    void sendOrderConfirmationTest() {
        var input = new SendOrderConfirmationInput()
                .setOrderId("ORD-100")
                .setCustomerId("CUST-100")
                .setCustomerEmail("customer100@example.com");

        var result = notificationsConsumerService.sendOrderConfirmation(input);

        assertThat(result.getType()).isEqualTo(NotificationType.ORDER_CONFIRMATION);
        assertThat(result.getChannel()).isEqualTo(NotificationChannel.EMAIL);
        assertThat(result.getRecipient()).isEqualTo("customer100@example.com");
        assertThat(result.getNotificationId()).startsWith("NOT-");
        assertThat(result.getCreatedAt()).isEqualTo(NOW);
        assertThat(result.getStatus()).isEqualTo(NotificationStatus.SENT);
        assertThat(result.getSentAt()).isEqualTo(NOW);
        assertThat(result.getFailedAt()).isNull();

        var saved = notificationRepository
                .findByOrderIdAndType("ORD-100", NotificationType.ORDER_CONFIRMATION)
                .orElseThrow();
        assertThat(saved.getStatus()).isEqualTo(NotificationStatus.SENT);

        assertThat(eventPublisher.getEvents()).hasSize(1);
        var event = (OrderConfirmationSent) eventPublisher.getEvents().get(0);
        assertThat(event.getNotificationId()).isEqualTo(result.getNotificationId());
        assertThat(event.getOrderId()).isEqualTo("ORD-100");
        assertThat(event.getRecipient()).isEqualTo("customer100@example.com");
        assertThat(event.getSentAt()).isEqualTo(NOW);
    }

    @Test
    void sendOrderConfirmationDeliveryFailureTest() {
        var input = new SendOrderConfirmationInput()
                .setOrderId("ORD-101")
                .setCustomerId("CUST-101")
                .setCustomerEmail("x@fail.invalid");

        var result = notificationsConsumerService.sendOrderConfirmation(input);

        assertThat(result.getStatus()).isEqualTo(NotificationStatus.FAILED);
        assertThat(result.getFailedAt()).isEqualTo(NOW);
        assertThat(result.getFailureReason()).isNotBlank();
        assertThat(result.getSentAt()).isNull();
        assertThat(notificationRepository
                        .findByOrderIdAndType("ORD-101", NotificationType.ORDER_CONFIRMATION)
                        .orElseThrow()
                        .getStatus())
                .isEqualTo(NotificationStatus.FAILED);

        assertThat(eventPublisher.getEvents()).hasSize(1);
        var event = (NotificationDeliveryFailed) eventPublisher.getEvents().get(0);
        assertThat(event.getReason()).isEqualTo(result.getFailureReason());
        assertThat(event.getType()).isEqualTo(NotificationType.ORDER_CONFIRMATION);
        assertThat(event.getOrderId()).isEqualTo("ORD-101");
        assertThat(event.getFailedAt()).isEqualTo(NOW);
    }

    @Test
    void sendOrderConfirmationRedeliveryWhenSentIsNoOpTest() {
        var before = notificationRepository.findAll().size();
        var input = new SendOrderConfirmationInput()
                .setOrderId("ORD-001")
                .setCustomerId("CUST-001")
                .setCustomerEmail("customer@example.com");

        var result = notificationsConsumerService.sendOrderConfirmation(input);

        assertThat(result.getNotificationId()).isEqualTo("NOT-001");
        assertThat(result.getStatus()).isEqualTo(NotificationStatus.SENT);
        assertThat(notificationRepository.findAll()).hasSize(before);
        assertThat(eventPublisher.getEvents()).isEmpty();
    }

    @Test
    void sendOrderConfirmationRedeliveryWhenFailedIsNoOpTest() {
        notificationsConsumerService.sendOrderConfirmation(new SendOrderConfirmationInput()
                .setOrderId("ORD-102")
                .setCustomerId("CUST-102")
                .setCustomerEmail("x@fail.invalid"));
        eventPublisher.getEvents().clear();
        var before = notificationRepository.findAll().size();

        var result = notificationsConsumerService.sendOrderConfirmation(new SendOrderConfirmationInput()
                .setOrderId("ORD-102")
                .setCustomerId("CUST-102")
                .setCustomerEmail("customer102@example.com"));

        assertThat(result.getStatus()).isEqualTo(NotificationStatus.FAILED);
        assertThat(notificationRepository.findAll()).hasSize(before);
        assertThat(eventPublisher.getEvents()).isEmpty();
    }

    @Test
    void sendStockUnavailableNotificationTest() {
        var input = new SendStockUnavailableNotificationInput()
                .setOrderId("ORD-110")
                .setCustomerId("CUST-110")
                .setCustomerEmail("customer110@example.com")
                .addUnavailableItems(new StockUnavailableItemInput().setSku("SKU-A"))
                .addUnavailableItems(new StockUnavailableItemInput().setSku("SKU-B"));

        var result = notificationsConsumerService.sendStockUnavailableNotification(input);

        assertThat(result.getType()).isEqualTo(NotificationType.STOCK_UNAVAILABLE);
        assertThat(result.getChannel()).isEqualTo(NotificationChannel.EMAIL);
        assertThat(result.getRecipient()).isEqualTo("customer110@example.com");
        assertThat(result.getSkus()).containsExactly("SKU-A", "SKU-B");
        assertThat(result.getNotificationId()).startsWith("NOT-");
        assertThat(result.getStatus()).isEqualTo(NotificationStatus.SENT);
        assertThat(result.getSentAt()).isEqualTo(NOW);

        assertThat(notificationRepository
                        .findByOrderIdAndType("ORD-110", NotificationType.STOCK_UNAVAILABLE)
                        .orElseThrow()
                        .getStatus())
                .isEqualTo(NotificationStatus.SENT);

        assertThat(eventPublisher.getEvents()).hasSize(1);
        var event =
                (StockUnavailableNotificationSent) eventPublisher.getEvents().get(0);
        assertThat(event.getNotificationId()).isEqualTo(result.getNotificationId());
        assertThat(event.getOrderId()).isEqualTo("ORD-110");
        assertThat(event.getSkus()).containsExactly("SKU-A", "SKU-B");
        assertThat(event.getSentAt()).isEqualTo(NOW);
    }

    @Test
    void sendStockUnavailableNotificationDeliveryFailureTest() {
        var result = notificationsConsumerService.sendStockUnavailableNotification(
                new SendStockUnavailableNotificationInput()
                        .setOrderId("ORD-111")
                        .setCustomerId("CUST-111")
                        .setCustomerEmail("x@fail.invalid")
                        .addUnavailableItems(new StockUnavailableItemInput().setSku("SKU-A")));

        assertThat(result.getStatus()).isEqualTo(NotificationStatus.FAILED);
        assertThat(eventPublisher.getEvents()).hasSize(1);
        var event = (NotificationDeliveryFailed) eventPublisher.getEvents().get(0);
        assertThat(event.getType()).isEqualTo(NotificationType.STOCK_UNAVAILABLE);
        assertThat(event.getOrderId()).isEqualTo("ORD-111");
    }

    @Test
    void sendStockUnavailableNotificationRedeliveryIsNoOpTest() {
        var before = notificationRepository.findAll().size();

        var result = notificationsConsumerService.sendStockUnavailableNotification(
                new SendStockUnavailableNotificationInput()
                        .setOrderId("ORD-020")
                        .setCustomerId("CUST-020")
                        .setCustomerEmail("customer20@example.com")
                        .addUnavailableItems(new StockUnavailableItemInput().setSku("SKU-004")));

        assertThat(result.getStatus()).isEqualTo(NotificationStatus.SENT);
        assertThat(result.getType()).isEqualTo(NotificationType.STOCK_UNAVAILABLE);
        assertThat(notificationRepository.findAll()).hasSize(before);
        assertThat(eventPublisher.getEvents()).isEmpty();
    }

    @Test
    void sendOrderCancelledNotificationTest() {
        var input = new SendOrderCancelledNotificationInput()
                .setOrderId("ORD-120")
                .setCustomerId("CUST-120")
                .setCustomerEmail("customer120@example.com")
                .setReason("PAYMENT_DECLINED");

        var result = notificationsConsumerService.sendOrderCancelledNotification(input);

        assertThat(result.getType()).isEqualTo(NotificationType.ORDER_CANCELLED);
        assertThat(result.getChannel()).isEqualTo(NotificationChannel.EMAIL);
        assertThat(result.getRecipient()).isEqualTo("customer120@example.com");
        assertThat(result.getBody()).contains("PAYMENT_DECLINED");
        assertThat(result.getNotificationId()).startsWith("NOT-");
        assertThat(result.getStatus()).isEqualTo(NotificationStatus.SENT);
        assertThat(result.getSentAt()).isEqualTo(NOW);

        assertThat(notificationRepository
                        .findByOrderIdAndType("ORD-120", NotificationType.ORDER_CANCELLED)
                        .orElseThrow()
                        .getStatus())
                .isEqualTo(NotificationStatus.SENT);

        assertThat(eventPublisher.getEvents()).hasSize(1);
        var event = (OrderCancelledNotificationSent) eventPublisher.getEvents().get(0);
        assertThat(event.getNotificationId()).isEqualTo(result.getNotificationId());
        assertThat(event.getOrderId()).isEqualTo("ORD-120");
        assertThat(event.getReason()).isEqualTo("PAYMENT_DECLINED");
        assertThat(event.getRecipient()).isEqualTo("customer120@example.com");
        assertThat(event.getSentAt()).isEqualTo(NOW);
    }

    @Test
    void sendOrderCancelledNotificationDeliveryFailureTest() {
        var result =
                notificationsConsumerService.sendOrderCancelledNotification(new SendOrderCancelledNotificationInput()
                        .setOrderId("ORD-121")
                        .setCustomerId("CUST-121")
                        .setCustomerEmail("x@fail.invalid")
                        .setReason("PAYMENT_DECLINED"));

        assertThat(result.getStatus()).isEqualTo(NotificationStatus.FAILED);
        assertThat(eventPublisher.getEvents()).hasSize(1);
        var event = (NotificationDeliveryFailed) eventPublisher.getEvents().get(0);
        assertThat(event.getType()).isEqualTo(NotificationType.ORDER_CANCELLED);
        assertThat(event.getOrderId()).isEqualTo("ORD-121");
    }

    @Test
    void sendOrderCancelledNotificationRedeliveryIsNoOpTest() {
        var before = notificationRepository.findAll().size();

        var result =
                notificationsConsumerService.sendOrderCancelledNotification(new SendOrderCancelledNotificationInput()
                        .setOrderId("ORD-013")
                        .setCustomerId("CUST-013")
                        .setCustomerEmail("customer13@example.com")
                        .setReason("PAYMENT_DECLINED"));

        assertThat(result.getStatus()).isEqualTo(NotificationStatus.SENT);
        assertThat(result.getType()).isEqualTo(NotificationType.ORDER_CANCELLED);
        assertThat(notificationRepository.findAll()).hasSize(before);
        assertThat(eventPublisher.getEvents()).isEmpty();
    }
}
