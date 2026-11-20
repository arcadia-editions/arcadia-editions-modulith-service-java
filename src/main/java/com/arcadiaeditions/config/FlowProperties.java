package com.arcadiaeditions.config;

import java.time.Duration;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Place order flow settings: arcadia.payments.max-attempts, arcadia.payments.processor-timeout, arcadia.payments.retry-backoff. */
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
        /** ZFL header: one processor call lasts at most 30 seconds; then it is a technical failure. */
        private Duration processorTimeout = Duration.ofSeconds(30);
        /**
         * ZFL header: the wait of reauthorizePayment before each attempt after the first: element 0 before attempt 2
         * (5 s), element 1 before attempt 3 (15 s); the last one for any later attempt.
         */
        private List<Duration> retryBackoff = List.of(Duration.ofSeconds(5), Duration.ofSeconds(15));

        public List<Duration> getRetryBackoff() {
            return retryBackoff;
        }

        public void setRetryBackoff(List<Duration> retryBackoff) {
            this.retryBackoff = retryBackoff;
        }

        public Duration getProcessorTimeout() {
            return processorTimeout;
        }

        public void setProcessorTimeout(Duration processorTimeout) {
            this.processorTimeout = processorTimeout;
        }

        public int getMaxAttempts() {
            return maxAttempts;
        }

        public void setMaxAttempts(int maxAttempts) {
            this.maxAttempts = maxAttempts;
        }
    }
}
