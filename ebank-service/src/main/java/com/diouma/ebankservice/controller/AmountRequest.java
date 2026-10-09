package com.diouma.ebankservice.controller;

import java.math.BigDecimal;

/** The body of a deposit or a withdrawal: how much, as an exact decimal. */
public record AmountRequest(BigDecimal amount) {
}
