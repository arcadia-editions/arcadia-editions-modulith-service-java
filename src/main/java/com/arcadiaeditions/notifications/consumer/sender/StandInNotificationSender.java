package com.arcadiaeditions.notifications.consumer.sender;

import com.arcadiaeditions.notifications.consumer.service.NotificationSender;
import org.springframework.stereotype.Component;

/**
 * Deterministic stand-in for the e-mail provider (data.sql header assumptions): a recipient ending in
 * {@code @fail.invalid} fails with {@code mailbox_unavailable}; any other recipient is delivered.
 */
@Component
@org.jmolecules.architecture.layered.InfrastructureLayer
public class StandInNotificationSender implements NotificationSender {

    public static final String FAILING_DOMAIN = "@fail.invalid";
    public static final String MAILBOX_UNAVAILABLE = "mailbox_unavailable";

    @Override
    public DeliveryResult send(NotificationMessage message) {
        if (message.recipient().endsWith(FAILING_DOMAIN)) {
            return DeliveryResult.failed(MAILBOX_UNAVAILABLE);
        }
        return DeliveryResult.sent();
    }
}
