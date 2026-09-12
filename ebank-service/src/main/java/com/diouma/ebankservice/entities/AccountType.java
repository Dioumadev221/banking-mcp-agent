package com.diouma.ebankservice.entities;

import com.diouma.ebankservice.exception.InvalidAccountTypeException;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Locale;

/**
 * The kinds of account this bank opens.
 *
 * <p>Storing the type as an enum rather than a free String is what stops the
 * agent from persisting whatever the model happened to write.
 */
public enum AccountType {

    CURRENT_ACCOUNT,
    SAVING_ACCOUNT;

    /**
     * Parses a caller-supplied type, tolerating the shapes a language model
     * actually produces ("current-account", "SAVING ACCOUNT", trailing spaces)
     * and rejecting everything else.
     *
     * <p>Lenient on input, strict on storage: being rigid here would make the
     * tool fail on harmless spelling, while accepting anything would put
     * unusable rows in the database. Rejection is explicit so the model can
     * read the error and retry with a valid value.
     */
    @JsonCreator
    public static AccountType from(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new InvalidAccountTypeException(raw);
        }
        String normalised = raw.trim()
                .toUpperCase(Locale.ROOT)
                .replace('-', '_')
                .replace(' ', '_');

        for (AccountType type : values()) {
            if (type.name().equals(normalised)) {
                return type;
            }
        }
        throw new InvalidAccountTypeException(raw);
    }

    /** Keeps the hyphenated form the REST API and the MCP tool document. */
    @JsonValue
    public String wireName() {
        return name().replace('_', '-');
    }
}
