package com.arcadiaeditions.notifications.consumer.repository.jpa;

import com.arcadiaeditions.notifications.consumer.domain.*;
import java.math.*;
import java.time.*;
import java.util.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the Notification entity.
 */
@SuppressWarnings("unused")
@Repository
@org.jmolecules.ddd.annotation.Repository
@org.jmolecules.architecture.layered.InfrastructureLayer
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    Optional<Notification> findByOrderIdAndType(String orderId, NotificationType type);
}
