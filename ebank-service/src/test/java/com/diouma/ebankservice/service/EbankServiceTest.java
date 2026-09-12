package com.diouma.ebankservice.service;

import com.diouma.ebankservice.entities.AccountType;
import com.diouma.ebankservice.entities.BankAccount;
import com.diouma.ebankservice.exception.AccountNotFoundException;
import com.diouma.ebankservice.exception.UnknownCustomerException;
import com.diouma.ebankservice.feign.CustomerRestClient;
import com.diouma.ebankservice.models.Customer;
import com.diouma.ebankservice.repository.BankAccountRepository;
import feign.FeignException;
import feign.Request;
import feign.RequestTemplate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EbankServiceTest {

    @Mock
    private CustomerRestClient customerRestClient;

    @Mock
    private BankAccountRepository accountRepository;

    @InjectMocks
    private EbankService ebankService;

    @Test
    void createAccount_assigns_the_id_and_the_creation_date() {
        when(customerRestClient.getCustomerById(anyLong())).thenReturn(existingCustomer());
        when(accountRepository.save(any(BankAccount.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        BankAccount created = ebankService.createAccount(AccountType.CURRENT_ACCOUNT, 5000, 1L);

        assertThat(created.getId()).isNotNull();
        assertThat(created.getCreatedAt()).isNotNull();
        assertThat(created.getType()).isEqualTo(AccountType.CURRENT_ACCOUNT);
        assertThat(created.getCustomerId()).isEqualTo(1L);
    }

    @Test
    void createAccount_refuses_an_owner_that_does_not_exist() {
        when(customerRestClient.getCustomerById(99L)).thenThrow(notFound());

        assertThatThrownBy(() -> ebankService.createAccount(AccountType.SAVING_ACCOUNT, 100, 99L))
                .isInstanceOf(UnknownCustomerException.class);

        // The point of the check: nothing reaches the database.
        verify(accountRepository, never()).save(any());
    }

    @Test
    void createAccount_refuses_a_negative_opening_balance() {
        assertThatThrownBy(() -> ebankService.createAccount(AccountType.CURRENT_ACCOUNT, -1, 1L))
                .isInstanceOf(IllegalArgumentException.class);

        // Rejected before the remote call, so an invalid request costs nothing.
        verify(customerRestClient, never()).getCustomerById(anyLong());
        verify(accountRepository, never()).save(any());
    }

    @Test
    void getAllBankAccounts_returns_every_stored_account() {
        when(accountRepository.findAll()).thenReturn(List.of(
                BankAccount.builder().id("a").type(AccountType.SAVING_ACCOUNT).balance(2000).customerId(1).build(),
                BankAccount.builder().id("b").type(AccountType.CURRENT_ACCOUNT).balance(50000).customerId(3).build()));

        assertThat(ebankService.getAllBankAccounts()).hasSize(2);
    }

    @Test
    void getAccountById_raises_a_domain_exception_when_the_account_is_unknown() {
        when(accountRepository.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ebankService.getAccountById("missing"))
                .isInstanceOf(AccountNotFoundException.class);
    }

    @Test
    void getAccountById_still_returns_the_account_when_customer_service_is_down() {
        BankAccount stored = BankAccount.builder()
                .id("acc-1").type(AccountType.CURRENT_ACCOUNT).balance(500).customerId(7).build();
        when(accountRepository.findById("acc-1")).thenReturn(Optional.of(stored));
        when(customerRestClient.getCustomerById(7L)).thenThrow(notFound());

        BankAccount account = ebankService.getAccountById("acc-1");

        // An outage elsewhere must not make an account that exists look missing.
        assertThat(account.getId()).isEqualTo("acc-1");
        assertThat(account.getCustomer()).isNull();
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
