package com.arcadiaeditions.notifications.consumer.service.impl;

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

    /**
     * Constructor.
     */
    public NotificationsConsumerServiceImpl(
            NotificationRepository notificationRepository, EventPublisher eventPublisher) {
        this.notificationRepository = notificationRepository;
        this.eventPublisher = eventPublisher;
    }

    public Notification sendOrderConfirmation(SendOrderConfirmationInput input) {

        log.debug("Request sendOrderConfirmation: {}", input);

        var notification = notificationsConsumerServiceMapper.update(new Notification(), input);
        // TODO: implement this method
        // emit events
        var orderConfirmationSent = eventsMapper.asOrderConfirmationSent(notification);
        eventPublisher.onOrderConfirmationSent(orderConfirmationSent);
        var notificationDeliveryFailed = eventsMapper.asNotificationDeliveryFailed(notification);
        eventPublisher.onNotificationDeliveryFailed(notificationDeliveryFailed);
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
