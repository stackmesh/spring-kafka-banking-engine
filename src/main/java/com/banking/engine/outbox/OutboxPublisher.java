package com.banking.engine.outbox;

import com.banking.engine.config.BankingProperties;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class OutboxPublisher {

    private static final Logger log = LoggerFactory.getLogger(OutboxPublisher.class);

    private final OutboxClaimer claimer;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final BankingProperties properties;

    public OutboxPublisher(
            OutboxClaimer claimer,
            KafkaTemplate<String, String> kafkaTemplate,
            BankingProperties properties) {
        this.claimer = claimer;
        this.kafkaTemplate = kafkaTemplate;
        this.properties = properties;
    }

    @Scheduled(
            initialDelayString = "${banking.outbox.initial-delay:2s}",
            fixedDelayString = "${banking.outbox.poll-delay:250ms}")
    public void publishPending() {
        int recovered = claimer.recoverStale(properties.outbox().publishingTimeout());
        if (recovered > 0) {
            log.warn("Returned {} stale outbox events to the pending queue", recovered);
        }
        for (ClaimedMessage message : claimer.claim(properties.outbox().batchSize())) {
            publish(message);
        }
    }

    private void publish(ClaimedMessage message) {
        try {
            kafkaTemplate.send(message.topic(), message.key(), message.payload()).get(5, TimeUnit.SECONDS);
            claimer.markPublished(message.id());
            log.debug("Published outbox event {} to {}", message.id(), message.topic());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            claimer.markRetry(message.id(), properties.outbox().maxAttempts(), ex);
        } catch (Exception ex) {
            log.warn("Failed to publish outbox event {} to {}", message.id(), message.topic(), ex);
            claimer.markRetry(message.id(), properties.outbox().maxAttempts(), ex);
        }
    }
}
