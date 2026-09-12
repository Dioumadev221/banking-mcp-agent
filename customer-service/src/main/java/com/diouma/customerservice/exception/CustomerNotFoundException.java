package com.diouma.customerservice.exception;

/**
 * Raised when a customer id does not exist.
 *
 * <p>A dedicated type rather than a bare RuntimeException: it is what lets the
 * exception handler answer 404 instead of 500. Callers — including
 * ebank-service through OpenFeign — rely on that status to tell "this customer
 * does not exist" apart from "the customer service is broken".
 */
public class CustomerNotFoundException extends RuntimeException {

    public CustomerNotFoundException(Long id) {
        super("No customer with id " + id);
    }
}
