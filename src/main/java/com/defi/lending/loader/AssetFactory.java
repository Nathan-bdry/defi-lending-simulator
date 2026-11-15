package com.defi.lending.loader;

import com.defi.lending.model.Asset;
import com.defi.lending.model.AssetType;
import com.defi.lending.model.Stablecoin;
import com.defi.lending.model.VolatileToken;

import java.math.BigDecimal;

/**
 * The only place that chooses between {@link com.defi.lending.model.Stablecoin} and
 * {@link com.defi.lending.model.VolatileToken}. Loaders pass already-split columns in and get an {@link Asset} back.
 */
public final class AssetFactory {

    public Asset create(String symbol, String name, AssetType type, int decimals,
                        BigDecimal pegBand, BigDecimal volatilityBuffer) {
        // switch on type.
        // STABLECOIN requires a non-null pegBand and ignores volatilityBuffer; return a Stablecoin.
        // VOLATILE requires a non-null volatilityBuffer and ignores pegBand; return a VolatileToken.
        // A null type is IllegalArgumentException.
        // This switch is the simple factory: callers, including MarketLoader, must not name the subclasses.
        if (type == null) {
            throw new IllegalArgumentException("Type cannot be null");
        }
        switch (type) {
            case STABLECOIN:
                if (pegBand == null) {
                    throw new IllegalArgumentException("Peg band cannot be null");
                }
                return new Stablecoin(symbol, name, decimals, pegBand);
            case VOLATILE:
                if (volatilityBuffer == null) {
                    throw new IllegalArgumentException("Volatility buffer cannot be null");
                }
                return new VolatileToken(symbol, name, decimals, volatilityBuffer);
            default:
                throw new IllegalArgumentException("Unknown asset type");
        }
    }
}
