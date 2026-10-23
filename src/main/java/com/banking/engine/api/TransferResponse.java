package com.banking.engine.api;

import com.banking.engine.CurrencyCode;
import com.banking.engine.transfer.Transfer;
import com.banking.engine.transfer.TransferStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TransferResponse(
        UUID id,
        UUID sourceAccountId,
        UUID destinationAccountId,
        @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal amount,
        CurrencyCode currency,
        TransferStatus status,
        String failureCode,
        String failureReason,
        Instant createdAt,
        Instant updatedAt) {

    public static TransferResponse from(Transfer transfer) {
        return new TransferResponse(
                transfer.getId(),
                transfer.getSourceAccountId(),
                transfer.getDestinationAccountId(),
                transfer.getAmount(),
                transfer.getCurrency(),
                transfer.getStatus(),
                transfer.getFailureCode(),
                transfer.getFailureReason(),
                transfer.getCreatedAt(),
                transfer.getUpdatedAt());
    }
}
