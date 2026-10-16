package com.arcadiaeditions.payments.processing.adapter.out.jpa;

import com.arcadiaeditions.common.BaseRepositoryIntegrationTest;
import com.arcadiaeditions.payments.processing.application.port.out.jpa.PaymentRepository;
import com.arcadiaeditions.payments.processing.domain.*;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class PaymentRepositoryIntegrationTest extends BaseRepositoryIntegrationTest {

    @Autowired
    EntityManager entityManager;

    @Autowired
    PaymentRepository paymentRepository;

    @Test
    void findAllTest() {
        var results = paymentRepository.findAll();
        Assertions.assertFalse(results.isEmpty());
    }

    @Test
    void findByIdTest() {
        var id = 1L;
        var payment = paymentRepository.findById(id).orElseThrow();
        Assertions.assertNotNull(payment.getId());
        Assertions.assertNotNull(payment.getVersion());
    }

    @Test
    void saveTest() {
        Payment payment = new Payment();
        payment.setOrderId("");
        payment.setCustomerId("");
        payment.setPaymentMethodId("");
        payment.setCustomerEmail("");
        payment.setAmount(BigDecimal.valueOf(0));
        payment.setCurrency("");
        payment.setStatus(PaymentStatus.values()[0]);
        payment.setProviderReference("");
        payment.setAttemptCount(0);
        payment.setAuthorizedAt(Instant.now());
        payment.setDeclinedAt(Instant.now());
        payment.setFailedAt(Instant.now());
        payment.setCapturedAt(Instant.now());
        payment.setVoidedAt(Instant.now());
        payment.setFailureReason("");

        // Persist aggregate root
        var created = paymentRepository.save(payment);

        // reloading to get relationships persisted by id
        entityManager.flush();
        entityManager.refresh(created);
        Assertions.assertNotNull(created.getId());
        Assertions.assertNotNull(created.getVersion());
    }

    @Test
    void updateTest() {
        var id = 1L;
        var now = Instant.now();
        var payment = paymentRepository.findById(id).orElseThrow();
        payment.setOrderId("");
        payment.setCustomerId("");
        payment.setPaymentMethodId("");
        payment.setCustomerEmail("");
        payment.setAmount(BigDecimal.valueOf(0));
        payment.setCurrency("");
        payment.setStatus(PaymentStatus.values()[0]);
        payment.setProviderReference("");
        payment.setAttemptCount(0);
        payment.setAuthorizedAt(now);
        payment.setDeclinedAt(now);
        payment.setFailedAt(now);
        payment.setCapturedAt(now);
        payment.setVoidedAt(now);
        payment.setFailureReason("");

        payment = paymentRepository.save(payment);
        Assertions.assertEquals("", payment.getOrderId());
        Assertions.assertEquals("", payment.getCustomerId());
        Assertions.assertEquals("", payment.getPaymentMethodId());
        Assertions.assertEquals("", payment.getCustomerEmail());
        Assertions.assertEquals(BigDecimal.valueOf(0), payment.getAmount());
        Assertions.assertEquals("", payment.getCurrency());
        Assertions.assertEquals(PaymentStatus.values()[0], payment.getStatus());
        Assertions.assertEquals("", payment.getProviderReference());
        Assertions.assertEquals(0, payment.getAttemptCount());
        Assertions.assertEquals(now, payment.getAuthorizedAt());
        Assertions.assertEquals(now, payment.getDeclinedAt());
        Assertions.assertEquals(now, payment.getFailedAt());
        Assertions.assertEquals(now, payment.getCapturedAt());
        Assertions.assertEquals(now, payment.getVoidedAt());
        Assertions.assertEquals("", payment.getFailureReason());
    }

    @Test
    void deleteTest() {
        var id = 1L;
        paymentRepository.deleteById(id);
        var notFound = paymentRepository.findById(id);
        Assertions.assertFalse(notFound.isPresent());
    }
}
