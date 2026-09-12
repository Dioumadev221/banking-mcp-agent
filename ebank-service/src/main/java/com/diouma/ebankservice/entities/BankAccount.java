package com.diouma.ebankservice.entities;

import com.diouma.ebankservice.models.Customer;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Transient;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BankAccount {

    @Id
    private String id;

    private double balance;

    @Enumerated(EnumType.STRING)
    private AccountType type;

    private Instant createdAt;

    private long customerId;

    /**
     * Owner details, fetched from customer-service on read.
     *
     * <p>Not persisted: this service owns accounts, not customers. Storing a
     * copy would let the two databases drift apart with no way to tell which
     * one is right.
     */
    @Transient
    private Customer customer;
}
