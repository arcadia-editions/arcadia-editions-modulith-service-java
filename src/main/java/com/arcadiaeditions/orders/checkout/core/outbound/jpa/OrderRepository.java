package com.arcadiaeditions.orders.checkout.core.outbound.jpa;

import com.arcadiaeditions.orders.checkout.core.domain.*;
import java.math.*;
import java.time.*;
import java.util.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the Order entity.
 */
@SuppressWarnings("unused")
@Repository
@org.jmolecules.ddd.annotation.Repository
@org.jmolecules.architecture.hexagonal.SecondaryPort
public interface OrderRepository extends JpaRepository<Order, Long> {

    Optional<Order> findByOrderId(String orderId);
}
