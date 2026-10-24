package com.banking.engine.outbox;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

@Component
public class OutboxWriter {

    private final OutboxEventRepository repository;
    private final JsonMapper json;

    public OutboxWriter(OutboxEventRepository repository, JsonMapper json) {
        this.repository = repository;
        this.json = json;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void enqueue(String topic, String key, Object payload) {
        repository.save(OutboxEvent.pending(topic, key, json.writeValueAsString(payload)));
    }
}
