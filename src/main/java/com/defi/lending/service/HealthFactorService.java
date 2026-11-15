package com.defi.lending.service;

import com.defi.lending.Precision;
import com.defi.lending.model.HealthBand;
import com.defi.lending.model.Loan;
import com.defi.lending.model.Market;
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
        // reject a null book or oracle.
        if (markets == null) {
            throw new IllegalArgumentException("Market book cannot be null");
        }
        if (oracle == null) {
            throw new IllegalArgumentException("Oracle cannot be null");
        }
        this.markets = markets;
        this.oracle = oracle;
    }

    /** Sum of token supply times {@code Asset.unitValue}. Not weighted by the liquidation threshold. */
    public BigDecimal collateralValue(Position position) {
        // for each supplied symbol, look up the market and the oracle price.
        // Add supply * asset.unitValue(price). A missing market or a missing price is an IllegalStateException
        // that names the symbol: the scenario must publish a price before the asset is used as collateral.
        // An empty position is zero, not an error.
        BigDecimal sum = BigDecimal.ZERO;
        for (String symbol : position.suppliedSymbols()) {
            sum = sum.add(usdValue(requireMarket(symbol), position.supplyOf(symbol)));
        }
        return sum;
    }

    /** Collateral value weighted by each market's liquidation threshold. The numerator of the health factor. */
    public BigDecimal weightedCollateral(Position position) {
        // same sum as collateralValue, but multiply each asset's USD value by that market's
        // liquidation threshold before adding it.
        BigDecimal sum = BigDecimal.ZERO;
        for (String symbol : position.suppliedSymbols()) {
            Market market = requireMarket(symbol);
            sum = sum.add(usdValue(market, position.supplyOf(symbol))
                    .multiply(market.getLiquidationThreshold())
                    .setScale(Precision.SCALE, Precision.ROUNDING));
        }
        return sum;
    }

    /**
     * The most new debt, in USD, the collateral can support.
     * Uses the collateral factor (loan-to-value), which is tighter than the liquidation threshold.
     */
    public BigDecimal borrowingPower(Position position) {
        // sum of each asset's USD collateral value times that market's collateral factor.
        BigDecimal sum = BigDecimal.ZERO;
        for (String symbol : position.suppliedSymbols()) {
            Market market = requireMarket(symbol);
            sum = sum.add(usdValue(market, position.supplyOf(symbol))
                    .multiply(market.getCollateralFactor())
                    .setScale(Precision.SCALE, Precision.ROUNDING));
        }
        return sum;
    }

    /** Sum of outstanding debt times the debt asset's unit value. */
    public BigDecimal debtValue(Position position) {
        // for each loan, add outstanding * debtAsset.unitValue(oracle price).
        // A missing market or price is an IllegalStateException naming the symbol.
        // No loans means zero.
        BigDecimal sum = BigDecimal.ZERO;
        for (Loan loan : position.getLoans()) {
            sum = sum.add(usdValue(requireMarket(loan.getAssetSymbol()), loan.getOutstanding()));
        }
        return sum;
    }

    /**
     * Weighted collateral divided by debt value.
     * Empty when there is no debt: a supplier who has not borrowed is not "infinitely healthy" and is not
     * underwater. Do not substitute zero, which would look liquidatable.
     */
    public Optional<BigDecimal> healthFactor(Position position) {
        // when debtValue is zero, return empty. Otherwise weightedCollateral / debtValue,
        // rounded with Precision. Use compareTo against zero.
        BigDecimal debt = debtValue(position);
        if (debt.compareTo(BigDecimal.ZERO) == 0) {
            return Optional.empty();
        }
        return Optional.of(weightedCollateral(position).divide(debt, Precision.SCALE, Precision.ROUNDING));
    }

    public HealthBand band(Position position) {
        // NO_DEBT when healthFactor is empty.
        // AT_THRESHOLD when the factor compares equal to 1.
        // HEALTHY when it is strictly above 1, UNDERWATER when it is strictly below.
        Optional<BigDecimal> factor = healthFactor(position);
        if (factor.isEmpty()) {
            return HealthBand.NO_DEBT;
        }
        int compared = factor.get().compareTo(BigDecimal.ONE);
        if (compared > 0) {
            return HealthBand.HEALTHY;
        }
        if (compared == 0) {
            return HealthBand.AT_THRESHOLD;
        }
        return HealthBand.UNDERWATER;
    }

    private Market requireMarket(String symbol) {
        return markets.find(symbol).orElseThrow(
                () -> new IllegalStateException("No market for " + symbol));
    }

    private BigDecimal requirePrice(String symbol) {
        return oracle.getPrice(symbol).orElseThrow(
                () -> new IllegalStateException("No price for " + symbol));
    }

    private BigDecimal usdValue(Market market, BigDecimal tokens) {
        BigDecimal price = requirePrice(market.getAsset().getSymbol());
        return tokens.multiply(market.getAsset().unitValue(price))
                .setScale(Precision.SCALE, Precision.ROUNDING);
    }
}
