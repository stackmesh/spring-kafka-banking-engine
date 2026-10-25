package com.banking.engine.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.banking.engine.CurrencyCode;
import com.banking.engine.account.AccountStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

class AccountResponseJsonTest {

    @Test
    void moneyIsSerializedAsAString() {
        AccountResponse response = new AccountResponse(
                UUID.fromString("11111111-1111-1111-1111-111111111111"),
                "BE-10000001",
                "Ada Lovelace",
                CurrencyCode.USD,
                AccountStatus.ACTIVE,
                new BigDecimal("1000.00"),
                Instant.parse("2026-01-01T00:00:00Z"));

        String json = JsonMapper.builder().build().writeValueAsString(response);

        assertThat(json).contains("\"availableBalance\":\"1000.00\"");
    }
}
