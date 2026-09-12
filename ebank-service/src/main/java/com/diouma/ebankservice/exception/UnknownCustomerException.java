package com.diouma.ebankservice.exception;

/**
 * Raised when the account owner does not exist in customer-service.
 *
 * <p>Distinct from a transport failure: a 404 from customer-service means the
 * customer is genuinely unknown and the account must be rejected, whereas a
 * connection error means the check could not be performed at all. Collapsing
 * both into one error would let an account be created — or refused — for the
 * wrong reason.
 */
public class UnknownCustomerException extends RuntimeException {

    public UnknownCustomerException(long customerId) {
        super("No customer with id " + customerId + "; the account was not created");
    }
}
