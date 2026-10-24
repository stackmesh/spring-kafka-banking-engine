package com.banking.engine.api;

import com.banking.engine.CurrencyCode;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record CreateAccountRequest(
        @NotBlank @Size(max = 120) String holderName,
        @NotNull CurrencyCode currency,
        @NotNull @PositiveOrZero @Digits(integer = 17, fraction = 2) BigDecimal openingBalance) {
}
