package com.defi.lending;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Shared rounding policy for token amounts, USD values, and rates.
 * One scale keeps health-factor comparisons stable across services:
 * every multiplication and division rounds the same way, so "exactly 1.0"
 * means the same thing in a borrow check and in a liquidation check.
 */
public final class Precision {

    /** Enough fractional digits for an 18-decimal token without dropping a unit of wei-sized dust. */
    public static final int SCALE = 18;

    public static final RoundingMode ROUNDING = RoundingMode.HALF_UP;

    /**
     * 365-day year. Interest is an annual rate scaled by elapsed seconds over this constant,
     * so a one-day loan of a 10% asset accrues the same amount no matter which service applies it.
     */
    public static final BigDecimal SECONDS_PER_YEAR = BigDecimal.valueOf(365L * 24L * 60L * 60L);

    private Precision() {
    }
}
