package com.banking.engine.error;

import org.springframework.http.HttpStatus;

public class BankingException extends RuntimeException {

    private final String code;
    private final HttpStatus status;

    public BankingException(String code, HttpStatus status, String message) {
        super(message);
        this.code = code;
        this.status = status;
    }

    public String code() {
        return code;
    }

    public HttpStatus status() {
        return status;
    }
}
