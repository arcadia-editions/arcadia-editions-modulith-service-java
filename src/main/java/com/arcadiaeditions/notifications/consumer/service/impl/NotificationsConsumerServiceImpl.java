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
import java.util.function.Consumer;
import java.util.function.Supplier;
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

        return deliverOnce(
                input.getOrderId(),
                NotificationType.ORDER_CONFIRMATION,
                () -> {
                    var notification = notificationsConsumerServiceMapper.update(new Notification(), input);
                    notification
                            .setRecipient(input.getCustomerEmail())
                            .setSubject("Order confirmed")
                            .setBody("Your order %s is confirmed.".formatted(input.getOrderId()));
                    return notification;
                },
                sent -> eventPublisher.onOrderConfirmationSent(eventsMapper.asOrderConfirmationSent(sent)));
    }

    public Notification sendStockUnavailableNotification(SendStockUnavailableNotificationInput input) {

        log.debug("Request sendStockUnavailableNotification: {}", input);

        return deliverOnce(
                input.getOrderId(),
                NotificationType.STOCK_UNAVAILABLE,
                () -> {
                    var notification = notificationsConsumerServiceMapper.update(new Notification(), input);
                    var skus = input.getUnavailableItems().stream()
                            .map(StockUnavailableItemInput::getSku)
                            .toList();
                    notification
                            .setRecipient(input.getCustomerEmail())
                            .setSkus(new ArrayList<>(skus))
                            .setSubject("Items out of stock")
                            .setBody("Your order %s could not be placed: %s out of stock."
                                    .formatted(input.getOrderId(), String.join(", ", skus)));
                    return notification;
                },
                sent -> eventPublisher.onStockUnavailableNotificationSent(
                        eventsMapper.asStockUnavailableNotificationSent(sent)));
    }

    public Notification sendOrderCancelledNotification(SendOrderCancelledNotificationInput input) {

        log.debug("Request sendOrderCancelledNotification: {}", input);

        return deliverOnce(
                input.getOrderId(),
                NotificationType.ORDER_CANCELLED,
                () -> {
                    var notification = notificationsConsumerServiceMapper.update(new Notification(), input);
                    notification
                            .setRecipient(input.getCustomerEmail())
                            .setSubject("Order cancelled")
                            .setBody("Your order %s was cancelled: %s."
                                    .formatted(input.getOrderId(), input.getReason()));
                    return notification;
                },
                sent -> eventPublisher.onOrderCancelledNotificationSent(
                        eventsMapper.asOrderCancelledNotificationSent(sent, input.getReason())));
    }

    /**
     * Shared delivery flow of the three notification commands: a notification for ({@code orderId}, {@code type})
     * already {@code SENT} or {@code FAILED} is returned untouched and nothing is emitted (redelivery). Otherwise the
     * draft is completed ({@code channel}, {@code notificationId}, {@code createdAt}), handed to the
     * {@link NotificationSender}, saved as {@code SENT} (and {@code onSent} emits the type specific event) or as
     * {@code FAILED} (and {@code NotificationDeliveryFailed} is emitted).
     *
     * @param draft builds the type specific part: recipient, subject, body, skus ...
     * @param onSent emits the "sent" event of the notification type
     */
    private Notification deliverOnce(
            String orderId, NotificationType type, Supplier<Notification> draft, Consumer<Notification> onSent) {
        var existing = notificationRepository.findByOrderIdAndType(orderId, type);
        if (existing.isPresent()
                && (existing.get().getStatus() == NotificationStatus.SENT
                        || existing.get().getStatus() == NotificationStatus.FAILED)) {
            log.debug(
                    "Notification {} for order {} already {}, ignoring",
                    type,
                    orderId,
                    existing.get().getStatus());
            return existing.get();
        }

        var notification = existing.orElseGet(draft);
        notification
                .setType(type)
                .setChannel(NotificationChannel.EMAIL)
                .setNotificationId(
                        notification.getNotificationId() != null
                                ? notification.getNotificationId()
                                : BusinessIds.newId("NOT"))
                .setCreatedAt(notification.getCreatedAt() != null ? notification.getCreatedAt() : Instant.now(clock));

        switch (notificationSender.send(notification)) {
            case NotificationSender.Delivered delivered -> {
                notification.setStatus(NotificationStatus.SENT).setSentAt(Instant.now(clock));
                notification = notificationRepository.save(notification);
                onSent.accept(notification);
            }
            case NotificationSender.DeliveryFailed failed -> {
                notification
                        .setStatus(NotificationStatus.FAILED)
                        .setFailedAt(Instant.now(clock))
                        .setFailureReason(failed.reason());
                notification = notificationRepository.save(notification);
                eventPublisher.onNotificationDeliveryFailed(eventsMapper.asNotificationDeliveryFailed(notification));
            }
        }
        return notification;
    }
}
