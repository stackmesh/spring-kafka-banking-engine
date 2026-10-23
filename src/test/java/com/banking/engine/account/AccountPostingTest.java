package com.banking.engine.account;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;

import com.banking.engine.CurrencyCode;
import com.banking.engine.error.AccountPostingException;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AccountPostingTest {

    @Test
    void debitReducesTheAvailableBalance() {
        Account account = open("100.00");

        account.applyDebit(new BigDecimal("40.00"));

        assertThat(account.getAvailableBalance()).isEqualByComparingTo("60.00");
    }

    @Test
    void debitNeverOverdraws() {
        Account account = open("25.00");

        Throwable thrown = catchThrowable(() -> account.applyDebit(new BigDecimal("25.01")));
        assertThat(thrown).isInstanceOf(AccountPostingException.class);
        assertThat(((AccountPostingException) thrown).code()).isEqualTo("INSUFFICIENT_FUNDS");
        assertThat(account.getAvailableBalance()).isEqualByComparingTo("25.00");
    }

    @Test
    void frozenAccountRejectsDebitsAndCreditsButAcceptsCompensation() {
        Account account = open("80.00");
        account.freeze();

        assertThatThrownBy(() -> account.applyDebit(new BigDecimal("10.00")))
                .isInstanceOf(AccountPostingException.class);
        assertThatThrownBy(() -> account.applyCredit(new BigDecimal("10.00"), false))
                .isInstanceOf(AccountPostingException.class);

        account.applyCredit(new BigDecimal("10.00"), true);

        assertThat(account.getAvailableBalance()).isEqualByComparingTo("90.00");
    }

    private static Account open(String balance) {
        return Account.open(UUID.randomUUID(), "BE-10000001", "Ada Lovelace", CurrencyCode.USD, new BigDecimal(balance));
    }
}
