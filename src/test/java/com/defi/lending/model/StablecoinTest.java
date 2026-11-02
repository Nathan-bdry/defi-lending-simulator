package com.defi.lending.model;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.fail;

/** Peg valuation for {@link Stablecoin}. */
class StablecoinTest {

    @Test
    @Disabled("TODO")
    void priceInsideTheBandIsUsedUnchanged() {
        // Normal: a price strictly inside (1 - pegBand, 1 + pegBand) is the unit value.
        fail("TODO");
    }

    @Test
    @Disabled("TODO")
    void priceExactlyOnEitherEdgeOfTheBandIsStillOnPeg() {
        // Edge: 1 - pegBand and 1 + pegBand are inside the band and are not haircut.
        fail("TODO");
    }

    @Test
    @Disabled("TODO")
    void priceOutsideTheBandIsHaircut() {
        // Normal: below 1 - pegBand, or above 1 + pegBand, the unit value is price * (1 - pegBand).
        fail("TODO");
    }

    @Test
    @Disabled("TODO")
    void negativePriceIsRejected() {
        // Invalid: a null or negative oracle price throws IllegalArgumentException.
        // A price of zero is outside the band and values to zero.
        fail("TODO");
    }
}
