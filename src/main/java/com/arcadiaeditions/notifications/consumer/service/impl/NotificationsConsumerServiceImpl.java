package com.arcadiaeditions.notifications.consumer.service.impl;

import com.arcadiaeditions.common.BusinessIds;
import com.arcadiaeditions.notifications.consumer.domain.*;
import com.arcadiaeditions.notifications.consumer.domain.events.*;
import com.arcadiaeditions.notifications.consumer.events.*;
import com.arcadiaeditions.notifications.consumer.repository.jpa.*;
import com.arcadiaeditions.notifications.consumer.service.*;
import com.arcadiaeditions.notifications.consumer.service.dtos.*;
import com.arcadiaeditions.notifications.consumer.service.impl.mappers.*;
import java.math.*;
import java.time.*;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing [Notification].
 */
@Service
@Transactional
@org.springframework.modulith.NamedInterface("NotificationsConsumerService")
@org.jspecify.annotations.NullMarked
@org.jmolecules.architecture.layered.ApplicationLayer
public class NotificationsConsumerServiceImpl implements NotificationsConsumerService {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final NotificationsConsumerServiceMapper notificationsConsumerServiceMapper =
            NotificationsConsumerServiceMapper.INSTANCE;

    private final NotificationRepository notificationRepository;

    private final EventsMapper eventsMapper = EventsMapper.INSTANCE;
    private final EventPublisher eventPublisher;

    private final NotificationSender notificationSender;

    private final Clock clock;

    /**
     * Constructor.
     */
    public NotificationsConsumerServiceImpl(
            NotificationRepository notificationRepository,
            EventPublisher eventPublisher,
            NotificationSender notificationSender,
            Clock clock) {
        this.notificationRepository = notificationRepository;
        this.eventPublisher = eventPublisher;
        this.notificationSender = notificationSender;
        this.clock = clock;
    }

    public Notification sendOrderConfirmation(SendOrderConfirmationInput input) {

        log.debug("Request sendOrderConfirmation: {}", input);

        // idempotent by orderId and type: a redelivered PaymentCaptured changes nothing and emits nothing
        var existing =
                notificationRepository.findByOrderIdAndType(input.getOrderId(), NotificationType.ORDER_CONFIRMATION);
        if (existing.isPresent()) {
            log.debug(
                    "sendOrderConfirmation: order {} already has a confirmation ({})",
                    input.getOrderId(),
                    existing.get().getStatus());
            return existing.get();
        }

        var notification = notificationsConsumerServiceMapper.update(new Notification(), input);
        notification.setNotificationId(BusinessIds.newId("NOT"));
        notification.setType(NotificationType.ORDER_CONFIRMATION);
        notification.setChannel(NotificationChannel.EMAIL);
        notification.setRecipient(input.getCustomerEmail());
        notification.setSubject("Order confirmed");
        notification.setBody("Your order " + input.getOrderId() + " is confirmed.");
        notification.setStatus(NotificationStatus.PENDING);
        notification.setCreatedAt(Instant.now(clock));

        var result = notificationSender.send(new NotificationSender.NotificationMessage(
                notification.getNotificationId(),
                notification.getType(),
                notification.getChannel(),
                notification.getRecipient(),
                notification.getSubject(),
                notification.getBody()));

        if (!result.delivered()) {
            // TODO T-36: FAILED with failedAt and failureReason, emit NotificationDeliveryFailed
            throw new UnsupportedOperationException("sendOrderConfirmation delivery failure branch not implemented yet");
        }
        notification.setStatus(NotificationStatus.SENT);
        notification.setSentAt(Instant.now(clock));
        notification = notificationRepository.save(notification);
        eventPublisher.onOrderConfirmationSent(eventsMapper.asOrderConfirmationSent(notification));
        return notification;
    }

    public Notification sendStockUnavailableNotification(SendStockUnavailableNotificationInput input) {

        log.debug("Request sendStockUnavailableNotification: {}", input);

        var notification = notificationsConsumerServiceMapper.update(new Notification(), input);
        // TODO: implement this method
        // emit events
        var stockUnavailableNotificationSent = eventsMapper.asStockUnavailableNotificationSent(notification);
        eventPublisher.onStockUnavailableNotificationSent(stockUnavailableNotificationSent);
        var notificationDeliveryFailed = eventsMapper.asNotificationDeliveryFailed(notification);
        eventPublisher.onNotificationDeliveryFailed(notificationDeliveryFailed);
        return notification;
    }

    public Notification sendOrderCancelledNotification(SendOrderCancelledNotificationInput input) {

        log.debug("Request sendOrderCancelledNotification: {}", input);

        var notification = notificationsConsumerServiceMapper.update(new Notification(), input);
        // TODO: implement this method
        // emit events
        var orderCancelledNotificationSent = eventsMapper.asOrderCancelledNotificationSent(notification);
        eventPublisher.onOrderCancelledNotificationSent(orderCancelledNotificationSent);
        var notificationDeliveryFailed = eventsMapper.asNotificationDeliveryFailed(notification);
        eventPublisher.onNotificationDeliveryFailed(notificationDeliveryFailed);
        return notification;
    }
}
