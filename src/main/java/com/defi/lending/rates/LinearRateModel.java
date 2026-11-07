package com.defi.lending.rates;

import java.math.BigDecimal;

/**
 * Borrow rate rises in a straight line with utilization.
 * Used for a listing whose rate should stay predictable, such as the DAI pool in the sample file.
 */
public final class LinearRateModel implements InterestRateModel {

    private final BigDecimal baseRate;
    private final BigDecimal slope1;

    /**
     * @param baseRate annual rate at zero utilization
     * @param slope1 annual rate added on top of the base when utilization is 1
     */
    public LinearRateModel(BigDecimal baseRate, BigDecimal slope1) {
        // DONE: reject a null or negative baseRate or slope1 (IllegalArgumentException).
        if (baseRate == null || baseRate.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Base rate cannot be null or negative");
        }
        if (slope1 == null || slope1.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Slope1 cannot be null or negative");
        }
        this.baseRate = baseRate;
        this.slope1 = slope1;
    }

    public BigDecimal getBaseRate() {
        return baseRate;
    }

    public BigDecimal getSlope1() {
        return slope1;
    }

    @Override
    public RateModelType getType() {
        return RateModelType.LINEAR;
    }

    @Override
    public BigDecimal borrowRate(BigDecimal utilization) {
        // DONE: reject a null or negative utilization (IllegalArgumentException).
        // Return baseRate + slope1 * utilization, rounded with Precision.
        // At utilization 0 the rate is the base; at utilization 1 it is baseRate + slope1.
        if (utilization == null || utilization.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Utilization cannot be null or negative");
        }
        return baseRate.add(slope1.multiply(utilization)).setScale(Precision.SCALE, RoundingMode.HALF_UP);
    }

    @Override
    public BigDecimal supplyRate(BigDecimal utilization, BigDecimal reserveFactor) {
        // DONE: reject a null or negative utilization, or a reserveFactor outside [0, 1)
        // (IllegalArgumentException).
        // Return borrowRate(utilization) * utilization * (1 - reserveFactor), rounded with Precision.
        // Suppliers earn nothing when nobody is borrowing, even if the base rate is positive.
        if (utilization == null || utilization.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Utilization cannot be null or negative");
        }
        if (reserveFactor == null || reserveFactor.compareTo(BigDecimal.ZERO) < 0 || reserveFactor.compareTo(BigDecimal.ONE) >= 0) {
            throw new IllegalArgumentException("Reserve factor must be in the range [0, 1)");
        }
        return borrowRate(utilization).multiply(utilization).multiply(BigDecimal.ONE.subtract(reserveFactor)).setScale(Precision.SCALE, RoundingMode.HALF_UP);
    }
}
