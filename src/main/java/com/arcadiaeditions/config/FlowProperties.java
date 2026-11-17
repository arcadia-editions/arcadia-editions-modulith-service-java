package com.arcadiaeditions.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Place order flow settings: arcadia.payments.max-attempts, arcadia.payments.retry-delay. */
@ConfigurationProperties(prefix = "arcadia")
public class FlowProperties {

    private Payments payments = new Payments();

    public Payments getPayments() {
        return payments;
    }

    public void setPayments(Payments payments) {
        this.payments = payments;
    }

    public static class Payments {
        private int maxAttempts = 3;
        private Duration retryDelay = Duration.ofSeconds(30);

        public int getMaxAttempts() {
            return maxAttempts;
        }

        public void setMaxAttempts(int maxAttempts) {
            this.maxAttempts = maxAttempts;
        }

        public Duration getRetryDelay() {
            return retryDelay;
        }

        public void setRetryDelay(Duration retryDelay) {
            this.retryDelay = retryDelay;
        }
    }
}
