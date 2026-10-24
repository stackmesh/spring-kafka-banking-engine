package com.banking.engine.clearing;

public record ClearingDecision(boolean approved, String code, String reason) {

    public static ClearingDecision approve() {
        return new ClearingDecision(true, null, null);
    }

    public static ClearingDecision decline(String code, String reason) {
        return new ClearingDecision(false, code, reason);
    }
}
