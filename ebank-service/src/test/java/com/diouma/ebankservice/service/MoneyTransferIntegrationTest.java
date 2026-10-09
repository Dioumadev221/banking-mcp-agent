package com.diouma.ebankservice.service;

import com.diouma.ebankservice.entities.AccountType;
import com.diouma.ebankservice.entities.AccountTransaction;
import com.diouma.ebankservice.entities.BankAccount;
import com.diouma.ebankservice.entities.OperationType;
import com.diouma.ebankservice.exception.InsufficientBalanceException;
import com.diouma.ebankservice.repository.AccountTransactionRepository;
import com.diouma.ebankservice.repository.BankAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Exercises a transfer end to end against a real PostgreSQL started in a
 * throw-away container. Accounts are inserted straight through the repository
 * so the test needs neither customer-service nor Eureka: it is about what the
 * money operations do to the database, nothing else.
 */
@SpringBootTest(properties = {
        "eureka.client.enabled=false",
        "demo.seed-accounts=false"
})
@Testcontainers
class MoneyTransferIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine");

    @Autowired
    private EbankService ebankService;

    @Autowired
    private BankAccountRepository accountRepository;

    @Autowired
    private AccountTransactionRepository transactionRepository;

    @BeforeEach
    void reset() {
        transactionRepository.deleteAll();
        accountRepository.deleteAll();
    }

    @Test
    void a_transfer_moves_money_and_leaves_a_matching_audit_trail() {
        BankAccount from = accountRepository.save(account("1000"));
        BankAccount to = accountRepository.save(account("200"));

        ebankService.transfer(from.getId(), to.getId(), new BigDecimal("300"));

        assertThat(balanceOf(from.getId())).isEqualByComparingTo("700");
        assertThat(balanceOf(to.getId())).isEqualByComparingTo("500");

        List<AccountTransaction> fromLedger = transactionRepository.findByAccountIdOrderByCreatedAtDesc(from.getId());
        List<AccountTransaction> toLedger = transactionRepository.findByAccountIdOrderByCreatedAtDesc(to.getId());

        assertThat(fromLedger).singleElement().satisfies(line -> {
            assertThat(line.getType()).isEqualTo(OperationType.TRANSFER_OUT);
            assertThat(line.getAmount()).isEqualByComparingTo("300");
            assertThat(line.getBalanceAfter()).isEqualByComparingTo("700");
            assertThat(line.getCounterpartyAccountId()).isEqualTo(to.getId());
        });
        assertThat(toLedger).singleElement().satisfies(line -> {
            assertThat(line.getType()).isEqualTo(OperationType.TRANSFER_IN);
            assertThat(line.getBalanceAfter()).isEqualByComparingTo("500");
        });

        // The two legs carry the same transfer id: that is what ties the debit
        // on one account to the credit on the other.
        assertThat(fromLedger.get(0).getTransferId())
                .isEqualTo(toLedger.get(0).getTransferId())
                .isNotNull();
    }

    @Test
    void an_overdrawing_transfer_is_rejected_and_nothing_changes() {
        BankAccount from = accountRepository.save(account("100"));
        BankAccount to = accountRepository.save(account("0"));

        assertThatThrownBy(() -> ebankService.transfer(from.getId(), to.getId(), new BigDecimal("300")))
                .isInstanceOf(InsufficientBalanceException.class);

        // Balances are untouched and no ledger row was written.
        assertThat(balanceOf(from.getId())).isEqualByComparingTo("100");
        assertThat(balanceOf(to.getId())).isEqualByComparingTo("0");
        assertThat(transactionRepository.count()).isZero();
    }

    @Test
    void money_keeps_its_exact_value_through_the_database() {
        // 0.10 + 0.20 is exactly 0.30 with BigDecimal; with double it would not
        // be. This is why the balance is numeric, and the point is worth a test.
        BankAccount from = accountRepository.save(account("0.30"));
        BankAccount to = accountRepository.save(account("0"));

        ebankService.transfer(from.getId(), to.getId(), new BigDecimal("0.10"));
        ebankService.transfer(from.getId(), to.getId(), new BigDecimal("0.20"));

        assertThat(balanceOf(from.getId())).isEqualByComparingTo("0.00");
        assertThat(balanceOf(to.getId())).isEqualByComparingTo("0.30");
    }

    private BigDecimal balanceOf(String id) {
        return accountRepository.findById(id).orElseThrow().getBalance();
    }

    private BankAccount account(String balance) {
        return BankAccount.builder()
                .id(UUID.randomUUID().toString())
                .type(AccountType.CURRENT_ACCOUNT)
                .balance(new BigDecimal(balance))
                .customerId(1L)
                .createdAt(Instant.now())
                .build();
    }
}
