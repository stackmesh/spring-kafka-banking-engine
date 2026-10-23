package com.banking.engine.clearing;

import com.banking.engine.CurrencyCode;
import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.cloud.client.circuitbreaker.CircuitBreaker;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Component
@Primary
public class ResilientClearingGateway implements ClearingGateway {

    private final CircuitBreaker circuitBreaker;
    private final DeterministicClearingGateway delegate;

    public ResilientClearingGateway(CircuitBreakerFactory<?, ?> circuitBreakerFactory, DeterministicClearingGateway delegate) {
        this.circuitBreaker = circuitBreakerFactory.create("clearing");
        this.delegate = delegate;
    }

    @Override
    public ClearingDecision authorize(UUID transferId, BigDecimal amount, CurrencyCode currency) {
        return circuitBreaker.run(
                () -> delegate.authorize(transferId, amount, currency),
                throwable -> {
                    throw new ClearingUnavailableException("Clearing house is unavailable", throwable);
                });
    }
}
