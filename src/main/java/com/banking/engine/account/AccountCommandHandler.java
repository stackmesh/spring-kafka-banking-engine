package com.banking.engine.account;

import com.banking.engine.CurrencyCode;
import com.banking.engine.Money;
import com.banking.engine.error.AccountNotFoundException;
import com.banking.engine.error.AccountPostingException;
import com.banking.engine.inbox.ProcessedCommand;
import com.banking.engine.inbox.ProcessedCommandRepository;
import com.banking.engine.ledger.EntryType;
import com.banking.engine.ledger.Ledger;
import com.banking.engine.ledger.PostingReason;
import com.banking.engine.messaging.Topics;
import com.banking.engine.outbox.OutboxWriter;
import java.math.BigDecimal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountCommandHandler {

    private static final Logger log = LoggerFactory.getLogger(AccountCommandHandler.class);

    private final AccountRepository accounts;
    private final ProcessedCommandRepository processedCommands;
    private final Ledger ledger;
    private final OutboxWriter outbox;

    public AccountCommandHandler(
            AccountRepository accounts,
            ProcessedCommandRepository processedCommands,
            Ledger ledger,
            OutboxWriter outbox) {
        this.accounts = accounts;
        this.processedCommands = processedCommands;
        this.ledger = ledger;
        this.outbox = outbox;
    }

    @Transactional
    public void handle(AccountCommand command) {
        if (processedCommands.existsById(command.commandId())) {
            log.info("Ignoring duplicate {} command {} for transfer {}", command.type(), command.commandId(), command.transferId());
            return;
        }
        AccountResult result;
        try {
            result = post(command);
        } catch (AccountPostingException | AccountNotFoundException ex) {
            result = AccountResult.failure(command, ex.code(), ex.getMessage());
            log.info("Posting {} for transfer {} rejected: {}", command.type(), command.transferId(), ex.getMessage());
        }
        processedCommands.save(ProcessedCommand.of(command.commandId()));
        outbox.enqueue(Topics.ACCOUNT_RESULTS, command.transferId().toString(), result);
    }

    private AccountResult post(AccountCommand command) {
        Account account = accounts.findByIdForUpdate(command.accountId())
                .orElseThrow(() -> new AccountNotFoundException(command.accountId()));
        if (account.getCurrency() != CurrencyCode.valueOf(command.currency())) {
            throw new AccountPostingException("CURRENCY_MISMATCH", "Account currency does not match the command");
        }
        BigDecimal amount = Money.requirePositive(command.amount());
        switch (command.type()) {
            case DEBIT -> {
                account.applyDebit(amount);
                ledger.append(account.getId(), command.transferId(), EntryType.DEBIT, PostingReason.TRANSFER_DEBIT, amount, account.getAvailableBalance());
            }
            case CREDIT -> {
                account.applyCredit(amount, false);
                ledger.append(account.getId(), command.transferId(), EntryType.CREDIT, PostingReason.TRANSFER_CREDIT, amount, account.getAvailableBalance());
            }
            case COMPENSATE -> {
                account.applyCredit(amount, true);
                ledger.append(account.getId(), command.transferId(), EntryType.CREDIT, PostingReason.COMPENSATION, amount, account.getAvailableBalance());
            }
        }
        log.info("Posted {} {} {} on account {}", command.type(), amount.toPlainString(), command.currency(), account.getAccountNumber());
        return AccountResult.success(command);
    }
}
