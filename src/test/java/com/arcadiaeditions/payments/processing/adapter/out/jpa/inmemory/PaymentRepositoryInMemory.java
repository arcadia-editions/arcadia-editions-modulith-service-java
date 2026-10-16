package com.arcadiaeditions.payments.processing.adapter.out.jpa.inmemory;

import com.arcadiaeditions.payments.processing.application.port.out.jpa.PaymentRepository;
import com.arcadiaeditions.payments.processing.domain.*;
import java.math.*;
import java.time.*;
import java.util.*;

public class PaymentRepositoryInMemory extends InMemoryJpaRepository<Payment> implements PaymentRepository {

    @Override
    public Optional<Payment> findByOrderId(String orderId) {
        return Optional.ofNullable(findByUniqueField("orderId", orderId));
    }
}
