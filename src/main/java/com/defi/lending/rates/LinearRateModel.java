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
        // TODO: reject a null or negative baseRate or slope1 (IllegalArgumentException).
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
        // TODO: reject a null or negative utilization (IllegalArgumentException).
        // Return baseRate + slope1 * utilization, rounded with Precision.
        // At utilization 0 the rate is the base; at utilization 1 it is baseRate + slope1.
        throw new UnsupportedOperationException("LinearRateModel.borrowRate");
    }

    @Override
    public BigDecimal supplyRate(BigDecimal utilization, BigDecimal reserveFactor) {
        // TODO: reject a null or negative utilization, or a reserveFactor outside [0, 1)
        // (IllegalArgumentException).
        // Return borrowRate(utilization) * utilization * (1 - reserveFactor), rounded with Precision.
        // Suppliers earn nothing when nobody is borrowing, even if the base rate is positive.
        throw new UnsupportedOperationException("LinearRateModel.supplyRate");
    }
}
