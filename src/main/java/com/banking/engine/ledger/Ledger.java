package com.banking.engine.ledger;

import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class Ledger {

    private final LedgerEntryRepository repository;

    public Ledger(LedgerEntryRepository repository) {
        this.repository = repository;
    }

    public LedgerEntry append(
            UUID accountId,
            UUID transferId,
            EntryType entryType,
            PostingReason postingReason,
            BigDecimal amount,
            BigDecimal balanceAfter) {
        return repository.save(LedgerEntry.post(accountId, transferId, entryType, postingReason, amount, balanceAfter));
    }
}
