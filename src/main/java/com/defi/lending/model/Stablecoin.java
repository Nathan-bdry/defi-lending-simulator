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
        // TODO: reject a null pegBand, or a pegBand that is not in the half-open range [0, 1)
        // (IllegalArgumentException). 0.005 means the peg holds for prices from 0.995 to 1.005.
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
        // TODO: reject a null or negative oraclePrice (IllegalArgumentException).
        // The peg is 1. If oraclePrice is inside [1 - pegBand, 1 + pegBand], return it unchanged.
        // Otherwise return oraclePrice * (1 - pegBand), rounded with Precision, and never negative.
        throw new UnsupportedOperationException("Stablecoin.unitValue");
    }
}
