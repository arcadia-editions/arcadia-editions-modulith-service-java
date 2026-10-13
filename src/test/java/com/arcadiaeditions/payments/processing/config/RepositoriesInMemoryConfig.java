package com.arcadiaeditions.payments.processing.config;

import com.arcadiaeditions.payments.processing.adapter.out.jpa.inmemory.*;
import com.arcadiaeditions.payments.processing.application.port.out.jpa.*;
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
