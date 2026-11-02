package com.defi.lending.model;

/**
 * A user action loaded from {@code operations.csv}.
 * The file order is the execution order; this enum does not rank the actions.
 */
public enum OperationType {
    DEPOSIT,
    WITHDRAW,
    BORROW,
    REPAY
}
