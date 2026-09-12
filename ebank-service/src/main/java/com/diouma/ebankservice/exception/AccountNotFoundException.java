package com.diouma.ebankservice.exception;

/** Raised when an account id does not exist. */
public class AccountNotFoundException extends RuntimeException {

    public AccountNotFoundException(String id) {
        super("No account with id " + id);
    }
}
