package com.banking.engine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.banking.engine.error.InvalidMoneyException;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class MoneyTest {

    @Test
    void scalesASingleDecimalPlace() {
        assertThat(Money.requirePositive(new BigDecimal("10.1"))).isEqualTo(new BigDecimal("10.10"));
    }

    @Test
    void rejectsMoreThanTwoDecimalPlaces() {
        assertThatThrownBy(() -> Money.requirePositive(new BigDecimal("10.123")))
                .isInstanceOf(InvalidMoneyException.class);
    }

    @Test
    void rejectsZeroAndNegativeTransfers() {
        assertThatThrownBy(() -> Money.requirePositive(BigDecimal.ZERO)).isInstanceOf(InvalidMoneyException.class);
        assertThatThrownBy(() -> Money.requirePositive(new BigDecimal("-1.00"))).isInstanceOf(InvalidMoneyException.class);
    }

    @Test
    void allowsAZeroOpeningBalance() {
        assertThat(Money.requireNonNegative(new BigDecimal("0"))).isEqualTo(new BigDecimal("0.00"));
    }
}
