package com.arcadiaeditions.payments.processing.application.port.out.jpa;

import com.arcadiaeditions.payments.processing.domain.*;
import java.math.*;
import java.time.*;
import java.util.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the Payment entity.
 */
@SuppressWarnings("unused")
@Repository
@org.jmolecules.ddd.annotation.Repository
@org.jmolecules.architecture.hexagonal.SecondaryPort
public interface PaymentRepository extends JpaRepository<Payment, Long> {}
