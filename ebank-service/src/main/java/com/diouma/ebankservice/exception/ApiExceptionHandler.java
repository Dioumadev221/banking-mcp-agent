package com.diouma.ebankservice.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Maps domain exceptions to HTTP statuses, in one place.
 *
 * <p>Without this, every failure surfaces as 500 and a caller cannot tell an
 * invalid request from a broken service. Responses use ProblemDetail
 * (RFC 7807), the format Spring produces for its own errors.
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(AccountNotFoundException.class)
    ProblemDetail handleAccountNotFound(AccountNotFoundException exception) {
        return problem(HttpStatus.NOT_FOUND, "Account not found", exception.getMessage());
    }

    @ExceptionHandler(UnknownCustomerException.class)
    ProblemDetail handleUnknownCustomer(UnknownCustomerException exception) {
        // 422 rather than 404: the account resource is a valid target, it is the
        // owner it refers to that does not exist.
        return problem(HttpStatus.UNPROCESSABLE_ENTITY, "Unknown customer", exception.getMessage());
    }

    /** Covers InvalidAccountTypeException and the other argument checks. */
    @ExceptionHandler(IllegalArgumentException.class)
    ProblemDetail handleInvalidArgument(IllegalArgumentException exception) {
        return problem(HttpStatus.BAD_REQUEST, "Invalid request", exception.getMessage());
    }

    private ProblemDetail problem(HttpStatus status, String title, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        return problem;
    }
}
