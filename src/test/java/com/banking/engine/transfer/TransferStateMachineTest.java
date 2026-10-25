package com.banking.engine.transfer;

import static org.assertj.core.api.Assertions.assertThat;

import com.banking.engine.account.AccountCommandType;
import com.banking.engine.clearing.ClearingDecision;
import org.junit.jupiter.api.Test;

class TransferStateMachineTest {

    private final TransferStateMachine machine = new TransferStateMachine();

    @Test
    void startDebitsTheSourceAccount() {
        SagaTransition transition = machine.decide(TransferStatus.REQUESTED, SagaSignal.START, null, null, null);

        assertThat(transition.nextStatus()).isEqualTo(TransferStatus.DEBIT_PENDING);
        assertThat(transition.command()).isEqualTo(AccountCommandType.DEBIT);
    }

    @Test
    void approvedDebitMovesOnToCredit() {
        SagaTransition transition = machine.decide(
                TransferStatus.DEBIT_PENDING,
                SagaSignal.DEBIT_SUCCEEDED,
                ClearingDecision.approve(),
                null,
                null);

        assertThat(transition.nextStatus()).isEqualTo(TransferStatus.CREDIT_PENDING);
        assertThat(transition.command()).isEqualTo(AccountCommandType.CREDIT);
    }

    @Test
    void declinedClearingCompensatesTheDebit() {
        SagaTransition transition = machine.decide(
                TransferStatus.DEBIT_PENDING,
                SagaSignal.DEBIT_SUCCEEDED,
                ClearingDecision.decline("CLEARING_REJECTED", "declined"),
                null,
                null);

        assertThat(transition.nextStatus()).isEqualTo(TransferStatus.COMPENSATION_PENDING);
        assertThat(transition.command()).isEqualTo(AccountCommandType.COMPENSATE);
        assertThat(transition.failureCode()).isEqualTo("CLEARING_REJECTED");
    }

    @Test
    void insufficientFundsFailsWithoutACompensation() {
        SagaTransition transition = machine.decide(
                TransferStatus.DEBIT_PENDING,
                SagaSignal.DEBIT_FAILED,
                null,
                "INSUFFICIENT_FUNDS",
                "short");

        assertThat(transition.nextStatus()).isEqualTo(TransferStatus.FAILED);
        assertThat(transition.command()).isNull();
        assertThat(transition.failureCode()).isEqualTo("INSUFFICIENT_FUNDS");
    }

    @Test
    void successfulCreditCompletesTheTransfer() {
        SagaTransition transition = machine.decide(
                TransferStatus.CREDIT_PENDING,
                SagaSignal.CREDIT_SUCCEEDED,
                null,
                null,
                null);

        assertThat(transition.nextStatus()).isEqualTo(TransferStatus.COMPLETED);
        assertThat(transition.command()).isNull();
    }

    @Test
    void failedCreditRollsTheDebitBack() {
        SagaTransition transition = machine.decide(
                TransferStatus.CREDIT_PENDING,
                SagaSignal.CREDIT_FAILED,
                null,
                "ACCOUNT_FROZEN",
                "frozen");

        assertThat(transition.nextStatus()).isEqualTo(TransferStatus.COMPENSATION_PENDING);
        assertThat(transition.command()).isEqualTo(AccountCommandType.COMPENSATE);
    }

    @Test
    void successfulCompensationClosesTheRollback() {
        SagaTransition transition = machine.decide(
                TransferStatus.COMPENSATION_PENDING,
                SagaSignal.COMPENSATE_SUCCEEDED,
                null,
                null,
                null);

        assertThat(transition.nextStatus()).isEqualTo(TransferStatus.ROLLED_BACK);
    }

    @Test
    void failedCompensationStaysRetryable() {
        SagaTransition transition = machine.decide(
                TransferStatus.COMPENSATION_PENDING,
                SagaSignal.COMPENSATE_FAILED,
                null,
                "ACCOUNT_CLOSED",
                "closed");

        assertThat(transition.retry()).isTrue();
    }

    @Test
    void duplicateSignalsDoNotMoveATerminalTransfer() {
        SagaTransition transition = machine.decide(
                TransferStatus.COMPLETED,
                SagaSignal.DEBIT_SUCCEEDED,
                ClearingDecision.approve(),
                null,
                null);

        assertThat(transition.ignored(TransferStatus.COMPLETED)).isTrue();
    }

    @Test
    void aRedeliveredStartDoesNotDebitTwice() {
        SagaTransition transition = machine.decide(TransferStatus.DEBIT_PENDING, SagaSignal.START, null, null, null);

        assertThat(transition.ignored(TransferStatus.DEBIT_PENDING)).isTrue();
    }
}
