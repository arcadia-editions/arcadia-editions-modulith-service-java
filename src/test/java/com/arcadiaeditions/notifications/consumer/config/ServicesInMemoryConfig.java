package com.arcadiaeditions.notifications.consumer.config;

import com.arcadiaeditions.config.TestDataLoader;
import com.arcadiaeditions.notifications.consumer.domain.*;
import com.arcadiaeditions.notifications.consumer.domain.events.*;
import com.arcadiaeditions.notifications.consumer.events.*;
import com.arcadiaeditions.notifications.consumer.service.*;
import com.arcadiaeditions.notifications.consumer.service.impl.*;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Services InMemory Config. It can be used standalone or with @SpringBootTest.
 */
@Configuration
@Profile("in-memory")
public class ServicesInMemoryConfig extends RepositoriesInMemoryConfig {

    private InMemoryEventPublisher eventPublisher = new InMemoryEventPublisher();
    protected final NotificationsConsumerServiceImpl notificationsConsumerService =
            new NotificationsConsumerServiceImpl(
                    notificationRepository(),
                    eventPublisher,
                    new StandInEmailNotificationSender(),
                    Clock.fixed(Instant.parse("2026-10-14T10:00:00Z"), ZoneOffset.UTC));

    @Bean
    public <T extends NotificationsConsumerService> T notificationsConsumerService() {
        return (T) notificationsConsumerService;
    }

    static List<Notification> _notifications;

    public void reloadTestData() {

        var testDataLoader = new TestDataLoader(List.of(Notification.class));
        var notifications = _notifications != null
                ? _notifications
                : testDataLoader.loadCollectionTestDataAsObjects(Notification.class);
        notificationRepository().deleteAll();
        notificationRepository().saveAll(notifications);
        eventPublisher.getEvents().clear();
    }

    @Bean
    public InMemoryEventPublisher eventPublisher() {
        return eventPublisher;
    }
}
