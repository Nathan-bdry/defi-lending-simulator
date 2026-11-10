package com.defi.lending.rates;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.defi.lending.Precision;

/** Borrow and supply rates for {@link KinkedRateModel}, including the kink boundary. */
class KinkedRateModelTest {

    @Test
    void belowTheKinkOnlySlopeOneApplies() {
        // Normal: utilization under the kink uses baseRate + slope1 * (utilization / optimalUtilization).
        // slope2 does not appear.
        KinkedRateModel model = new KinkedRateModel(new BigDecimal("0.01"), new BigDecimal("0.04"), new BigDecimal("0.08"), new BigDecimal("0.50"));
        BigDecimal utilization = new BigDecimal("0.25");
        BigDecimal reserveFactor = new BigDecimal("0.10");

        assertEquals(0, new BigDecimal("0.01").add(new BigDecimal("0.04").multiply(utilization.divide(new BigDecimal("0.50"), Precision.SCALE, Precision.ROUNDING))).compareTo(model.borrowRate(utilization)));
        assertEquals(0, model.borrowRate(utilization).multiply(utilization).multiply(BigDecimal.ONE.subtract(reserveFactor)).compareTo(model.supplyRate(utilization, reserveFactor)));
    }

    @Test
    void exactlyAtTheKinkMatchesTheSlopeOneBranch() {
        // Edge: utilization equal to optimalUtilization returns baseRate + slope1.
        // The boundary stays on the first branch, so the two formulas meet.
        KinkedRateModel model = new KinkedRateModel(new BigDecimal("0.01"), new BigDecimal("0.04"), new BigDecimal("0.08"), new BigDecimal("0.50"));
        BigDecimal utilization = new BigDecimal("0.50");
        BigDecimal reserveFactor = new BigDecimal("0.10");

        assertEquals(0, new BigDecimal("0.01").add(new BigDecimal("0.04")).compareTo(model.borrowRate(utilization)));
        assertEquals(0, model.borrowRate(utilization).multiply(utilization).multiply(BigDecimal.ONE.subtract(reserveFactor)).compareTo(model.supplyRate(utilization, reserveFactor)));
    }

    @Test
    void aboveTheKinkAddsSlopeTwoOnTheExcess() {
        // Normal: above the kink the rate is
        // baseRate + slope1 + slope2 * (utilization - optimal) / (1 - optimal).
        KinkedRateModel model = new KinkedRateModel(new BigDecimal("0.01"), new BigDecimal("0.04"), new BigDecimal("0.08"), new BigDecimal("0.50"));
        BigDecimal utilization = new BigDecimal("0.75");
        BigDecimal reserveFactor = new BigDecimal("0.10");

        assertEquals(0, new BigDecimal("0.01").add(new BigDecimal("0.04")).add(new BigDecimal("0.08").multiply(utilization.subtract(new BigDecimal("0.50")).divide(BigDecimal.ONE.subtract(new BigDecimal("0.50")), Precision.SCALE, Precision.ROUNDING))).compareTo(model.borrowRate(utilization)));
        assertEquals(0, model.borrowRate(utilization).multiply(utilization).multiply(BigDecimal.ONE.subtract(reserveFactor)).compareTo(model.supplyRate(utilization, reserveFactor)));
    }

    @Test
    void fullUtilizationReachesBasePlusBothSlopes() {
        // Edge: utilization 1 returns baseRate + slope1 + slope2.
        KinkedRateModel model = new KinkedRateModel(new BigDecimal("0.01"), new BigDecimal("0.04"), new BigDecimal("0.08"), new BigDecimal("0.50"));
        BigDecimal utilization = new BigDecimal("1.00");
        BigDecimal reserveFactor = new BigDecimal("0.10");

        assertEquals(0, new BigDecimal("0.01").add(new BigDecimal("0.04")).add(new BigDecimal("0.08")).compareTo(model.borrowRate(utilization)));
        assertEquals(0, model.borrowRate(utilization).multiply(utilization).multiply(BigDecimal.ONE.subtract(reserveFactor)).compareTo(model.supplyRate(utilization, reserveFactor)));
    }

    @Test
    void supplyRateUsesTheSameRelationshipAsTheLinearModel() {
        // Normal: supply rate is borrowRate(utilization) * utilization * (1 - reserveFactor).
        // The kink changes the borrow rate, not this relationship.
        KinkedRateModel model = new KinkedRateModel(new BigDecimal("0.01"), new BigDecimal("0.04"), new BigDecimal("0.08"), new BigDecimal("0.50"));
        BigDecimal utilization = new BigDecimal("0.25");
        BigDecimal reserveFactor = new BigDecimal("0.10");

        assertEquals(0, model.borrowRate(utilization).multiply(utilization).multiply(BigDecimal.ONE.subtract(reserveFactor)).compareTo(model.supplyRate(utilization, reserveFactor)));
    }

    @Test
    void optimalUtilizationOutsideTheOpenUnitIntervalIsRejected() {
        // Invalid: an optimal utilization of 0, 1, or outside that range throws IllegalArgumentException.
        // Both branches need a non-zero span.
        assertThrows(IllegalArgumentException.class, () -> new KinkedRateModel(new BigDecimal("0.01"), new BigDecimal("0.04"), new BigDecimal("0.08"), new BigDecimal("0.00")));
        assertThrows(IllegalArgumentException.class, () -> new KinkedRateModel(new BigDecimal("0.01"), new BigDecimal("0.04"), new BigDecimal("0.08"), new BigDecimal("1.00")));
        assertThrows(IllegalArgumentException.class, () -> new KinkedRateModel(new BigDecimal("0.01"), new BigDecimal("0.04"), new BigDecimal("0.08"), new BigDecimal("2.00")));
    }

    @Test
    void negativeUtilizationIsRejected() {
        // Invalid: a negative utilization throws IllegalArgumentException.
        KinkedRateModel model = new KinkedRateModel(new BigDecimal("0.01"), new BigDecimal("0.04"), new BigDecimal("0.08"), new BigDecimal("0.50"));
        assertThrows(IllegalArgumentException.class, () -> model.borrowRate(new BigDecimal("-0.10")));
        assertThrows(IllegalArgumentException.class, () -> model.supplyRate(new BigDecimal("-0.10"), new BigDecimal("0.10")));
    }
}
