package com.diouma.ebankservice.controller;

import com.diouma.ebankservice.entities.AccountType;

/**
 * What a caller may send when opening an account.
 *
 * <p>A dedicated request type rather than the entity: accepting BankAccount
 * directly would let a caller choose the account id and the creation date,
 * which the service alone is allowed to set.
 */
public record CreateAccountRequest(AccountType type, double balance, long customerId) {
}
