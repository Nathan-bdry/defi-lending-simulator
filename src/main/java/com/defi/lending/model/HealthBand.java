package com.defi.lending.model;

/**
 * Where an account sits relative to the liquidation line.
 * Exactly 1 is its own band so a report does not call a fully extended position underwater.
 */
public enum HealthBand {
    /** No debt, so the health factor is undefined. */
    NO_DEBT,
    /** Health factor strictly above 1. */
    HEALTHY,
    /** Health factor equal to 1. Solvent, and not liquidatable. */
    AT_THRESHOLD,
    /** Health factor strictly below 1. May be liquidated. */
    UNDERWATER
}
