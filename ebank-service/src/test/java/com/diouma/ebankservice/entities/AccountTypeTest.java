package com.diouma.ebankservice.entities;

import com.diouma.ebankservice.exception.InvalidAccountTypeException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * These cases are the guardrail between a language model and the database.
 * The accepted spellings are the ones a model actually produces; everything
 * else must be refused rather than stored.
 */
class AccountTypeTest {

    @ParameterizedTest
    @ValueSource(strings = {"CURRENT-ACCOUNT", "current-account", "CURRENT_ACCOUNT", "  Current-Account  "})
    void accepts_the_spellings_a_model_produces(String raw) {
        assertThat(AccountType.from(raw)).isEqualTo(AccountType.CURRENT_ACCOUNT);
    }

    @ParameterizedTest
    @ValueSource(strings = {"compte courant", "CURRENT", "checking", "SAVINGS", "", "   "})
    void refuses_anything_else(String raw) {
        assertThatThrownBy(() -> AccountType.from(raw))
                .isInstanceOf(InvalidAccountTypeException.class);
    }

    @Test
    void refuses_null() {
        assertThatThrownBy(() -> AccountType.from(null))
                .isInstanceOf(InvalidAccountTypeException.class);
    }

    @Test
    void the_rejection_message_lists_the_valid_values_so_the_model_can_retry() {
        assertThatThrownBy(() -> AccountType.from("checking"))
                .hasMessageContaining("CURRENT-ACCOUNT")
                .hasMessageContaining("SAVING-ACCOUNT");
    }

    @Test
    void keeps_the_hyphenated_form_on_the_wire() {
        assertThat(AccountType.SAVING_ACCOUNT.wireName()).isEqualTo("SAVING-ACCOUNT");
    }
}
