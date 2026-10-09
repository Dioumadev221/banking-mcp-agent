package com.diouma.ebankservice.controller;

import com.diouma.ebankservice.entities.AccountType;

import java.math.BigDecimal;

/**
 * What a caller may send when opening an account.
 *
 * <p>A dedicated request type rather than the entity: accepting BankAccount
 * directly would let a caller choose the account id and the creation date,
 * which the service alone is allowed to set.
 *
 * <p>The balance is a BigDecimal: a REST caller is a program and can send an
 * exact decimal, which Jackson parses without the rounding a double would add.
 */
public record CreateAccountRequest(AccountType type, BigDecimal balance, long customerId) {
}
