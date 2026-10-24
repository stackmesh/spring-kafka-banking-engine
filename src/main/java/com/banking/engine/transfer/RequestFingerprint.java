package com.banking.engine.transfer;

import com.banking.engine.CurrencyCode;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;

public final class RequestFingerprint {

    private RequestFingerprint() {
    }

    public static String of(UUID sourceAccountId, UUID destinationAccountId, BigDecimal amount, CurrencyCode currency) {
        String raw = sourceAccountId + "|" + destinationAccountId + "|" + amount.toPlainString() + "|" + currency;
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(raw.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
