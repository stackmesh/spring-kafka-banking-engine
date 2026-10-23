package com.banking.engine.outbox;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class OutboxClaimer {

    private static final Logger log = LoggerFactory.getLogger(OutboxClaimer.class);

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional
    public int recoverStale(Duration timeout) {
        Instant cutoff = Instant.now().minus(timeout);
        return entityManager.createQuery("""
                        update OutboxEvent e
                        set e.status = :pending, e.claimedAt = null
                        where e.status = :publishing and e.claimedAt < :cutoff
                        """)
                .setParameter("pending", OutboxStatus.PENDING)
                .setParameter("publishing", OutboxStatus.PUBLISHING)
                .setParameter("cutoff", cutoff)
                .executeUpdate();
    }

    @Transactional
    public List<ClaimedMessage> claim(int batchSize) {
        int limit = Math.min(batchSize, 200);
        @SuppressWarnings("unchecked")
        List<Object> rows = entityManager.createNativeQuery("""
                        SELECT id FROM outbox_event
                        WHERE status = 'PENDING' AND available_at <= :now
                        ORDER BY created_at
                        LIMIT %d
                        FOR UPDATE SKIP LOCKED
                        """.formatted(limit))
                .setParameter("now", OffsetDateTime.now(ZoneOffset.UTC))
                .getResultList();
        Instant now = Instant.now();
        List<ClaimedMessage> claimed = new ArrayList<>(rows.size());
        for (Object row : rows) {
            UUID id = toUuid(row);
            OutboxEvent event = entityManager.find(OutboxEvent.class, id);
            if (event == null || event.getStatus() != OutboxStatus.PENDING) {
                continue;
            }
            event.claim(now);
            claimed.add(new ClaimedMessage(event.getId(), event.getTopic(), event.getMessageKey(), event.getPayload()));
        }
        return claimed;
    }

    @Transactional
    public void markPublished(UUID id) {
        OutboxEvent event = entityManager.find(OutboxEvent.class, id);
        if (event != null) {
            event.markPublished(Instant.now());
        }
    }

    @Transactional
    public void markRetry(UUID id, int maxAttempts, Exception error) {
        OutboxEvent event = entityManager.find(OutboxEvent.class, id);
        if (event == null) {
            return;
        }
        String message = error.getMessage() == null ? error.getClass().getSimpleName() : error.getMessage();
        event.markFailedAttempt(Instant.now(), maxAttempts, message);
        if (event.getStatus() == OutboxStatus.FAILED) {
            log.error("Outbox event {} exhausted publish attempts for topic {}", id, event.getTopic());
        }
    }

    private static UUID toUuid(Object value) {
        if (value instanceof UUID uuid) {
            return uuid;
        }
        if (value instanceof Object[] row && row.length > 0) {
            return toUuid(row[0]);
        }
        if (value instanceof String text) {
            return UUID.fromString(text);
        }
        throw new IllegalStateException("Unexpected outbox id type " + value.getClass().getName());
    }
}
