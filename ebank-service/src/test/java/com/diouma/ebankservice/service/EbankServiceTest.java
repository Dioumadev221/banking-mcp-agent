package com.diouma.ebankservice.service;

import com.diouma.ebankservice.entities.AccountType;
import com.diouma.ebankservice.entities.BankAccount;
import com.diouma.ebankservice.exception.AccountNotFoundException;
import com.diouma.ebankservice.exception.InsufficientBalanceException;
import com.diouma.ebankservice.exception.UnknownCustomerException;
import com.diouma.ebankservice.feign.CustomerRestClient;
import com.diouma.ebankservice.models.Customer;
import com.diouma.ebankservice.repository.AccountTransactionRepository;
import com.diouma.ebankservice.repository.BankAccountRepository;
import feign.FeignException;
import feign.Request;
import feign.RequestTemplate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EbankServiceTest {

    @Mock
    private CustomerRestClient customerRestClient;

    @Mock
    private BankAccountRepository accountRepository;

    @Mock
    private AccountTransactionRepository transactionRepository;

    @InjectMocks
    private EbankService ebankService;

    @Test
    void createAccount_assigns_the_id_and_the_creation_date() {
        when(customerRestClient.getCustomerById(anyLong())).thenReturn(existingCustomer());
        when(accountRepository.save(any(BankAccount.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        BankAccount created = ebankService.createAccount(AccountType.CURRENT_ACCOUNT, money(5000), 1L);

        assertThat(created.getId()).isNotNull();
        assertThat(created.getCreatedAt()).isNotNull();
        assertThat(created.getType()).isEqualTo(AccountType.CURRENT_ACCOUNT);
        assertThat(created.getCustomerId()).isEqualTo(1L);
    }

    @Test
    void createAccount_refuses_an_owner_that_does_not_exist() {
        when(customerRestClient.getCustomerById(99L)).thenThrow(notFound());

        assertThatThrownBy(() -> ebankService.createAccount(AccountType.SAVING_ACCOUNT, money(100), 99L))
                .isInstanceOf(UnknownCustomerException.class);

        // The point of the check: nothing reaches the database.
        verify(accountRepository, never()).save(any());
    }

    @Test
    void createAccount_refuses_a_negative_opening_balance() {
        assertThatThrownBy(() -> ebankService.createAccount(AccountType.CURRENT_ACCOUNT, money(-1), 1L))
                .isInstanceOf(IllegalArgumentException.class);

        // Rejected before the remote call, so an invalid request costs nothing.
        verify(customerRestClient, never()).getCustomerById(anyLong());
        verify(accountRepository, never()).save(any());
    }

    @Test
    void deposit_increases_the_balance_and_records_the_movement() {
        BankAccount account = account("acc", 100);
        when(accountRepository.findById("acc")).thenReturn(Optional.of(account));
        when(accountRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        ebankService.deposit("acc", money(50));

        assertThat(account.getBalance()).isEqualByComparingTo("150");
        verify(transactionRepository).save(any());
    }

    @Test
    void deposit_refuses_a_non_positive_amount() {
        assertThatThrownBy(() -> ebankService.deposit("acc", BigDecimal.ZERO))
                .isInstanceOf(IllegalArgumentException.class);

        // Rejected before the account is even loaded.
        verify(accountRepository, never()).findById(anyString());
    }

    @Test
    void withdraw_decreases_the_balance_when_funds_are_sufficient() {
        BankAccount account = account("acc", 500);
        when(accountRepository.findById("acc")).thenReturn(Optional.of(account));
        when(accountRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        ebankService.withdraw("acc", money(200));

        assertThat(account.getBalance()).isEqualByComparingTo("300");
        verify(transactionRepository).save(any());
    }

    @Test
    void withdraw_refuses_when_the_balance_is_too_low() {
        BankAccount account = account("acc", 100);
        when(accountRepository.findById("acc")).thenReturn(Optional.of(account));

        assertThatThrownBy(() -> ebankService.withdraw("acc", money(150)))
                .isInstanceOf(InsufficientBalanceException.class);

        // Nothing is written and the balance is left untouched.
        verify(accountRepository, never()).save(any());
        verify(transactionRepository, never()).save(any());
        assertThat(account.getBalance()).isEqualByComparingTo("100");
    }

    @Test
    void transfer_moves_money_and_records_both_legs() {
        BankAccount from = account("from", 1000);
        BankAccount to = account("to", 200);
        when(accountRepository.findById("from")).thenReturn(Optional.of(from));
        when(accountRepository.findById("to")).thenReturn(Optional.of(to));
        when(accountRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        ebankService.transfer("from", "to", money(300));

        assertThat(from.getBalance()).isEqualByComparingTo("700");
        assertThat(to.getBalance()).isEqualByComparingTo("500");
        // One row per account: the debit and the credit.
        verify(transactionRepository, times(2)).save(any());
    }

    @Test
    void transfer_refuses_when_the_source_balance_is_too_low() {
        BankAccount from = account("from", 100);
        BankAccount to = account("to", 0);
        when(accountRepository.findById("from")).thenReturn(Optional.of(from));
        when(accountRepository.findById("to")).thenReturn(Optional.of(to));

        assertThatThrownBy(() -> ebankService.transfer("from", "to", money(300)))
                .isInstanceOf(InsufficientBalanceException.class);

        // The check runs before any write, so an impossible transfer changes nothing.
        verify(accountRepository, never()).save(any());
        verify(transactionRepository, never()).save(any());
        assertThat(from.getBalance()).isEqualByComparingTo("100");
        assertThat(to.getBalance()).isEqualByComparingTo("0");
    }

    @Test
    void transfer_refuses_the_same_account_on_both_sides() {
        assertThatThrownBy(() -> ebankService.transfer("acc", "acc", money(10)))
                .isInstanceOf(IllegalArgumentException.class);

        verify(accountRepository, never()).findById(anyString());
        verify(accountRepository, never()).save(any());
    }

    @Test
    void getAccountById_raises_a_domain_exception_when_the_account_is_unknown() {
        when(accountRepository.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ebankService.getAccountById("missing"))
                .isInstanceOf(AccountNotFoundException.class);
    }

    @Test
    void getAccountById_still_returns_the_account_when_customer_service_is_down() {
        BankAccount stored = account("acc-1", 500);
        stored.setCustomerId(7);
        when(accountRepository.findById("acc-1")).thenReturn(Optional.of(stored));
        when(customerRestClient.getCustomerById(7L)).thenThrow(notFound());

        BankAccount account = ebankService.getAccountById("acc-1");

        // An outage elsewhere must not make an account that exists look missing.
        assertThat(account.getId()).isEqualTo("acc-1");
        assertThat(account.getCustomer()).isNull();
    }

    private BankAccount account(String id, long balance) {
        return BankAccount.builder()
                .id(id)
                .type(AccountType.CURRENT_ACCOUNT)
                .balance(money(balance))
                .customerId(1L)
                .build();
    }

    private BigDecimal money(long amount) {
        return BigDecimal.valueOf(amount);
    }

    private Customer existingCustomer() {
        return Customer.builder().id(1L).name("Diouma").email("diouma@example.com").build();
    }

    private FeignException.NotFound notFound() {
        Request request = Request.create(Request.HttpMethod.GET, "/customers",
                Map.of(), null, StandardCharsets.UTF_8, new RequestTemplate());
        return new FeignException.NotFound("not found", request, null, Map.of());
    }
}
