package com.banking.engine.outbox;

import java.util.UUID;

public record ClaimedMessage(UUID id, String topic, String key, String payload) {
}
