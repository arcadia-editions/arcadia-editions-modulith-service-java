package com.arcadiaeditions.payments.processing.config;

import com.arcadiaeditions.config.TestDataLoader;
import com.arcadiaeditions.payments.processing.adapter.driven.event.*;
import com.arcadiaeditions.payments.processing.application.port.driving.*;
import com.arcadiaeditions.payments.processing.application.service.*;
import com.arcadiaeditions.payments.processing.domain.*;
import com.arcadiaeditions.payments.processing.domain.event.*;
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

    public final java.time.Clock clock = com.arcadiaeditions.config.TestClock.FIXED;

    private InMemoryEventPublisher eventPublisher = new InMemoryEventPublisher();
    private final InMemoryTransactionManager transactionManager = new InMemoryTransactionManager();
    protected final PaymentsProcessingServiceImpl paymentsProcessingService =
            new PaymentsProcessingServiceImpl(
                    paymentRepository(),
                    eventPublisher,
                    new com.arcadiaeditions.payments.processing.adapter.driven.provider.StandInPaymentProvider(),
                    clock,
                    transactionManager,
                    new com.arcadiaeditions.config.FlowProperties());

    @Bean
    public <T extends PaymentsProcessingService> T paymentsProcessingService() {
        return (T) paymentsProcessingService;
    }

    static List<Payment> _payments;

    public void reloadTestData() {

        var testDataLoader = new TestDataLoader(List.of(Payment.class));
        var payments = _payments != null ? _payments : testDataLoader.loadCollectionTestDataAsObjects(Payment.class);
        paymentRepository().deleteAll();
        paymentRepository().saveAll(payments);
        eventPublisher.getEvents().clear();
        transactionManager.reset();
    }

    /** The in-memory transaction manager, for tests that check what was committed and when. */
    public InMemoryTransactionManager transactionManager() {
        return transactionManager;
    }

    @Bean
    public InMemoryEventPublisher eventPublisher() {
        return eventPublisher;
    }
}
