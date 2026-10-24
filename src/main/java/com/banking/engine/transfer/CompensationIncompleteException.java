package com.banking.engine.transfer;

import java.util.UUID;

public class CompensationIncompleteException extends RuntimeException {

    public CompensationIncompleteException(UUID transferId) {
        super("Compensation for transfer " + transferId + " did not post; it will be retried");
    }
}
