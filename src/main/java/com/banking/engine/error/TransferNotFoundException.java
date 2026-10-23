package com.banking.engine.error;

import java.util.UUID;
import org.springframework.http.HttpStatus;

public class TransferNotFoundException extends BankingException {

    public TransferNotFoundException(UUID transferId) {
        super("TRANSFER_NOT_FOUND", HttpStatus.NOT_FOUND, "Transfer " + transferId + " was not found");
    }
}
