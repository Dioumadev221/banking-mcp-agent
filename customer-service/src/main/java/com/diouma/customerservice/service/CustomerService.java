package com.diouma.customerservice.service;

import com.diouma.customerservice.entities.Customer;
import com.diouma.customerservice.repository.CustomerRepostory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CustomerService {
    private CustomerRepostory customerRepostory;

    public CustomerService(CustomerRepostory customerRepostory) {
        this.customerRepostory = customerRepostory;
    }
    public List<Customer> getAllCustomer() {
        return customerRepostory.findAll();
    }

    public Customer findCustomerById(Long id) {
        return customerRepostory.findById(id)
                .orElseThrow(()->new RuntimeException("Customer Not Found"));
    }
    public Customer saveCustomer(Customer customer){
        return customerRepostory.save(customer);
    }
}
