package com.defi.lending.rates;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.fail;

/** Borrow and supply rates for {@link KinkedRateModel}, including the kink boundary. */
class KinkedRateModelTest {

    @Test
    @Disabled("TODO")
    void belowTheKinkOnlySlopeOneApplies() {
        // Normal: utilization under the kink uses baseRate + slope1 * (utilization / optimalUtilization).
        // slope2 does not appear.
        fail("TODO");
    }

    @Test
    @Disabled("TODO")
    void exactlyAtTheKinkMatchesTheSlopeOneBranch() {
        // Edge: utilization equal to optimalUtilization returns baseRate + slope1.
        // The boundary stays on the first branch, so the two formulas meet.
        fail("TODO");
    }

    @Test
    @Disabled("TODO")
    void aboveTheKinkAddsSlopeTwoOnTheExcess() {
        // Normal: above the kink the rate is
        // baseRate + slope1 + slope2 * (utilization - optimal) / (1 - optimal).
        fail("TODO");
    }

    @Test
    @Disabled("TODO")
    void fullUtilizationReachesBasePlusBothSlopes() {
        // Edge: utilization 1 returns baseRate + slope1 + slope2.
        fail("TODO");
    }

    @Test
    @Disabled("TODO")
    void supplyRateUsesTheSameRelationshipAsTheLinearModel() {
        // Normal: supply rate is borrowRate(utilization) * utilization * (1 - reserveFactor).
        // The kink changes the borrow rate, not this relationship.
        fail("TODO");
    }

    @Test
    @Disabled("TODO")
    void optimalUtilizationOutsideTheOpenUnitIntervalIsRejected() {
        // Invalid: an optimal utilization of 0, 1, or outside that range throws IllegalArgumentException.
        // Both branches need a non-zero span.
        fail("TODO");
    }

    @Test
    @Disabled("TODO")
    void negativeUtilizationIsRejected() {
        // Invalid: a negative utilization throws IllegalArgumentException.
        fail("TODO");
    }
}
