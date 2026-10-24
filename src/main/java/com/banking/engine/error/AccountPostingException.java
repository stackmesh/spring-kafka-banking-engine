package com.banking.engine.error;

import org.springframework.http.HttpStatus;

public class AccountPostingException extends BankingException {

    public AccountPostingException(String code, String message) {
        super(code, HttpStatus.UNPROCESSABLE_CONTENT, message);
    }
}
