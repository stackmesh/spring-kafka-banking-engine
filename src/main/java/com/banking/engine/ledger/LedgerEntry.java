package com.banking.engine.ledger;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ledger_entry")
public class LedgerEntry {

    @Id
    private UUID id;

    @Column(name = "account_id", nullable = false)
    private UUID accountId;

    @Column(name = "transfer_id")
    private UUID transferId;

    @Enumerated(EnumType.STRING)
    @Column(name = "entry_type", nullable = false, length = 16)
    private EntryType entryType;

    @Enumerated(EnumType.STRING)
    @Column(name = "posting_reason", nullable = false, length = 32)
    private PostingReason postingReason;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "balance_after", nullable = false, precision = 19, scale = 2)
    private BigDecimal balanceAfter;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected LedgerEntry() {
    }

    public static LedgerEntry post(
            UUID accountId,
            UUID transferId,
            EntryType entryType,
            PostingReason postingReason,
            BigDecimal amount,
            BigDecimal balanceAfter) {
        LedgerEntry entry = new LedgerEntry();
        entry.id = UUID.randomUUID();
        entry.accountId = accountId;
        entry.transferId = transferId;
        entry.entryType = entryType;
        entry.postingReason = postingReason;
        entry.amount = amount;
        entry.balanceAfter = balanceAfter;
        entry.createdAt = Instant.now();
        return entry;
    }

    public UUID getId() {
        return id;
    }

    public UUID getAccountId() {
        return accountId;
    }

    public UUID getTransferId() {
        return transferId;
    }

    public EntryType getEntryType() {
        return entryType;
    }

    public PostingReason getPostingReason() {
        return postingReason;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public BigDecimal getBalanceAfter() {
        return balanceAfter;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
