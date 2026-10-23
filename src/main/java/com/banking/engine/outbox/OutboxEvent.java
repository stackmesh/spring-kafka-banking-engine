package com.banking.engine.outbox;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "outbox_event")
public class OutboxEvent {

    @Id
    private UUID id;

    @Column(nullable = false, length = 200)
    private String topic;

    @Column(name = "message_key", nullable = false, length = 100)
    private String messageKey;

    @Column(nullable = false, columnDefinition = "text")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OutboxStatus status;

    @Column(nullable = false)
    private int attempts;

    @Column(name = "last_error", length = 500)
    private String lastError;

    @Column(name = "available_at", nullable = false)
    private Instant availableAt;

    @Column(name = "claimed_at")
    private Instant claimedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "published_at")
    private Instant publishedAt;

    protected OutboxEvent() {
    }

    public static OutboxEvent pending(String topic, String messageKey, String payload) {
        Instant now = Instant.now();
        OutboxEvent event = new OutboxEvent();
        event.id = UUID.randomUUID();
        event.topic = topic;
        event.messageKey = messageKey;
        event.payload = payload;
        event.status = OutboxStatus.PENDING;
        event.attempts = 0;
        event.availableAt = now;
        event.createdAt = now;
        return event;
    }

    public void claim(Instant now) {
        status = OutboxStatus.PUBLISHING;
        claimedAt = now;
    }

    public void markPublished(Instant now) {
        status = OutboxStatus.PUBLISHED;
        publishedAt = now;
        claimedAt = null;
        lastError = null;
    }

    public void markFailedAttempt(Instant now, int maxAttempts, String error) {
        attempts += 1;
        lastError = truncate(error);
        claimedAt = null;
        if (attempts >= maxAttempts) {
            status = OutboxStatus.FAILED;
            return;
        }
        status = OutboxStatus.PENDING;
        availableAt = now.plusSeconds(Math.min(attempts, 30L));
    }

    private static String truncate(String error) {
        if (error == null || error.length() <= 500) {
            return error;
        }
        return error.substring(0, 500);
    }

    public UUID getId() {
        return id;
    }

    public String getTopic() {
        return topic;
    }

    public String getMessageKey() {
        return messageKey;
    }

    public String getPayload() {
        return payload;
    }

    public OutboxStatus getStatus() {
        return status;
    }

    public int getAttempts() {
        return attempts;
    }

    public String getLastError() {
        return lastError;
    }
}
