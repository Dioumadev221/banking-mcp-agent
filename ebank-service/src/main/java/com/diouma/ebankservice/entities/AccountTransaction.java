package com.diouma.ebankservice.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * One immutable line in the account ledger: the statement a customer reads and
 * the audit trail the bank keeps. A row is written when a movement happens and
 * never updated afterwards.
 */
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private OperationType type;

    /** Always strictly positive; {@link #type} carries the direction. */
    @Column(precision = 19, scale = 4)
    private BigDecimal amount;

    /**
     * The account's balance once this movement was applied. Storing it makes a
     * statement self-contained: a reader sees the running balance without
     * replaying every earlier row.
     */
    @Column(precision = 19, scale = 4)
    private BigDecimal balanceAfter;

    /** The account this line belongs to. */
    private String accountId;

    /** The other account in a transfer; null for a deposit or withdrawal. */
    private String counterpartyAccountId;

    /** Shared by the two rows of one transfer; null otherwise. */
    private String transferId;

    private Instant createdAt;
}
