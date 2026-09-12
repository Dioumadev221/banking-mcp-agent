package com.diouma.ebankservice.service;

import com.diouma.ebankservice.entities.AccountType;
import com.diouma.ebankservice.entities.BankAccount;
import com.diouma.ebankservice.exception.AccountNotFoundException;
import com.diouma.ebankservice.exception.UnknownCustomerException;
import com.diouma.ebankservice.feign.CustomerRestClient;
import com.diouma.ebankservice.models.Customer;
import com.diouma.ebankservice.repository.BankAccountRepository;
import feign.FeignException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class EbankService {

    private static final Logger log = LoggerFactory.getLogger(EbankService.class);

    private final CustomerRestClient customerRestClient;
    private final BankAccountRepository accountRepository;

    public EbankService(CustomerRestClient customerRestClient, BankAccountRepository accountRepository) {
        this.customerRestClient = customerRestClient;
        this.accountRepository = accountRepository;
    }

    @Transactional(readOnly = true)
    public List<BankAccount> getAllBankAccounts() {
        return accountRepository.findAll();
    }

    @Transactional(readOnly = true)
    public BankAccount getAccountById(String id) {
        BankAccount account = accountRepository.findById(id)
                .orElseThrow(() -> new AccountNotFoundException(id));
        account.setCustomer(fetchOwnerOrNull(account.getCustomerId()));
        return account;
    }

    /**
     * Opens an account, after checking that its owner exists.
     *
     * <p>The check is the point of this method: the account lives here, the
     * customer lives in another service, and nothing but this call prevents an
     * account from being opened for somebody who does not exist.
     */
    @Transactional
    public BankAccount createAccount(AccountType type, double balance, long customerId) {
        if (balance < 0) {
            throw new IllegalArgumentException("Opening balance cannot be negative, got " + balance);
        }
        requireExistingCustomer(customerId);

        BankAccount account = BankAccount.builder()
                .id(UUID.randomUUID().toString())
                .type(type)
                .balance(balance)
                .customerId(customerId)
                .createdAt(Instant.now())
                .build();

        return accountRepository.save(account);
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
