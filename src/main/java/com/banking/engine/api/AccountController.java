package com.banking.engine.api;

import com.banking.engine.account.AccountService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/accounts")
public class AccountController {

    private final AccountService accounts;

    public AccountController(AccountService accounts) {
        this.accounts = accounts;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AccountResponse open(@Valid @RequestBody CreateAccountRequest request) {
        return AccountResponse.from(accounts.open(request));
    }

    @GetMapping("/{accountId}")
    public AccountResponse get(@PathVariable UUID accountId) {
        return AccountResponse.from(accounts.get(accountId));
    }

    @GetMapping("/{accountId}/ledger")
    public List<LedgerEntryResponse> ledger(@PathVariable UUID accountId) {
        return accounts.ledger(accountId).stream().map(LedgerEntryResponse::from).toList();
    }

    @PostMapping("/{accountId}/freeze")
    public AccountResponse freeze(@PathVariable UUID accountId) {
        return AccountResponse.from(accounts.freeze(accountId));
    }

    @PostMapping("/{accountId}/unfreeze")
    public AccountResponse unfreeze(@PathVariable UUID accountId) {
        return AccountResponse.from(accounts.unfreeze(accountId));
    }
}
