package com.diouma.ebankservice.exception;

import java.math.BigDecimal;

/**
 * Raised when an account does not hold enough to cover a withdrawal or the
 * debit side of a transfer.
 *
 * <p>This is a rejected request, not a fault: the account exists and the amount
 * is well formed, there is simply not enough money. It maps to 422, distinct
 * from the 400 used for a malformed amount.
 */
public class InsufficientBalanceException extends RuntimeException {

    public InsufficientBalanceException(String accountId, BigDecimal balance, BigDecimal requested) {
        super("Account " + accountId + " holds " + balance + " but " + requested + " was requested");
    }
}
