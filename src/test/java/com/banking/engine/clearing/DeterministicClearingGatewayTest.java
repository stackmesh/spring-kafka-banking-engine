package com.banking.engine.clearing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.banking.engine.CurrencyCode;
import com.banking.engine.config.BankingProperties;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class DeterministicClearingGatewayTest {

    @Test
    void declinesAmountsEndingInThirteenCents() {
        DeterministicClearingGateway gateway = gateway(ClearingMode.DETERMINISTIC);

        assertThat(gateway.authorize(UUID.randomUUID(), new BigDecimal("10.13"), CurrencyCode.USD).approved()).isFalse();
        assertThat(gateway.authorize(UUID.randomUUID(), new BigDecimal("10.12"), CurrencyCode.USD).approved()).isTrue();
    }

    @Test
    void unavailableModeThrowsInsteadOfApproving() {
        DeterministicClearingGateway gateway = gateway(ClearingMode.UNAVAILABLE);

        assertThatThrownBy(() -> gateway.authorize(UUID.randomUUID(), new BigDecimal("5.00"), CurrencyCode.EUR))
                .isInstanceOf(ClearingUnavailableException.class);
    }

    private static DeterministicClearingGateway gateway(ClearingMode mode) {
        BankingProperties properties = new BankingProperties(
                new BankingProperties.Outbox(Duration.ofMillis(250), Duration.ofSeconds(2), 50, Duration.ofSeconds(30), 8),
                new BankingProperties.Clearing(mode),
                new BankingProperties.Messaging(3, 1));
        return new DeterministicClearingGateway(properties);
    }
}
