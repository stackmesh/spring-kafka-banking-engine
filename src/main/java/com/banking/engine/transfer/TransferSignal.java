package com.banking.engine.transfer;

import java.util.UUID;

public record TransferSignal(UUID transferId, String type) {

    public static TransferSignal start(UUID transferId) {
        return new TransferSignal(transferId, "START");
    }
}
