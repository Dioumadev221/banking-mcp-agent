package com.diouma.ebankservice.exception;

import java.util.Arrays;

import com.diouma.ebankservice.entities.AccountType;

/**
 * Raised when the requested account type is not one this bank opens.
 *
 * <p>The message lists the accepted values on purpose: it is returned to the
 * agent as the tool result, so the model can correct itself instead of failing
 * silently.
 */
public class InvalidAccountTypeException extends IllegalArgumentException {

    public InvalidAccountTypeException(String raw) {
        super("Unknown account type '" + raw + "'. Expected one of "
                + Arrays.stream(AccountType.values()).map(AccountType::wireName).toList());
    }
}
