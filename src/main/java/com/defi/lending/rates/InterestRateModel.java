package com.defi.lending.rates;

import java.math.BigDecimal;

/**
 * Strategy for the interest a pool pays and charges.
 * Two markets can share a shape (linear or kinked) with different parameters;
 * the lending service asks the market's model and never branches on the concrete class.
 */
public interface InterestRateModel {

    RateModelType getType();

    /**
     * Annual borrow rate as a fraction at this utilization. A return of 0.05 means 5% a year.
     * Utilization is total borrowed divided by total supplied.
     */
    BigDecimal borrowRate(BigDecimal utilization);

    /**
     * Annual supply rate as a fraction.
     * Every model pays suppliers {@code borrowRate * utilization * (1 - reserveFactor)};
     * the models differ in {@link #borrowRate(BigDecimal)}, not in this relationship.
     */
    BigDecimal supplyRate(BigDecimal utilization, BigDecimal reserveFactor);
}
