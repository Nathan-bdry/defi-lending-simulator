package com.defi.lending.service;

import com.defi.lending.model.HealthBand;
import com.defi.lending.model.MarketBook;
import com.defi.lending.model.Position;
import com.defi.lending.oracle.PriceOracle;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * Turns a position plus oracle prices into USD collateral, debt, and a health factor.
 * Does not move balances. A borrow check builds a draft {@link Position} and values that.
 */
public final class HealthFactorService {

    private final MarketBook markets;
    private final PriceOracle oracle;

    public HealthFactorService(MarketBook markets, PriceOracle oracle) {
        // TODO: reject a null book or oracle.
        this.markets = markets;
        this.oracle = oracle;
    }

    /** Sum of token supply times {@code Asset.unitValue}. Not weighted by the liquidation threshold. */
    public BigDecimal collateralValue(Position position) {
        // TODO: for each supplied symbol, look up the market and the oracle price.
        // Add supply * asset.unitValue(price). A missing market or a missing price is an IllegalStateException
        // that names the symbol: the scenario must publish a price before the asset is used as collateral.
        // An empty position is zero, not an error.
        throw new UnsupportedOperationException("HealthFactorService.collateralValue");
    }

    /** Collateral value weighted by each market's liquidation threshold. The numerator of the health factor. */
    public BigDecimal weightedCollateral(Position position) {
        // TODO: same sum as collateralValue, but multiply each asset's USD value by that market's
        // liquidation threshold before adding it.
        throw new UnsupportedOperationException("HealthFactorService.weightedCollateral");
    }

    /**
     * The most new debt, in USD, the collateral can support.
     * Uses the collateral factor (loan-to-value), which is tighter than the liquidation threshold.
     */
    public BigDecimal borrowingPower(Position position) {
        // TODO: sum of each asset's USD collateral value times that market's collateral factor.
        throw new UnsupportedOperationException("HealthFactorService.borrowingPower");
    }

    /** Sum of outstanding debt times the debt asset's unit value. */
    public BigDecimal debtValue(Position position) {
        // TODO: for each loan, add outstanding * debtAsset.unitValue(oracle price).
        // A missing market or price is an IllegalStateException naming the symbol.
        // No loans means zero.
        throw new UnsupportedOperationException("HealthFactorService.debtValue");
    }

    /**
     * Weighted collateral divided by debt value.
     * Empty when there is no debt: a supplier who has not borrowed is not "infinitely healthy" and is not
     * underwater. Do not substitute zero, which would look liquidatable.
     */
    public Optional<BigDecimal> healthFactor(Position position) {
        // TODO: when debtValue is zero, return empty. Otherwise weightedCollateral / debtValue,
        // rounded with Precision. Use compareTo against zero.
        throw new UnsupportedOperationException("HealthFactorService.healthFactor");
    }

    public HealthBand band(Position position) {
        // TODO: NO_DEBT when healthFactor is empty.
        // AT_THRESHOLD when the factor compares equal to 1.
        // HEALTHY when it is strictly above 1, UNDERWATER when it is strictly below.
        throw new UnsupportedOperationException("HealthFactorService.band");
    }
}
