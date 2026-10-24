package com.banking.engine.config;

import com.banking.engine.clearing.ClearingMode;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "banking")
public record BankingProperties(Outbox outbox, Clearing clearing, Messaging messaging) {

    public record Outbox(
            Duration pollDelay,
            Duration initialDelay,
            int batchSize,
            Duration publishingTimeout,
            int maxAttempts) {

        public Outbox {
            if (batchSize <= 0 || maxAttempts <= 0) {
                throw new IllegalArgumentException("Outbox batch size and max attempts must be positive");
            }
        }
    }

    public record Clearing(ClearingMode mode) {
    }

    public record Messaging(int partitions, int replicas) {

        public Messaging {
            if (partitions <= 0 || replicas <= 0) {
                throw new IllegalArgumentException("Topic partitions and replicas must be positive");
            }
        }
    }
}
