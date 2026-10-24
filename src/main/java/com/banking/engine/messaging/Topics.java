package com.banking.engine.messaging;

public final class Topics {

    public static final String TRANSFER_COMMANDS = "banking.transfer.commands";
    public static final String ACCOUNT_COMMANDS = "banking.account.commands";
    public static final String ACCOUNT_RESULTS = "banking.account.results";
    public static final String DLT_SUFFIX = ".dlt";

    private Topics() {
    }
}
