package com.banking.engine.transfer;

import com.banking.engine.CurrencyCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "transfer")
public class Transfer {

    @Id
    private UUID id;

    @Column(name = "idempotency_key", nullable = false, unique = true, length = 100)
    private String idempotencyKey;

    @Column(name = "request_fingerprint", nullable = false, length = 64)
    private String requestFingerprint;

    @Column(name = "source_account_id", nullable = false)
    private UUID sourceAccountId;

    @Column(name = "destination_account_id", nullable = false)
    private UUID destinationAccountId;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 3)
    private CurrencyCode currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private TransferStatus status;

    @Column(name = "failure_code", length = 64)
    private String failureCode;

    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(nullable = false)
    private long version;

    protected Transfer() {
    }

    public static Transfer request(
            UUID id,
            String idempotencyKey,
            String requestFingerprint,
            UUID sourceAccountId,
            UUID destinationAccountId,
            BigDecimal amount,
            CurrencyCode currency) {
        Instant now = Instant.now();
        Transfer transfer = new Transfer();
        transfer.id = id;
        transfer.idempotencyKey = idempotencyKey;
        transfer.requestFingerprint = requestFingerprint;
        transfer.sourceAccountId = sourceAccountId;
        transfer.destinationAccountId = destinationAccountId;
        transfer.amount = amount;
        transfer.currency = currency;
        transfer.status = TransferStatus.REQUESTED;
        transfer.createdAt = now;
        transfer.updatedAt = now;
        return transfer;
    }

    public void advance(TransferStatus nextStatus, String failureCode, String failureReason) {
        this.status = nextStatus;
        if (failureCode != null) {
            this.failureCode = failureCode;
            this.failureReason = truncate(failureReason);
        }
        this.updatedAt = Instant.now();
    }

    private static String truncate(String reason) {
        if (reason == null || reason.length() <= 500) {
            return reason;
        }
        return reason.substring(0, 500);
    }

    public UUID getId() {
        return id;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public String getRequestFingerprint() {
        return requestFingerprint;
    }

    public UUID getSourceAccountId() {
        return sourceAccountId;
    }

    public UUID getDestinationAccountId() {
        return destinationAccountId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public CurrencyCode getCurrency() {
        return currency;
    }

    public TransferStatus getStatus() {
        return status;
    }

    public String getFailureCode() {
        return failureCode;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public long getVersion() {
        return version;
    }
}
