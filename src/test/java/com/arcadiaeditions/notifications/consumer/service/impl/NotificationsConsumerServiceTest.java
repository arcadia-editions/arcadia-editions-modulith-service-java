package com.arcadiaeditions.notifications.consumer.service.impl;

import static org.mockito.Mockito.*;

import com.arcadiaeditions.notifications.consumer.config.*;
import com.arcadiaeditions.notifications.consumer.domain.*;
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

    @BeforeEach
    void setUp() {
        context.reloadTestData();
    }

    @Test
    void sendOrderConfirmationTest() { // TODO: implement this test
    }

    @Test
    void sendStockUnavailableNotificationTest() { // TODO: implement this test
    }

    @Test
    void sendOrderCancelledNotificationTest() { // TODO: implement this test
    }
}
