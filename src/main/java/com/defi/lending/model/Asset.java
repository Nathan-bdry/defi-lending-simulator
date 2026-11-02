package com.defi.lending.model;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * A token the protocol can list. Subclasses decide how a raw oracle price becomes one token's USD value.
 * Pool governance (loan-to-value, liquidation threshold, bonus, close factor, interest model)
 * is not stored here: two listings of the same class can have different parameters, so those live on {@link Market}.
 */
public abstract class Asset {

    private final String symbol;
    private final String name;
    private final int decimals;

    protected Asset(String symbol, String name, int decimals) {
        // TODO: reject a null or blank symbol, a null or blank name, and a negative decimals
        // (IllegalArgumentException). Do not change the symbol's case; files and code must match it.
        this.symbol = symbol;
        this.name = name;
        this.decimals = decimals;
    }

    public String getSymbol() {
        return symbol;
    }

    public String getName() {
        return name;
    }

    /** Maximum decimal places an amount of this asset may use. Amounts are whole tokens, not base units. */
    public int getDecimals() {
        return decimals;
    }

    public abstract AssetType getType();

    /**
     * USD value of one whole token.
     * Callers must value collateral and debt through this method so a stablecoin and a volatile token
     * with the same oracle print are not treated as the same risk.
     */
    public abstract BigDecimal unitValue(BigDecimal oraclePrice);

    @Override
    public final boolean equals(Object other) {
        // The ticker is the listing. Rebuilding an asset from the same row must not look like a second token.
        if (this == other) {
            return true;
        }
        if (!(other instanceof Asset asset)) {
            return false;
        }
        return Objects.equals(symbol, asset.symbol);
    }

    @Override
    public final int hashCode() {
        return Objects.hashCode(symbol);
    }

    @Override
    public String toString() {
        return getType() + " " + symbol;
    }
}
