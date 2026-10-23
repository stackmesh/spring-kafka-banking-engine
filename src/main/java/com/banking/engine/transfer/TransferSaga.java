package com.banking.engine.transfer;

import com.banking.engine.account.AccountCommand;
import com.banking.engine.account.AccountCommandType;
import com.banking.engine.account.AccountResult;
import com.banking.engine.clearing.ClearingDecision;
import com.banking.engine.clearing.ClearingGateway;
import com.banking.engine.error.TransferNotFoundException;
import com.banking.engine.messaging.Topics;
import com.banking.engine.outbox.OutboxWriter;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TransferSaga {

    private static final Logger log = LoggerFactory.getLogger(TransferSaga.class);

    private final TransferRepository transfers;
    private final TransferStateMachine stateMachine;
    private final ClearingGateway clearingGateway;
    private final OutboxWriter outbox;

    public TransferSaga(
            TransferRepository transfers,
            TransferStateMachine stateMachine,
            ClearingGateway clearingGateway,
            OutboxWriter outbox) {
        this.transfers = transfers;
        this.stateMachine = stateMachine;
        this.clearingGateway = clearingGateway;
        this.outbox = outbox;
    }

    @Transactional
    public void onStart(TransferSignal signal) {
        if (!"START".equals(signal.type())) {
            throw new IllegalArgumentException("Unknown transfer signal " + signal.type());
        }
        Transfer transfer = lock(signal.transferId());
        SagaTransition transition = stateMachine.decide(transfer.getStatus(), SagaSignal.START, null, null, null);
        apply(transfer, transition);
    }

    @Transactional
    public void onResult(AccountResult result) {
        Transfer transfer = lock(result.transferId());
        SagaSignal signal = signalOf(result);
        ClearingDecision clearing = null;
        if (transfer.getStatus() == TransferStatus.DEBIT_PENDING && signal == SagaSignal.DEBIT_SUCCEEDED) {
            clearing = clearingGateway.authorize(transfer.getId(), transfer.getAmount(), transfer.getCurrency());
        }
        SagaTransition transition = stateMachine.decide(
                transfer.getStatus(),
                signal,
                clearing,
                result.failureCode(),
                result.failureReason());
        apply(transfer, transition);
    }

    private Transfer lock(UUID transferId) {
        return transfers.findByIdForUpdate(transferId).orElseThrow(() -> new TransferNotFoundException(transferId));
    }

    private void apply(Transfer transfer, SagaTransition transition) {
        if (transition.retry()) {
            log.warn("Compensation for transfer {} failed and will be retried", transfer.getId());
            throw new CompensationIncompleteException(transfer.getId());
        }
        if (transition.ignored(transfer.getStatus())) {
            log.debug("Ignoring signal for transfer {} in status {}", transfer.getId(), transfer.getStatus());
            return;
        }
        TransferStatus previous = transfer.getStatus();
        transfer.advance(transition.nextStatus(), transition.failureCode(), transition.failureReason());
        if (transition.command() != null) {
            enqueue(transfer, transition.command());
        }
        log.info("Transfer {} moved {} -> {}", transfer.getId(), previous, transfer.getStatus());
    }

    private void enqueue(Transfer transfer, AccountCommandType type) {
        UUID accountId = switch (type) {
            case DEBIT, COMPENSATE -> transfer.getSourceAccountId();
            case CREDIT -> transfer.getDestinationAccountId();
        };
        AccountCommand command = AccountCommand.of(transfer.getId(), type, accountId, transfer.getAmount(), transfer.getCurrency());
        outbox.enqueue(Topics.ACCOUNT_COMMANDS, accountId.toString(), command);
    }

    private static SagaSignal signalOf(AccountResult result) {
        return switch (result.type()) {
            case DEBIT -> result.success() ? SagaSignal.DEBIT_SUCCEEDED : SagaSignal.DEBIT_FAILED;
            case CREDIT -> result.success() ? SagaSignal.CREDIT_SUCCEEDED : SagaSignal.CREDIT_FAILED;
            case COMPENSATE -> result.success() ? SagaSignal.COMPENSATE_SUCCEEDED : SagaSignal.COMPENSATE_FAILED;
        };
    }
}
