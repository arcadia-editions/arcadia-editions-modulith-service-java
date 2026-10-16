package com.arcadiaeditions.fulfillment.shipping;

import com.arcadiaeditions.fulfillment.shipping.domain.*;
import java.math.*;
import java.time.*;
import java.util.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the Fulfillment entity.
 */
@SuppressWarnings("unused")
@Repository
@org.jmolecules.ddd.annotation.Repository
public interface FulfillmentRepository extends JpaRepository<Fulfillment, Long> {

    Optional<Fulfillment> findByOrderId(String orderId);
}
