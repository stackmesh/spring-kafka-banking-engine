package com.banking.engine.transfer;

public enum SagaSignal {
    START,
    DEBIT_SUCCEEDED,
    DEBIT_FAILED,
    CREDIT_SUCCEEDED,
    CREDIT_FAILED,
    COMPENSATE_SUCCEEDED,
    COMPENSATE_FAILED
}
