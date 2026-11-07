package com.defi.lending.rates;

import java.math.BigDecimal;

/**
 * Aave-style borrow rate: a gentle slope up to an optimal utilization, then a steep slope.
 * The kink is what makes a pool expensive to max out, so cash remains for withdrawals.
 */
public final class KinkedRateModel implements InterestRateModel {

    private final BigDecimal baseRate;
    private final BigDecimal slope1;
    private final BigDecimal slope2;
    private final BigDecimal optimalUtilization;

    /**
     * @param baseRate annual rate at zero utilization
     * @param slope1 annual rate at the kink, above the base (not a per-unit slope)
     * @param slope2 extra annual rate added between the kink and full utilization
     * @param optimalUtilization kink, strictly between 0 and 1
     */
    public KinkedRateModel(BigDecimal baseRate, BigDecimal slope1, BigDecimal slope2,
                           BigDecimal optimalUtilization) {
        // DONE: reject nulls. baseRate, slope1, and slope2 must be >= 0.
        // optimalUtilization must be strictly inside (0, 1) so both branches have a non-zero span.
        if (baseRate == null || baseRate.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Base rate cannot be null or negative");
        }
        if (slope1 == null || slope1.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Slope1 cannot be null or negative");
        }
        if (slope2 == null || slope2.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Slope2 cannot be null or negative");
        }
        if (optimalUtilization == null || optimalUtilization.compareTo(BigDecimal.ZERO) <= 0 || optimalUtilization.compareTo(BigDecimal.ONE) >= 0) {
            throw new IllegalArgumentException("Optimal utilization must be strictly inside (0, 1)");
        }
        this.baseRate = baseRate;
        this.slope1 = slope1;
        this.slope2 = slope2;
        this.optimalUtilization = optimalUtilization;
    }

    public BigDecimal getBaseRate() {
        return baseRate;
    }

    public BigDecimal getSlope1() {
        return slope1;
    }

    public BigDecimal getSlope2() {
        return slope2;
    }

    public BigDecimal getOptimalUtilization() {
        return optimalUtilization;
    }

    @Override
    public RateModelType getType() {
        return RateModelType.KINKED;
    }

    @Override
    public BigDecimal borrowRate(BigDecimal utilization) {
        // DONE: reject a null or negative utilization (IllegalArgumentException).
        // When utilization <= optimalUtilization:
        //     baseRate + slope1 * (utilization / optimalUtilization)
        // Otherwise:
        //     baseRate + slope1 + slope2 * (utilization - optimalUtilization) / (1 - optimalUtilization)
        // The boundary belongs to the first branch, so the two pieces meet at the kink.
        // Round the result with Precision.
        if (utilization == null || utilization.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Utilization cannot be null or negative");
        }
        if (utilization.compareTo(optimalUtilization) <= 0) {
            return baseRate.add(slope1.multiply(utilization.divide(optimalUtilization, RoundingMode.HALF_UP))).setScale(Precision.SCALE, RoundingMode.HALF_UP);
        }
        return baseRate.add(slope1).add(slope2.multiply(utilization.subtract(optimalUtilization).divide(BigDecimal.ONE.subtract(optimalUtilization), RoundingMode.HALF_UP))).setScale(Precision.SCALE, RoundingMode.HALF_UP);
    }

    @Override
    public BigDecimal supplyRate(BigDecimal utilization, BigDecimal reserveFactor) {
        // DONE: same contract as LinearRateModel.supplyRate:
        // borrowRate(utilization) * utilization * (1 - reserveFactor), rounded with Precision.
        // Reject a reserveFactor outside [0, 1).
        if (utilization == null || utilization.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Utilization cannot be null or negative");
        }
        if (reserveFactor == null || reserveFactor.compareTo(BigDecimal.ZERO) < 0 || reserveFactor.compareTo(BigDecimal.ONE) >= 0) {
            throw new IllegalArgumentException("Reserve factor must be in the range [0, 1)");
        }
        return borrowRate(utilization).multiply(utilization).multiply(BigDecimal.ONE.subtract(reserveFactor)).setScale(Precision.SCALE, RoundingMode.HALF_UP);
    }
}
