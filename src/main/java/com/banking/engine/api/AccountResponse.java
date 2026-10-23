package com.banking.engine.api;

import com.banking.engine.CurrencyCode;
import com.banking.engine.account.Account;
import com.banking.engine.account.AccountStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record AccountResponse(
        UUID id,
        String accountNumber,
        String holderName,
        CurrencyCode currency,
        AccountStatus status,
        @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal availableBalance,
        Instant createdAt) {

    public static AccountResponse from(Account account) {
        return new AccountResponse(
                account.getId(),
                account.getAccountNumber(),
                account.getHolderName(),
                account.getCurrency(),
                account.getStatus(),
                account.getAvailableBalance(),
                account.getCreatedAt());
    }
}
