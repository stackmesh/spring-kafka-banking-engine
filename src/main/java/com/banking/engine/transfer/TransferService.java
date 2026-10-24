package com.banking.engine.transfer;

import com.banking.engine.Money;
import com.banking.engine.account.Account;
import com.banking.engine.account.AccountRepository;
import com.banking.engine.account.AccountStatus;
import com.banking.engine.api.CreateTransferRequest;
import com.banking.engine.error.AccountNotFoundException;
import com.banking.engine.error.IdempotencyConflictException;
import com.banking.engine.error.InvalidTransferException;
import com.banking.engine.error.TransferNotFoundException;
import com.banking.engine.messaging.Topics;
import com.banking.engine.outbox.OutboxWriter;
import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TransferService {

    private final TransferRepository transfers;
    private final AccountRepository accounts;
    private final OutboxWriter outbox;

    public TransferService(TransferRepository transfers, AccountRepository accounts, OutboxWriter outbox) {
        this.transfers = transfers;
        this.accounts = accounts;
        this.outbox = outbox;
    }

    @Transactional
    public TransferSubmission submit(String idempotencyKey, CreateTransferRequest request) {
        String key = requireKey(idempotencyKey);
        BigDecimal amount = Money.requirePositive(request.amount());
        String fingerprint = RequestFingerprint.of(
                request.sourceAccountId(),
                request.destinationAccountId(),
                amount,
                request.currency());
        return transfers.findByIdempotencyKey(key)
                .map(existing -> replay(existing, key, fingerprint))
                .orElseGet(() -> accept(key, fingerprint, amount, request));
    }

    @Transactional(readOnly = true)
    public Transfer get(UUID transferId) {
        return transfers.findById(transferId).orElseThrow(() -> new TransferNotFoundException(transferId));
    }

    private TransferSubmission replay(Transfer existing, String key, String fingerprint) {
        if (!existing.getRequestFingerprint().equals(fingerprint)) {
            throw new IdempotencyConflictException(key);
        }
        return new TransferSubmission(existing, false);
    }

    private TransferSubmission accept(String key, String fingerprint, BigDecimal amount, CreateTransferRequest request) {
        if (request.sourceAccountId().equals(request.destinationAccountId())) {
            throw new InvalidTransferException("SAME_ACCOUNT", "Source and destination accounts must be different");
        }
        Account source = accounts.findById(request.sourceAccountId())
                .orElseThrow(() -> new AccountNotFoundException(request.sourceAccountId()));
        Account destination = accounts.findById(request.destinationAccountId())
                .orElseThrow(() -> new AccountNotFoundException(request.destinationAccountId()));
        if (source.getCurrency() != request.currency() || destination.getCurrency() != request.currency()) {
            throw new InvalidTransferException("CURRENCY_MISMATCH", "Transfer currency must match both accounts");
        }
        requireActive(source, "Source");
        requireActive(destination, "Destination");
        Transfer transfer = Transfer.request(
                UUID.randomUUID(),
                key,
                fingerprint,
                source.getId(),
                destination.getId(),
                amount,
                request.currency());
        transfers.save(transfer);
        outbox.enqueue(Topics.TRANSFER_COMMANDS, transfer.getId().toString(), TransferSignal.start(transfer.getId()));
        return new TransferSubmission(transfer, true);
    }

    private static void requireActive(Account account, String role) {
        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw new InvalidTransferException(
                    "ACCOUNT_NOT_ACTIVE",
                    role + " account " + account.getAccountNumber() + " is " + account.getStatus());
        }
    }

    private static String requireKey(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank() || idempotencyKey.length() > 100) {
            throw new InvalidTransferException("INVALID_IDEMPOTENCY_KEY", "Idempotency-Key must be 1-100 characters");
        }
        return idempotencyKey.trim();
    }
}
