package com.banking.engine.messaging;

import com.banking.engine.account.AccountResult;
import com.banking.engine.transfer.TransferSaga;
import com.banking.engine.transfer.TransferSignal;
import org.slf4j.MDC;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

@Component
public class TransferListener {

    private final JsonMapper json;
    private final TransferSaga saga;

    public TransferListener(JsonMapper json, TransferSaga saga) {
        this.json = json;
        this.saga = saga;
    }

    @KafkaListener(topics = Topics.TRANSFER_COMMANDS)
    public void onStart(String payload) {
        TransferSignal signal = json.readValue(payload, TransferSignal.class);
        try (MDC.MDCCloseable ignored = MDC.putCloseable("transferId", signal.transferId().toString())) {
            saga.onStart(signal);
        }
    }

    @KafkaListener(topics = Topics.ACCOUNT_RESULTS)
    public void onResult(String payload) {
        AccountResult result = json.readValue(payload, AccountResult.class);
        try (MDC.MDCCloseable ignored = MDC.putCloseable("transferId", result.transferId().toString())) {
            saga.onResult(result);
        }
    }
}
