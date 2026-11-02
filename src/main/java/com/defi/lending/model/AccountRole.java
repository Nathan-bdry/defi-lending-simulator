package com.defi.lending.model;

/**
 * What an account is allowed to do when a price tick makes a position liquidatable.
 * Exactly one {@link #LIQUIDATOR} is required in {@code accounts.csv}; everyone else is a {@link #USER}.
 */
public enum AccountRole {
    USER,
    LIQUIDATOR
}
