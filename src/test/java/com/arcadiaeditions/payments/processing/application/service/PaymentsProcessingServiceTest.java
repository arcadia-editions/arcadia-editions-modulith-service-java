package com.arcadiaeditions.payments.processing.application.service;

import static org.mockito.Mockito.*;

import com.arcadiaeditions.payments.processing.adapter.driven.jpa.inmemory.*;
import com.arcadiaeditions.payments.processing.application.port.driven.jpa.*;
import com.arcadiaeditions.payments.processing.application.port.driving.*;
import com.arcadiaeditions.payments.processing.application.port.driving.dto.*;
import com.arcadiaeditions.payments.processing.application.service.mapper.*;
import com.arcadiaeditions.payments.processing.config.*;
import com.arcadiaeditions.payments.processing.domain.*;
import java.time.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Acceptance Test for PaymentsProcessingService.
 */
class PaymentsProcessingServiceTest {

    private final Logger log = LoggerFactory.getLogger(getClass());

    ServicesInMemoryConfig context = new ServicesInMemoryConfig();
    PaymentsProcessingServiceImpl paymentsProcessingService = context.paymentsProcessingService();

    PaymentRepositoryInMemory paymentRepository = context.paymentRepository();

    @BeforeEach
    void setUp() {
        context.reloadTestData();
    }

    @Test
    void authorizePaymentTest() { // TODO: implement this test
    }

    @Test
    void retryPaymentTest() { // TODO: implement this test
    }

    @Test
    void capturePaymentTest() { // TODO: implement this test
    }

    @Test
    void voidPaymentTest() { // TODO: implement this test
    }
}
