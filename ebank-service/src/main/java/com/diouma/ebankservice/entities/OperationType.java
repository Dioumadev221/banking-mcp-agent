package com.diouma.ebankservice.entities;

/**
 * The kind of movement a ledger row records.
 *
 * <p>A transfer is stored as two rows: a {@code TRANSFER_OUT} on the source and
 * a {@code TRANSFER_IN} on the destination, sharing one transfer id. Keeping the
 * two directions distinct means an account's statement reads as money leaving or
 * arriving, without having to compare account ids to work out which way it went.
 */
public enum OperationType {
    DEPOSIT,
    WITHDRAWAL,
    TRANSFER_IN,
    TRANSFER_OUT
}
