package com.defi.lending.model;

import java.math.BigDecimal;

/**
 * A token that targets a one-dollar peg. Inside the band the oracle price is trusted;
 * outside it, collateral is haircut so a broken peg shows up in the health factor
 * before the price has gone to zero.
 */
public final class Stablecoin extends Asset {

    private final BigDecimal pegBand;

    public Stablecoin(String symbol, String name, int decimals, BigDecimal pegBand) {
        super(symbol, name, decimals);
        // DONE: reject a null pegBand, or a pegBand that is not in the half-open range [0, 1)
        // (IllegalArgumentException). 0.005 means the peg holds for prices from 0.995 to 1.005.
        if (pegBand == null) {
            throw new IllegalArgumentException("Peg band cannot be null");
        }
        if (pegBand.compareTo(BigDecimal.ZERO) < 0 || pegBand.compareTo(BigDecimal.ONE) >= 0) {
            throw new IllegalArgumentException("Peg band must be in the half-open range [0, 1)");
        }
        this.pegBand = pegBand;
    }

    public BigDecimal getPegBand() {
        return pegBand;
    }

    @Override
    public AssetType getType() {
        return AssetType.STABLECOIN;
    }

    @Override
    public BigDecimal unitValue(BigDecimal oraclePrice) {
        // DONE: reject a null or negative oraclePrice (IllegalArgumentException).
        // The peg is 1. If oraclePrice is inside [1 - pegBand, 1 + pegBand], return it unchanged.
        // Otherwise return oraclePrice * (1 - pegBand), rounded with Precision, and never negative.
        if (oraclePrice == null) {
            throw new IllegalArgumentException("Oracle price cannot be null");
        }
        if (oraclePrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Oracle price cannot be negative");
        }
        if (oraclePrice.compareTo(BigDecimal.ONE.subtract(pegBand)) < 0 || oraclePrice.compareTo(BigDecimal.ONE.add(pegBand)) > 0) {
            return oraclePrice.multiply(BigDecimal.ONE.subtract(pegBand)).setScale(getDecimals(), RoundingMode.HALF_UP);
        }
        throw new UnsupportedOperationException("Stablecoin.unitValue");
    }
}
