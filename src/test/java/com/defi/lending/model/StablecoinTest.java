package com.defi.lending.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Peg valuation for {@link Stablecoin}. */
class StablecoinTest {

    @Test
    void priceInsideTheBandIsUsedUnchanged() {
        // Normal: a price strictly inside (1 - pegBand, 1 + pegBand) is the unit value.
        Stablecoin dai = new Stablecoin("DAI", "Dai", 18, new BigDecimal("0.01"));
        BigDecimal belowPeg = new BigDecimal("0.995");
        BigDecimal abovePeg = new BigDecimal("1.005");

        assertEquals(0, belowPeg.compareTo(dai.unitValue(belowPeg)));
        assertEquals(0, abovePeg.compareTo(dai.unitValue(abovePeg)));
    }

    @Test
    void priceExactlyOnEitherEdgeOfTheBandIsStillOnPeg() {
        // Edge: 1 - pegBand and 1 + pegBand are inside the band and are not haircut.
        Stablecoin dai = new Stablecoin("DAI", "Dai", 18, new BigDecimal("0.01"));
        BigDecimal belowPeg = new BigDecimal("0.99");
        BigDecimal abovePeg = new BigDecimal("1.01");

        assertEquals(0, belowPeg.compareTo(dai.unitValue(belowPeg)));
        assertEquals(0, abovePeg.compareTo(dai.unitValue(abovePeg)));
    }

    @Test
    void priceOutsideTheBandIsHaircut() {
        // Normal: below 1 - pegBand, or above 1 + pegBand, the unit value is price * (1 - pegBand).
        Stablecoin dai = new Stablecoin("DAI", "Dai", 18, new BigDecimal("0.01"));
        BigDecimal belowPeg = new BigDecimal("0.98");
        BigDecimal abovePeg = new BigDecimal("1.02");
        BigDecimal factor = BigDecimal.ONE.subtract(new BigDecimal("0.01"));

        assertEquals(0, belowPeg.multiply(factor).compareTo(dai.unitValue(belowPeg)));
        assertEquals(0, abovePeg.multiply(factor).compareTo(dai.unitValue(abovePeg)));
    }

    @Test
    void negativePriceIsRejected() {
        // Invalid: a null or negative oracle price throws IllegalArgumentException.
        // A price of zero is outside the band and values to zero.
        Stablecoin dai = new Stablecoin("DAI", "Dai", 18, new BigDecimal("0.01"));
        BigDecimal negativePrice = new BigDecimal("-0.01");
        BigDecimal zeroPrice = new BigDecimal("0.00");

        assertThrows(IllegalArgumentException.class, () -> dai.unitValue(negativePrice));
        assertThrows(IllegalArgumentException.class, () -> dai.unitValue(null));
        assertEquals(0, BigDecimal.ZERO.compareTo(dai.unitValue(zeroPrice)));
    }
}
