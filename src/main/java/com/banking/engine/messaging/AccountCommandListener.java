package com.banking.engine.messaging;

import com.banking.engine.account.AccountCommand;
import com.banking.engine.account.AccountCommandHandler;
import org.slf4j.MDC;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

@Component
public class AccountCommandListener {

    private final JsonMapper json;
    private final AccountCommandHandler handler;

    public AccountCommandListener(JsonMapper json, AccountCommandHandler handler) {
        this.json = json;
        this.handler = handler;
    }

    @KafkaListener(topics = Topics.ACCOUNT_COMMANDS)
    public void onCommand(String payload) {
        AccountCommand command = json.readValue(payload, AccountCommand.class);
        try (MDC.MDCCloseable ignored = MDC.putCloseable("transferId", command.transferId().toString())) {
            handler.handle(command);
        }
    }
}
