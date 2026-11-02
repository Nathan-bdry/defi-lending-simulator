package com.defi.lending.model;

import java.math.BigDecimal;

/**
 * A token whose price is expected to move. Collateral is valued below the oracle print
 * so a short wick does not instantly wipe the health factor; the market's liquidation
 * threshold is a second, separate control.
 */
public final class VolatileToken extends Asset {

    private final BigDecimal volatilityBuffer;

    public VolatileToken(String symbol, String name, int decimals, BigDecimal volatilityBuffer) {
        super(symbol, name, decimals);
        // TODO: reject a null volatilityBuffer, or one that is not in [0, 1)
        // (IllegalArgumentException). 0.02 values the token at 98% of the oracle price.
        this.volatilityBuffer = volatilityBuffer;
    }

    public BigDecimal getVolatilityBuffer() {
        return volatilityBuffer;
    }

    @Override
    public AssetType getType() {
        return AssetType.VOLATILE;
    }

    @Override
    public BigDecimal unitValue(BigDecimal oraclePrice) {
        // TODO: reject a null or negative oraclePrice (IllegalArgumentException).
        // Return oraclePrice * (1 - volatilityBuffer), rounded with Precision. Never negative.
        throw new UnsupportedOperationException("VolatileToken.unitValue");
    }
}
