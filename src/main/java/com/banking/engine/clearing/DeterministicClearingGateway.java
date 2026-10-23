package com.banking.engine.clearing;

import com.banking.engine.CurrencyCode;
import com.banking.engine.config.BankingProperties;
import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class DeterministicClearingGateway {

    static final String REJECTED = "CLEARING_REJECTED";

    private final BankingProperties properties;

    public DeterministicClearingGateway(BankingProperties properties) {
        this.properties = properties;
    }

    public ClearingDecision authorize(UUID transferId, BigDecimal amount, CurrencyCode currency) {
        return switch (properties.clearing().mode()) {
            case ALWAYS_APPROVE -> ClearingDecision.approve();
            case ALWAYS_REJECT -> ClearingDecision.decline(REJECTED, "Clearing house declined the transfer");
            case UNAVAILABLE -> throw new ClearingUnavailableException("Clearing house is unavailable");
            case DETERMINISTIC -> declines(amount)
                    ? ClearingDecision.decline(REJECTED, "Mock clearing house declines amounts ending in .13")
                    : ClearingDecision.approve();
        };
    }

    static boolean declines(BigDecimal amount) {
        return amount.movePointRight(2).intValue() % 100 == 13;
    }
}
