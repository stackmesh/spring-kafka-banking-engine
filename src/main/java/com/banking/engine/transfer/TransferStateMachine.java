package com.banking.engine.transfer;

import com.banking.engine.account.AccountCommandType;
import com.banking.engine.clearing.ClearingDecision;
import org.springframework.stereotype.Component;

@Component
public class TransferStateMachine {

    public SagaTransition decide(
            TransferStatus status,
            SagaSignal signal,
            ClearingDecision clearing,
            String failureCode,
            String failureReason) {
        return switch (status) {
            case REQUESTED -> signal == SagaSignal.START
                    ? SagaTransition.command(TransferStatus.DEBIT_PENDING, AccountCommandType.DEBIT, null, null)
                    : SagaTransition.ignore(status);
            case DEBIT_PENDING -> onDebitPending(status, signal, clearing, failureCode, failureReason);
            case CREDIT_PENDING -> onCreditPending(status, signal, failureCode, failureReason);
            case COMPENSATION_PENDING -> onCompensationPending(status, signal);
            case COMPLETED, ROLLED_BACK, FAILED -> SagaTransition.ignore(status);
        };
    }

    private static SagaTransition onDebitPending(
            TransferStatus status,
            SagaSignal signal,
            ClearingDecision clearing,
            String failureCode,
            String failureReason) {
        return switch (signal) {
            case DEBIT_SUCCEEDED -> afterDebit(clearing);
            case DEBIT_FAILED -> SagaTransition.terminal(TransferStatus.FAILED, failureCode, failureReason);
            case START, CREDIT_SUCCEEDED, CREDIT_FAILED, COMPENSATE_SUCCEEDED, COMPENSATE_FAILED ->
                    SagaTransition.ignore(status);
        };
    }

    private static SagaTransition afterDebit(ClearingDecision clearing) {
        if (clearing == null) {
            throw new IllegalStateException("A clearing decision is required after a successful debit");
        }
        if (clearing.approved()) {
            return SagaTransition.command(TransferStatus.CREDIT_PENDING, AccountCommandType.CREDIT, null, null);
        }
        return SagaTransition.command(
                TransferStatus.COMPENSATION_PENDING,
                AccountCommandType.COMPENSATE,
                clearing.code(),
                clearing.reason());
    }

    private static SagaTransition onCreditPending(
            TransferStatus status,
            SagaSignal signal,
            String failureCode,
            String failureReason) {
        return switch (signal) {
            case CREDIT_SUCCEEDED -> SagaTransition.terminal(TransferStatus.COMPLETED, null, null);
            case CREDIT_FAILED -> SagaTransition.command(
                    TransferStatus.COMPENSATION_PENDING,
                    AccountCommandType.COMPENSATE,
                    failureCode,
                    failureReason);
            case START, DEBIT_SUCCEEDED, DEBIT_FAILED, COMPENSATE_SUCCEEDED, COMPENSATE_FAILED ->
                    SagaTransition.ignore(status);
        };
    }

    private static SagaTransition onCompensationPending(TransferStatus status, SagaSignal signal) {
        return switch (signal) {
            case COMPENSATE_SUCCEEDED -> SagaTransition.terminal(TransferStatus.ROLLED_BACK, null, null);
            case COMPENSATE_FAILED -> SagaTransition.repeat();
            case START, DEBIT_SUCCEEDED, DEBIT_FAILED, CREDIT_SUCCEEDED, CREDIT_FAILED -> SagaTransition.ignore(status);
        };
    }
}
