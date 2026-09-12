package com.diouma.agentservice.models;

/**
 * The structured form of a natural-language account request.
 *
 * <p>Used by the extraction endpoint, which turns a sentence into typed fields
 * without acting on them - the step before letting the model call a tool.
 */
public record AccountRequest(String type, double balance, long customerId) {
}
