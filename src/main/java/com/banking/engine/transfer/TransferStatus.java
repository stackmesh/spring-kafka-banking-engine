package com.banking.engine.transfer;

public enum TransferStatus {
    REQUESTED,
    DEBIT_PENDING,
    CREDIT_PENDING,
    COMPENSATION_PENDING,
    COMPLETED,
    ROLLED_BACK,
    FAILED
}
