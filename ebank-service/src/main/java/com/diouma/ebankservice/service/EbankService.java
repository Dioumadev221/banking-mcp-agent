package com.diouma.ebankservice.service;

import com.diouma.ebankservice.entities.AccountType;
import com.diouma.ebankservice.entities.AccountTransaction;
import com.diouma.ebankservice.entities.BankAccount;
import com.diouma.ebankservice.entities.OperationType;
import com.diouma.ebankservice.exception.AccountNotFoundException;
import com.diouma.ebankservice.exception.InsufficientBalanceException;
import com.diouma.ebankservice.exception.UnknownCustomerException;
import com.diouma.ebankservice.feign.CustomerRestClient;
import com.diouma.ebankservice.models.Customer;
import com.diouma.ebankservice.repository.AccountTransactionRepository;
import com.diouma.ebankservice.repository.BankAccountRepository;
import feign.FeignException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class EbankService {

    private static final Logger log = LoggerFactory.getLogger(EbankService.class);

    private final CustomerRestClient customerRestClient;
    private final BankAccountRepository accountRepository;
    private final AccountTransactionRepository transactionRepository;

    public EbankService(CustomerRestClient customerRestClient,
                        BankAccountRepository accountRepository,
                        AccountTransactionRepository transactionRepository) {
        this.customerRestClient = customerRestClient;
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
    }

    @Transactional(readOnly = true)
    public List<BankAccount> getAllBankAccounts() {
        return accountRepository.findAll();
    }

    @Transactional(readOnly = true)
    public BankAccount getAccountById(String id) {
        BankAccount account = loadAccount(id);
        account.setCustomer(fetchOwnerOrNull(account.getCustomerId()));
        return account;
    }

    @Transactional(readOnly = true)
    public BigDecimal getBalance(String id) {
        return loadAccount(id).getBalance();
    }

    @Transactional(readOnly = true)
    public List<AccountTransaction> getStatement(String id) {
        // Load first so an unknown account is a 404, not an empty statement that
        // would make a typo look like an account with no history.
        loadAccount(id);
        return transactionRepository.findByAccountIdOrderByCreatedAtDesc(id);
    }

    /**
     * Opens an account, after checking that its owner exists.
     *
     * <p>The check is the point of this method: the account lives here, the
     * customer lives in another service, and nothing but this call prevents an
     * account from being opened for somebody who does not exist.
     */
    @Transactional
    public BankAccount createAccount(AccountType type, BigDecimal openingBalance, long customerId) {
        if (openingBalance == null || openingBalance.signum() < 0) {
            throw new IllegalArgumentException("Opening balance cannot be negative, got " + openingBalance);
        }
        requireExistingCustomer(customerId);

        BankAccount account = BankAccount.builder()
                .id(UUID.randomUUID().toString())
                .type(type)
                .balance(openingBalance)
                .customerId(customerId)
                .createdAt(Instant.now())
                .build();

        BankAccount saved = accountRepository.save(account);

        // The opening balance is the first movement, so a statement is complete
        // from the very first line rather than starting from an unexplained sum.
        if (openingBalance.signum() > 0) {
            record(OperationType.DEPOSIT, saved, openingBalance, null, null);
        }
        return saved;
    }

    @Transactional
    public BankAccount deposit(String accountId, BigDecimal amount) {
        requirePositive(amount);
        BankAccount account = loadAccount(accountId);

        account.setBalance(account.getBalance().add(amount));
        BankAccount saved = accountRepository.save(account);

        record(OperationType.DEPOSIT, saved, amount, null, null);
        return saved;
    }

    @Transactional
    public BankAccount withdraw(String accountId, BigDecimal amount) {
        requirePositive(amount);
        BankAccount account = loadAccount(accountId);
        requireSufficientBalance(account, amount);

        account.setBalance(account.getBalance().subtract(amount));
        BankAccount saved = accountRepository.save(account);

        record(OperationType.WITHDRAWAL, saved, amount, null, null);
        return saved;
    }

    /**
     * Moves money from one account to another.
     *
     * <p>The whole method is a single transaction: the debit, the credit and
     * the two ledger rows either all commit or none do. A crash after the debit
     * and before the credit cannot make money vanish - the transaction rolls
     * back and the source keeps its money. The balance check is done before any
     * write, so an impossible transfer changes nothing.
     */
    @Transactional
    public void transfer(String fromAccountId, String toAccountId, BigDecimal amount) {
        requirePositive(amount);
        if (fromAccountId.equals(toAccountId)) {
            throw new IllegalArgumentException("Cannot transfer to the same account: " + fromAccountId);
        }

        BankAccount from = loadAccount(fromAccountId);
        BankAccount to = loadAccount(toAccountId);
        requireSufficientBalance(from, amount);

        from.setBalance(from.getBalance().subtract(amount));
        to.setBalance(to.getBalance().add(amount));
        accountRepository.save(from);
        accountRepository.save(to);

        // One id ties the two legs together: the debit on the source and the
        // credit on the destination are the two sides of the same transfer.
        String transferId = UUID.randomUUID().toString();
        record(OperationType.TRANSFER_OUT, from, amount, toAccountId, transferId);
        record(OperationType.TRANSFER_IN, to, amount, fromAccountId, transferId);

        log.info("Transfer {} moved {} from {} to {}", transferId, amount, fromAccountId, toAccountId);
    }

    private void record(OperationType type, BankAccount account, BigDecimal amount,
                        String counterpartyAccountId, String transferId) {
        transactionRepository.save(AccountTransaction.builder()
                .type(type)
                .amount(amount)
                .balanceAfter(account.getBalance())
                .accountId(account.getId())
                .counterpartyAccountId(counterpartyAccountId)
                .transferId(transferId)
                .createdAt(Instant.now())
                .build());
    }

    private BankAccount loadAccount(String id) {
        return accountRepository.findById(id)
                .orElseThrow(() -> new AccountNotFoundException(id));
    }

    private void requirePositive(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("Amount must be strictly positive, got " + amount);
        }
    }

    private void requireSufficientBalance(BankAccount account, BigDecimal amount) {
        if (account.getBalance().compareTo(amount) < 0) {
            throw new InsufficientBalanceException(account.getId(), account.getBalance(), amount);
        }
    }

    private void requireExistingCustomer(long customerId) {
        try {
            customerRestClient.getCustomerById(customerId);
        } catch (FeignException.NotFound notFound) {
            // Translated to a domain exception: a 404 means the customer really
            // does not exist, which is a rejected request, not a server fault.
            throw new UnknownCustomerException(customerId);
        }
    }

    /**
     * Owner details are a convenience on read, not part of the account.
     *
     * <p>If customer-service is unavailable the account is still returned
     * without them: an outage in another service should not make an account
     * that exists here look missing.
     */
    private Customer fetchOwnerOrNull(long customerId) {
        try {
            return customerRestClient.getCustomerById(customerId);
        } catch (FeignException exception) {
            log.warn("Could not fetch customer {}: {}", customerId, exception.getMessage());
            return null;
        }
    }
}
