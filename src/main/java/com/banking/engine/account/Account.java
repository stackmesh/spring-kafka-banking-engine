package com.banking.engine.account;

import com.banking.engine.CurrencyCode;
import com.banking.engine.error.AccountPostingException;
import com.banking.engine.error.InvalidAccountStateException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "account")
public class Account {

    @Id
    private UUID id;

    @Column(name = "account_number", nullable = false, unique = true, length = 32)
    private String accountNumber;

    @Column(name = "holder_name", nullable = false, length = 120)
    private String holderName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 3)
    private CurrencyCode currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private AccountStatus status;

    @Column(name = "available_balance", nullable = false, precision = 19, scale = 2)
    private BigDecimal availableBalance;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Version
    @Column(nullable = false)
    private long version;

    protected Account() {
    }

    public static Account open(
            UUID id,
            String accountNumber,
            String holderName,
            CurrencyCode currency,
            BigDecimal openingBalance) {
        Account account = new Account();
        account.id = id;
        account.accountNumber = accountNumber;
        account.holderName = holderName;
        account.currency = currency;
        account.status = AccountStatus.ACTIVE;
        account.availableBalance = openingBalance;
        account.createdAt = Instant.now();
        return account;
    }

    public void applyDebit(BigDecimal amount) {
        requireActive("debited");
        if (availableBalance.compareTo(amount) < 0) {
            throw new AccountPostingException(
                    "INSUFFICIENT_FUNDS",
                    "Account " + accountNumber + " has insufficient funds");
        }
        availableBalance = availableBalance.subtract(amount);
    }

    public void applyCredit(BigDecimal amount, boolean compensation) {
        if (status == AccountStatus.CLOSED || (!compensation && status != AccountStatus.ACTIVE)) {
            String code = status == AccountStatus.CLOSED ? "ACCOUNT_CLOSED" : "ACCOUNT_FROZEN";
            throw new AccountPostingException(code, "Account " + accountNumber + " cannot accept a credit");
        }
        availableBalance = availableBalance.add(amount);
    }

    public void freeze() {
        if (status == AccountStatus.CLOSED) {
            throw new InvalidAccountStateException("Closed account " + accountNumber + " cannot be frozen");
        }
        status = AccountStatus.FROZEN;
    }

    public void unfreeze() {
        if (status != AccountStatus.FROZEN) {
            throw new InvalidAccountStateException("Account " + accountNumber + " is not frozen");
        }
        status = AccountStatus.ACTIVE;
    }

    private void requireActive(String action) {
        if (status != AccountStatus.ACTIVE) {
            String code = status == AccountStatus.CLOSED ? "ACCOUNT_CLOSED" : "ACCOUNT_FROZEN";
            throw new AccountPostingException(code, "Account " + accountNumber + " cannot be " + action);
        }
    }

    public UUID getId() {
        return id;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public String getHolderName() {
        return holderName;
    }

    public CurrencyCode getCurrency() {
        return currency;
    }

    public AccountStatus getStatus() {
        return status;
    }

    public BigDecimal getAvailableBalance() {
        return availableBalance;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public long getVersion() {
        return version;
    }
}
