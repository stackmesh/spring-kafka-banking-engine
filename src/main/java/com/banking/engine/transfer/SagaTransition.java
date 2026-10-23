package com.banking.engine.transfer;

import com.banking.engine.account.AccountCommandType;

public record SagaTransition(
        TransferStatus nextStatus,
        AccountCommandType command,
        String failureCode,
        String failureReason,
        boolean retry) {

    public static SagaTransition ignore(TransferStatus current) {
        return new SagaTransition(current, null, null, null, false);
    }

    public static SagaTransition command(
            TransferStatus nextStatus,
            AccountCommandType command,
            String failureCode,
            String failureReason) {
        return new SagaTransition(nextStatus, command, failureCode, failureReason, false);
    }

    public static SagaTransition terminal(TransferStatus nextStatus, String failureCode, String failureReason) {
        return new SagaTransition(nextStatus, null, failureCode, failureReason, false);
    }

    public static SagaTransition repeat() {
        return new SagaTransition(null, null, null, null, true);
    }

    public boolean ignored(TransferStatus current) {
        return !retry && command == null && nextStatus == current;
    }
}
