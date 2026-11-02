package com.defi.lending.rates;

/**
 * How a market turns utilization into an annual borrow rate.
 * The {@code rateModel} column of {@code markets.csv} uses these names.
 */
public enum RateModelType {
    LINEAR,
    KINKED
}
