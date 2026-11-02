package com.defi.lending.rates;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.fail;

/** Borrow and supply rates for {@link LinearRateModel}. */
class LinearRateModelTest {

    @Test
    @Disabled("TODO")
    void zeroUtilizationReturnsTheBaseRate() {
        // Edge: borrow rate at utilization 0 is the base rate. Supply rate is 0 even when the base is positive,
        // because suppliers are paid from borrower interest.
        fail("TODO");
    }

    @Test
    @Disabled("TODO")
    void fullUtilizationReturnsBasePlusSlope() {
        // Normal: borrow rate at utilization 1 is baseRate + slope1.
        fail("TODO");
    }

    @Test
    @Disabled("TODO")
    void rateIncreasesLinearlyBetweenTheEnds() {
        // Normal: at utilization 0.25 the borrow rate is baseRate + slope1 * 0.25.
        fail("TODO");
    }

    @Test
    @Disabled("TODO")
    void supplyRateShrinksByTheReserveFactor() {
        // Normal: supply rate equals borrowRate * utilization * (1 - reserveFactor).
        fail("TODO");
    }

    @Test
    @Disabled("TODO")
    void negativeUtilizationIsRejected() {
        // Invalid: a negative utilization throws IllegalArgumentException.
        fail("TODO");
    }

    @Test
    @Disabled("TODO")
    void negativeSlopeIsRejected() {
        // Invalid: a negative base rate or slope throws IllegalArgumentException from the constructor.
        fail("TODO");
    }
}
