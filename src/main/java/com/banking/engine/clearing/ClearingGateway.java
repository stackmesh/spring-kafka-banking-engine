package com.banking.engine.clearing;

import com.banking.engine.CurrencyCode;
import java.math.BigDecimal;
import java.util.UUID;

public interface ClearingGateway {

    ClearingDecision authorize(UUID transferId, BigDecimal amount, CurrencyCode currency);
}
