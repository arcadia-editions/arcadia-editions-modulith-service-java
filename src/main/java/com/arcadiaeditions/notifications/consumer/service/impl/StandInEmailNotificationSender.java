package com.arcadiaeditions.notifications.consumer.service.impl;

import com.arcadiaeditions.notifications.consumer.domain.Notification;
import com.arcadiaeditions.notifications.consumer.service.NotificationSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Deterministic stand-in for the e-mail channel (research R-06, contracts/stand-in-adapters.md).
 *
 * <p>Logs recipient, subject and body instead of sending. A recipient ending with {@code @fail.invalid} fails with
 * {@code mailbox_unavailable}; anything else is delivered.
 */
@Component
public class StandInEmailNotificationSender implements NotificationSender {

    static final String FAILING_RECIPIENT_SUFFIX = "@fail.invalid";

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Override
    public DeliveryResult send(Notification notification) {
        var recipient = notification != null ? notification.getRecipient() : null;
        log.info(
                "Stand-in e-mail to={} subject={} body={}",
                recipient,
                notification != null ? notification.getSubject() : null,
                notification != null ? notification.getBody() : null);
        if (recipient != null && recipient.endsWith(FAILING_RECIPIENT_SUFFIX)) {
            return new DeliveryFailed("mailbox_unavailable");
        }
        return new Delivered();
    }
}
