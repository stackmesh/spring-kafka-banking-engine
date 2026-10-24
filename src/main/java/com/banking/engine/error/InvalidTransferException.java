package com.banking.engine.error;

import org.springframework.http.HttpStatus;

public class InvalidTransferException extends BankingException {

    public InvalidTransferException(String code, String message) {
        super(code, HttpStatus.UNPROCESSABLE_CONTENT, message);
    }
}
