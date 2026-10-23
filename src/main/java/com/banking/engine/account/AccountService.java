package com.banking.engine.account;

import com.banking.engine.Money;
import com.banking.engine.api.CreateAccountRequest;
import com.banking.engine.error.AccountNotFoundException;
import com.banking.engine.ledger.EntryType;
import com.banking.engine.ledger.Ledger;
import com.banking.engine.ledger.LedgerEntry;
import com.banking.engine.ledger.LedgerEntryRepository;
import com.banking.engine.ledger.PostingReason;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountService {

    private final AccountRepository accounts;
    private final AccountNumberGenerator accountNumbers;
    private final Ledger ledger;
    private final LedgerEntryRepository ledgerEntries;

    public AccountService(
            AccountRepository accounts,
            AccountNumberGenerator accountNumbers,
            Ledger ledger,
            LedgerEntryRepository ledgerEntries) {
        this.accounts = accounts;
        this.accountNumbers = accountNumbers;
        this.ledger = ledger;
        this.ledgerEntries = ledgerEntries;
    }

    @Transactional
    public Account open(CreateAccountRequest request) {
        BigDecimal openingBalance = Money.requireNonNegative(request.openingBalance());
        Account account = Account.open(
                UUID.randomUUID(),
                accountNumbers.next(),
                request.holderName().trim(),
                request.currency(),
                openingBalance);
        accounts.save(account);
        if (openingBalance.signum() > 0) {
            ledger.append(account.getId(), null, EntryType.CREDIT, PostingReason.OPENING_BALANCE, openingBalance, openingBalance);
        }
        return account;
    }

    @Transactional(readOnly = true)
    public Account get(UUID accountId) {
        return accounts.findById(accountId).orElseThrow(() -> new AccountNotFoundException(accountId));
    }

    @Transactional(readOnly = true)
    public List<LedgerEntry> ledger(UUID accountId) {
        get(accountId);
        return ledgerEntries.findByAccountIdOrderByCreatedAtAsc(accountId);
    }

    @Transactional
    public Account freeze(UUID accountId) {
        Account account = accounts.findByIdForUpdate(accountId).orElseThrow(() -> new AccountNotFoundException(accountId));
        account.freeze();
        return account;
    }

    @Transactional
    public Account unfreeze(UUID accountId) {
        Account account = accounts.findByIdForUpdate(accountId).orElseThrow(() -> new AccountNotFoundException(accountId));
        account.unfreeze();
        return account;
    }
}
