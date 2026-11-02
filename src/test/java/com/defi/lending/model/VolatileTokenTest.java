package com.defi.lending.model;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.fail;

/** Haircut valuation for {@link VolatileToken}. */
class VolatileTokenTest {

    @Test
    @Disabled("TODO")
    void unitValueAppliesTheVolatilityBuffer() {
        // Normal: unit value is oraclePrice * (1 - volatilityBuffer).
        fail("TODO");
    }

    @Test
    @Disabled("TODO")
    void zeroBufferPassesTheOraclePriceThrough() {
        // Edge: a buffer of 0 returns the oracle price unchanged.
        fail("TODO");
    }

    @Test
    @Disabled("TODO")
    void zeroPriceStaysZero() {
        // Edge: an oracle price of zero values the token at zero. The buffer does not make it negative.
        fail("TODO");
    }

    @Test
    @Disabled("TODO")
    void negativePriceIsRejected() {
        // Invalid: a null or negative oracle price throws IllegalArgumentException.
        fail("TODO");
    }
}
