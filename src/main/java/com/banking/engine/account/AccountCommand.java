package com.banking.engine.account;

import com.banking.engine.CurrencyCode;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

public record AccountCommand(
        UUID commandId,
        UUID transferId,
        AccountCommandType type,
        UUID accountId,
        BigDecimal amount,
        String currency) {

    public static AccountCommand of(
            UUID transferId,
            AccountCommandType type,
            UUID accountId,
            BigDecimal amount,
            CurrencyCode currency) {
        return new AccountCommand(deterministicId(transferId, type), transferId, type, accountId, amount, currency.name());
    }

    public static UUID deterministicId(UUID transferId, AccountCommandType type) {
        return UUID.nameUUIDFromBytes((transferId + ":" + type.name()).getBytes(StandardCharsets.UTF_8));
    }
}
