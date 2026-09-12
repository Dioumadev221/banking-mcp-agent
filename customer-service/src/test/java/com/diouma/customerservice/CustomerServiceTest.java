package com.diouma.customerservice;

import com.diouma.customerservice.entities.Customer;
import com.diouma.customerservice.exception.CustomerNotFoundException;
import com.diouma.customerservice.repository.CustomerRepository;
import com.diouma.customerservice.service.CustomerService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

    @Mock
    private CustomerRepository customerRepository;

    @InjectMocks
    private CustomerService customerService;

    @Test
    void getAllCustomers_returns_every_stored_customer() {
        List<Customer> stored = List.of(
                Customer.builder().id(1L).name("Diouma").email("diouma@example.com").build(),
                Customer.builder().id(2L).name("Seynabou").email("seynabou@example.com").build(),
                Customer.builder().id(3L).name("Souleymane").email("souleymane@example.com").build());

        when(customerRepository.findAll()).thenReturn(stored);

        assertThat(customerService.getAllCustomers()).hasSize(3);
    }

    @Test
    void findCustomerById_raises_a_domain_exception_when_the_customer_is_unknown() {
        // The type matters: it is what makes the API answer 404 instead of 500,
        // and what lets ebank-service reject an account for a missing customer.
        when(customerRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> customerService.findCustomerById(99L))
                .isInstanceOf(CustomerNotFoundException.class)
                .hasMessageContaining("99");
    }
}
