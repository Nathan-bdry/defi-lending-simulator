package com.defi.lending.service;

import com.defi.lending.model.Market;
import com.defi.lending.model.MarketBook;
import com.defi.lending.model.Portfolio;

import java.time.LocalDateTime;

/**
 * Capitalizes interest onto every supply and debt of a market, then books the pool totals.
 * Rates come from the market's {@code InterestRateModel}; this class only applies them across time.
 */
public final class InterestAccrualService {

    private final MarketBook markets;
    private final Portfolio portfolio;

    public InterestAccrualService(MarketBook markets, Portfolio portfolio) {
        // TODO: reject a null book or portfolio.
        this.markets = markets;
        this.portfolio = portfolio;
    }

    /**
     * Brings one market, and every position in it, forward to {@code to}.
     * Reads utilization before changing any balance, then updates positions, then the market.
     */
    public void accrue(Market market, LocalDateTime to) {
        // TODO:
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
        throw new UnsupportedOperationException("InterestAccrualService.accrue");
    }

    public void accrueAll(LocalDateTime to) {
        // TODO: accrue every market in the book to the same timestamp.
        throw new UnsupportedOperationException("InterestAccrualService.accrueAll");
    }
}
