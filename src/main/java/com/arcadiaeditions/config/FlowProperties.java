package com.arcadiaeditions.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties("arcadia")
public record FlowProperties(@DefaultValue Payments payments) {

    public FlowProperties {
        if (payments == null) {
            payments = new Payments(3, Duration.ofMillis(200));
        }
    }

    public record Payments(
            @DefaultValue("3") int maxAttempts,
            @DefaultValue("200ms") Duration retryDelay) {}
}
