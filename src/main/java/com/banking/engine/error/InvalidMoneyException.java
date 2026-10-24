package com.banking.engine.error;

import org.springframework.http.HttpStatus;

public class InvalidMoneyException extends BankingException {

    public InvalidMoneyException(String message) {
        super("INVALID_AMOUNT", HttpStatus.BAD_REQUEST, message);
    }
}
