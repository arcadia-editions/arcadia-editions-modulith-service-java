package com.arcadiaeditions.notifications.consumer.repository.jpa;

import com.arcadiaeditions.common.BaseRepositoryIntegrationTest;
import com.arcadiaeditions.notifications.consumer.domain.*;
import jakarta.persistence.EntityManager;
import java.time.*;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class NotificationRepositoryIntegrationTest extends BaseRepositoryIntegrationTest {

    @Autowired
    EntityManager entityManager;

    @Autowired
    NotificationRepository notificationRepository;

    @Test
    void findAllTest() {
        var results = notificationRepository.findAll();
        Assertions.assertFalse(results.isEmpty());
    }

    @Test
    void findByIdTest() {
        var id = 1L;
        var notification = notificationRepository.findById(id).orElseThrow();
        Assertions.assertNotNull(notification.getId());
        Assertions.assertNotNull(notification.getVersion());
    }

    @Test
    void saveTest() {
        Notification notification = new Notification();
        notification.setNotificationId("");
        notification.setCustomerId("");
        notification.setOrderId("");
        notification.setSkus(List.of(""));
        notification.setType(NotificationType.values()[0]);
        notification.setChannel(NotificationChannel.values()[0]);
        notification.setStatus(NotificationStatus.values()[0]);
        notification.setSubject("");
        notification.setBody("");
        notification.setRecipient("");
        notification.setCreatedAt(Instant.now());
        notification.setSentAt(Instant.now());
        notification.setFailedAt(Instant.now());
        notification.setFailureReason("");

        // Persist aggregate root
        var created = notificationRepository.save(notification);

        // reloading to get relationships persisted by id
        entityManager.flush();
        entityManager.refresh(created);
        Assertions.assertNotNull(created.getId());
        Assertions.assertNotNull(created.getVersion());
    }

    @Test
    void updateTest() {
        var id = 1L;
        var notification = notificationRepository.findById(id).orElseThrow();
        notification.setNotificationId("");
        notification.setCustomerId("");
        notification.setOrderId("");
        notification.setSkus(List.of(""));
        notification.setType(NotificationType.values()[0]);
        notification.setChannel(NotificationChannel.values()[0]);
        notification.setStatus(NotificationStatus.values()[0]);
        notification.setSubject("");
        notification.setBody("");
        notification.setRecipient("");
        notification.setCreatedAt(Instant.now());
        notification.setSentAt(Instant.now());
        notification.setFailedAt(Instant.now());
        notification.setFailureReason("");

        notification = notificationRepository.save(notification);
        Assertions.assertEquals("", notification.getNotificationId());
        Assertions.assertEquals("", notification.getCustomerId());
        Assertions.assertEquals("", notification.getOrderId());
        Assertions.assertEquals(List.of(""), notification.getSkus());
        Assertions.assertEquals(NotificationType.values()[0], notification.getType());
        Assertions.assertEquals(NotificationChannel.values()[0], notification.getChannel());
        Assertions.assertEquals(NotificationStatus.values()[0], notification.getStatus());
        Assertions.assertEquals("", notification.getSubject());
        Assertions.assertEquals("", notification.getBody());
        Assertions.assertEquals("", notification.getRecipient());
        Assertions.assertEquals(Instant.now(), notification.getCreatedAt());
        Assertions.assertEquals(Instant.now(), notification.getSentAt());
        Assertions.assertEquals(Instant.now(), notification.getFailedAt());
        Assertions.assertEquals("", notification.getFailureReason());
    }

    @Test
    void deleteTest() {
        var id = 1L;
        notificationRepository.deleteById(id);
        var notFound = notificationRepository.findById(id);
        Assertions.assertFalse(notFound.isPresent());
    }
}
