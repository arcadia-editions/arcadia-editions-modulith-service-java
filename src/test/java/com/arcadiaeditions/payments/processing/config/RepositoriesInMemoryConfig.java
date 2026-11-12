package com.arcadiaeditions.payments.processing.config;

import com.arcadiaeditions.payments.processing.adapter.driven.jpa.inmemory.*;
import com.arcadiaeditions.payments.processing.application.port.driven.jpa.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

// @Configuration
public class RepositoriesInMemoryConfig {

    protected final PaymentRepository paymentRepository = new PaymentRepositoryInMemory();

    @Bean
    @Primary
    public <T extends PaymentRepository> T paymentRepository() {
        return (T) paymentRepository;
    }
}
