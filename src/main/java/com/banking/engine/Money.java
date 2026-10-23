package com.banking.engine;

import com.banking.engine.error.InvalidMoneyException;
import java.math.BigDecimal;
import java.math.RoundingMode;

public final class Money {

    private Money() {
    }

    public static BigDecimal scale(BigDecimal amount) {
        if (amount == null) {
            throw new InvalidMoneyException("Amount is required");
        }
        try {
            return amount.setScale(2, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException ex) {
            throw new InvalidMoneyException("Amount must have at most 2 decimal places");
        }
    }

    public static BigDecimal requirePositive(BigDecimal amount) {
        BigDecimal scaled = scale(amount);
        if (scaled.signum() <= 0) {
            throw new InvalidMoneyException("Amount must be greater than zero");
        }
        return scaled;
    }

    public static BigDecimal requireNonNegative(BigDecimal amount) {
        BigDecimal scaled = scale(amount);
        if (scaled.signum() < 0) {
            throw new InvalidMoneyException("Amount must be zero or greater");
        }
        return scaled;
    }
}
