package com.arcadiaeditions.payments.processing.config;

import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;

/** Transaction manager of the in-memory tests: no resources, it counts the commits and rollbacks. */
public class InMemoryTransactionManager implements PlatformTransactionManager {

    private final AtomicInteger commits = new AtomicInteger();
    private final AtomicInteger rollbacks = new AtomicInteger();

    @Override
    public TransactionStatus getTransaction(TransactionDefinition definition) {
        return new SimpleTransactionStatus(true);
    }

    @Override
    public void commit(TransactionStatus status) {
        commits.incrementAndGet();
    }

    @Override
    public void rollback(TransactionStatus status) {
        rollbacks.incrementAndGet();
    }

    public int commits() {
        return commits.get();
    }

    public int rollbacks() {
        return rollbacks.get();
    }

    public void reset() {
        commits.set(0);
        rollbacks.set(0);
    }
}
