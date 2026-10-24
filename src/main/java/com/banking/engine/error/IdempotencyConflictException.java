package com.banking.engine.error;

import org.springframework.http.HttpStatus;

public class IdempotencyConflictException extends BankingException {

    public IdempotencyConflictException(String idempotencyKey) {
        super(
                "IDEMPOTENCY_CONFLICT",
                HttpStatus.CONFLICT,
                "Idempotency-Key " + idempotencyKey + " was already used for a different transfer");
    }
}
