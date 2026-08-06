package com.diouma.customerservice;

import com.diouma.customerservice.entities.Customer;
import com.diouma.customerservice.repository.CustomerRepostory;
import com.diouma.customerservice.service.CustomerService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CustomerServiceTest {
    @Mock
    private CustomerRepostory customerRepostory;

    @InjectMocks
    private CustomerService customerService;

    @Test
    void allCutomer(){
        List<Customer> customers = new ArrayList<>();
        Customer fauxClient1 = Customer.builder().id(1L).name("Diouma").email("diouma@gmail.com").build();
        Customer fauxClient2 = Customer.builder().id(2L).name("Seynabou").email("nabou@gmail.com").build();
        Customer fauxClient3 = Customer.builder().id(3L).name("Souleymana").email("diallo@gmail.com").build();

        customers.add(fauxClient1 );
        customers.add(fauxClient2 );
        customers.add(fauxClient3 );

        when(customerRepostory.findAll()).thenReturn(customers);

        List<Customer> customersListe = customerService.getAllCustomer();
        assertThat(customersListe).isNotNull();
        assertThat(customersListe).hasSize(3);
    }
}
