package com.defi.lending.rates;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Borrow and supply rates for {@link LinearRateModel}. */
class LinearRateModelTest {

    @Test
    void zeroUtilizationReturnsTheBaseRate() {
        // Edge: borrow rate at utilization 0 is the base rate. Supply rate is 0 even when the base is positive,
        // because suppliers are paid from borrower interest.
        LinearRateModel model = new LinearRateModel(new BigDecimal("0.01"), new BigDecimal("0.04"));
        BigDecimal utilization = BigDecimal.ZERO;
        BigDecimal reserveFactor = new BigDecimal("0.10");

        assertEquals(0, new BigDecimal("0.01").compareTo(model.borrowRate(utilization)));
        assertEquals(0, BigDecimal.ZERO.compareTo(model.supplyRate(utilization, reserveFactor)));
    }

    @Test
    void fullUtilizationReturnsBasePlusSlope() {
        // Normal: borrow rate at utilization 1 is baseRate + slope1.
        LinearRateModel model = new LinearRateModel(new BigDecimal("0.01"), new BigDecimal("0.04"));
        BigDecimal utilization = BigDecimal.ONE;

        assertEquals(0, new BigDecimal("0.01").add(new BigDecimal("0.04")).compareTo(model.borrowRate(utilization)));
    }

    @Test
    void rateIncreasesLinearlyBetweenTheEnds() {
        // Normal: at utilization 0.25 the borrow rate is baseRate + slope1 * 0.25.
        LinearRateModel model = new LinearRateModel(new BigDecimal("0.01"), new BigDecimal("0.04"));
        BigDecimal utilization = new BigDecimal("0.25");

        assertEquals(0, new BigDecimal("0.01").add(new BigDecimal("0.04").multiply(utilization)).compareTo(model.borrowRate(utilization)));
    }

    @Test
    void supplyRateShrinksByTheReserveFactor() {
        // Normal: supply rate equals borrowRate * utilization * (1 - reserveFactor).
        LinearRateModel model = new LinearRateModel(new BigDecimal("0.01"), new BigDecimal("0.04"));
        BigDecimal utilization = new BigDecimal("0.25");
        BigDecimal reserveFactor = new BigDecimal("0.10");

        assertEquals(0, model.borrowRate(utilization).multiply(utilization).multiply(BigDecimal.ONE.subtract(reserveFactor)).compareTo(model.supplyRate(utilization, reserveFactor)));
    }

    @Test
    void negativeUtilizationIsRejected() {
        // Invalid: a negative utilization throws IllegalArgumentException.
        LinearRateModel model = new LinearRateModel(new BigDecimal("0.01"), new BigDecimal("0.04"));
        BigDecimal utilization = new BigDecimal("-0.10");
        BigDecimal reserveFactor = new BigDecimal("0.10");

        assertThrows(IllegalArgumentException.class, () -> model.borrowRate(utilization));
        assertThrows(IllegalArgumentException.class, () -> model.supplyRate(utilization, reserveFactor));
    }

    @Test
    void negativeSlopeIsRejected() {
        // Invalid: a negative base rate or slope throws IllegalArgumentException from the constructor.
        assertThrows(IllegalArgumentException.class, () -> new LinearRateModel(new BigDecimal("-0.01"), new BigDecimal("0.04")));
        assertThrows(IllegalArgumentException.class, () -> new LinearRateModel(new BigDecimal("0.01"), new BigDecimal("-0.04")));
    }
}
