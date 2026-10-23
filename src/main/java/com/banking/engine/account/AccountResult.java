package com.banking.engine.account;

import java.util.UUID;

public record AccountResult(
        UUID commandId,
        UUID transferId,
        AccountCommandType type,
        UUID accountId,
        boolean success,
        String failureCode,
        String failureReason) {

    public static AccountResult success(AccountCommand command) {
        return new AccountResult(
                command.commandId(),
                command.transferId(),
                command.type(),
                command.accountId(),
                true,
                null,
                null);
    }

    public static AccountResult failure(AccountCommand command, String failureCode, String failureReason) {
        return new AccountResult(
                command.commandId(),
                command.transferId(),
                command.type(),
                command.accountId(),
                false,
                failureCode,
                failureReason);
    }
}
