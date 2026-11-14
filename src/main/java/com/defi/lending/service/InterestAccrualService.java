package com.defi.lending.service;

import com.defi.lending.Precision;
import com.defi.lending.model.Loan;
import com.defi.lending.model.Market;
import com.defi.lending.model.MarketBook;
import com.defi.lending.model.Portfolio;
import com.defi.lending.model.Position;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * Capitalizes interest onto every supply and debt of a market, then books the pool totals.
 * Rates come from the market's {@code InterestRateModel}; this class only applies them across time.
 */
public final class InterestAccrualService {

    private final MarketBook markets;
    private final Portfolio portfolio;

    public InterestAccrualService(MarketBook markets, Portfolio portfolio) {
        // DONE: reject a null book or portfolio.
        if (markets == null) {
            throw new IllegalArgumentException("Market book cannot be null");
        }
        if (portfolio == null) {
            throw new IllegalArgumentException("Portfolio cannot be null");
        }
        this.markets = markets;
        this.portfolio = portfolio;
    }

    /**
     * Brings one market, and every position in it, forward to {@code to}.
     * Reads utilization before changing any balance, then updates positions, then the market.
     */
    public void accrue(Market market, LocalDateTime to) {
        // DONE:
        // 1. Reject a null market or timestamp. If `to` equals lastAccrual, return.
        //    If `to` is before lastAccrual, throw IllegalArgumentException.
        // 2. From the totals as they are now, read utilization, borrowRate, and supplyRate
        //    (supplyRate receives the market's reserve factor).
        // 3. dt is the seconds from lastAccrual to `to`.
        //    borrowFactor = 1 + borrowRate * dt / Precision.SECONDS_PER_YEAR
        //    supplyFactor = 1 + supplyRate * dt / Precision.SECONDS_PER_YEAR
        //    Round factors with Precision.
        // 4. For every position, capitalizeSupply(symbol, supplyFactor) and, when a loan in this
        //    asset exists, loan.capitalize(borrowFactor).
        // 5. newSupplied is the sum of every position's supply of this asset.
        //    newBorrowed is the sum of every position's outstanding debt of this asset.
        //    Reserves grow by (newBorrowed - oldBorrowed) - (newSupplied - oldSupplied),
        //    which is the borrower interest suppliers did not receive. Booking the residual,
        //    rather than a second multiplication, keeps
        //    cash + totalBorrowed = totalSupplied + reserves exact after rounding.
        // 6. market.bookAccrual(newSupplied, newBorrowed, addedReserves, to).
        // Do not change cash.
        if (market == null) {
            throw new IllegalArgumentException("Market cannot be null");
        }
        if (to == null) {
            throw new IllegalArgumentException("Timestamp cannot be null");
        }
        if (to.equals(market.getLastAccrual())) {
            return;
        }
        if (to.isBefore(market.getLastAccrual())) {
            throw new IllegalArgumentException("Timestamp cannot be before last accrual");
        }

        BigDecimal utilization = market.utilization();
        BigDecimal borrowRate = market.getRateModel().borrowRate(utilization);
        BigDecimal supplyRate = market.getRateModel().supplyRate(utilization, market.getReserveFactor());
        BigDecimal seconds = BigDecimal.valueOf(ChronoUnit.SECONDS.between(market.getLastAccrual(), to));
        BigDecimal borrowFactor = growthFactor(borrowRate, seconds);
        BigDecimal supplyFactor = growthFactor(supplyRate, seconds);

        String symbol = market.getAsset().getSymbol();
        BigDecimal oldSupplied = market.getTotalSupplied();
        BigDecimal oldBorrowed = market.getTotalBorrowed();
        for (Position position : portfolio.positions()) {
            position.capitalizeSupply(symbol, supplyFactor);
            position.getLoan(symbol).ifPresent(loan -> loan.capitalize(borrowFactor));
        }

        BigDecimal newSupplied = BigDecimal.ZERO;
        BigDecimal newBorrowed = BigDecimal.ZERO;
        for (Position position : portfolio.positions()) {
            newSupplied = newSupplied.add(position.supplyOf(symbol));
            newBorrowed = newBorrowed.add(position.getLoan(symbol).map(Loan::getOutstanding).orElse(BigDecimal.ZERO));
        }
        BigDecimal addedReserves = newBorrowed.subtract(oldBorrowed).subtract(newSupplied.subtract(oldSupplied));
        market.bookAccrual(newSupplied, newBorrowed, addedReserves, to);
    }

    public void accrueAll(LocalDateTime to) {
        // DONE: accrue every market in the book to the same timestamp.
        for (Market market : markets.all()) {
            accrue(market, to);
        }
    }

    private static BigDecimal growthFactor(BigDecimal annualRate, BigDecimal seconds) {
        BigDecimal growth = annualRate.multiply(seconds)
                .setScale(Precision.SCALE, Precision.ROUNDING)
                .divide(Precision.SECONDS_PER_YEAR, Precision.SCALE, Precision.ROUNDING);
        return BigDecimal.ONE.add(growth).setScale(Precision.SCALE, Precision.ROUNDING);
    }
}
