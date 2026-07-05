package com.diouma.customerservice.repository;

import com.diouma.customerservice.entities.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerRepostory extends JpaRepository<Customer,Long> {
}
