package com.banking.engine.error;

import java.util.UUID;
import org.springframework.http.HttpStatus;

public class AccountNotFoundException extends BankingException {

    public AccountNotFoundException(UUID accountId) {
        super("ACCOUNT_NOT_FOUND", HttpStatus.NOT_FOUND, "Account " + accountId + " was not found");
    }
}
