package com.diouma.ebankservice.entities;

import com.diouma.ebankservice.models.Customer;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Transient;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BankAccount {

    @Id
    private String id;

    /**
     * Money is BigDecimal, never double. A double cannot represent 0.10
     * exactly, so sums drift; on an account balance that is a bug, not a
     * rounding detail. The column is numeric(19,4) to match.
     */
    @Column(precision = 19, scale = 4)
    private BigDecimal balance;

    @Enumerated(EnumType.STRING)
    private AccountType type;

    private Instant createdAt;

    private long customerId;

    /**
     * Optimistic locking. Hibernate stamps every update with this version and
     * refuses one whose version is stale, so two operations that read the same
     * balance and both try to write cannot silently lose one of the changes:
     * the second fails and can be retried. No row is locked while a caller
     * thinks, which a pessimistic lock would do.
     */
    @Version
    private Long version;

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
