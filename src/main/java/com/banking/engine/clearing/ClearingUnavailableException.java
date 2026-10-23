package com.banking.engine.clearing;

public class ClearingUnavailableException extends RuntimeException {

    public ClearingUnavailableException(String message) {
        super(message);
    }

    public ClearingUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
