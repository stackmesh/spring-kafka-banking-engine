package com.banking.engine.error;

import org.springframework.http.HttpStatus;

public class InvalidAccountStateException extends BankingException {

    public InvalidAccountStateException(String message) {
        super("ACCOUNT_STATE", HttpStatus.UNPROCESSABLE_CONTENT, message);
    }
}
