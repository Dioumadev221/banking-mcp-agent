package com.diouma.ebankservice.controller;

import java.math.BigDecimal;

/** The body of a transfer: move {@code amount} from one account to another. */
public record TransferRequest(String fromAccountId, String toAccountId, BigDecimal amount) {
}
