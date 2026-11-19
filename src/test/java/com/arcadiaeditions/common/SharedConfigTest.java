package com.arcadiaeditions.common;

import static org.junit.jupiter.api.Assertions.*;

import com.arcadiaeditions.config.ClockConfiguration;
import com.arcadiaeditions.config.FlowProperties;
import com.arcadiaeditions.config.TestClock;
import com.arcadiaeditions.fulfillment.shipping.config.ServicesInMemoryConfig;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class SharedConfigTest {

    @Test
    void businessIdIsPrefixUuid() {
        String id = BusinessIds.newId("ORD");
        assertTrue(id.matches("ORD-[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"), id);
        assertNotEquals(id, BusinessIds.newId("ORD"));
    }

    @Test
    void flowPropertiesDefaults() {
        var p = new FlowProperties().getPayments();
        assertEquals(3, p.getMaxAttempts());
        assertEquals(java.util.List.of(Duration.ofSeconds(5), Duration.ofSeconds(15)), p.getRetryBackoff());
    }

    @Test
    void productionClockIsSystemUtc() {
        assertNotNull(new ClockConfiguration().clock());
    }

    @Test
    void inMemoryConfigsUseFixedClock() {
        assertEquals(Instant.parse("2026-10-14T10:00:00Z"), TestClock.FIXED.instant());
        assertSame(TestClock.FIXED, new ServicesInMemoryConfig().clock);
        assertSame(TestClock.FIXED, new com.arcadiaeditions.orders.checkout.config.ServicesInMemoryConfig().clock);
        assertSame(TestClock.FIXED, new com.arcadiaeditions.payments.processing.config.ServicesInMemoryConfig().clock);
        assertSame(TestClock.FIXED, new com.arcadiaeditions.notifications.consumer.config.ServicesInMemoryConfig().clock);
        assertSame(TestClock.FIXED, new com.arcadiaeditions.catalog.inventory.config.ServicesInMemoryConfig().clock);
    }
}
