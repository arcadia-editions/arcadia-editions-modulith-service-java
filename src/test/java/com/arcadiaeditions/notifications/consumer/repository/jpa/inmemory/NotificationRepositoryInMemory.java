package com.arcadiaeditions.notifications.consumer.repository.jpa.inmemory;

import com.arcadiaeditions.notifications.consumer.domain.*;
import com.arcadiaeditions.notifications.consumer.repository.jpa.NotificationRepository;
import java.math.*;
import java.time.*;
import java.util.*;

public class NotificationRepositoryInMemory extends InMemoryJpaRepository<Notification>
        implements NotificationRepository {

    @Override
    public Optional<Notification> findByOrderIdAndType(String orderId, NotificationType type) {
        return getEntities().values().stream()
                .filter(notification -> Objects.equals(orderId, notification.getOrderId()))
                .filter(notification -> Objects.equals(type, notification.getType()))
                .findFirst();
    }
}
