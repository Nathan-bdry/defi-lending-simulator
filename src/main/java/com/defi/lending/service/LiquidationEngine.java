package com.defi.lending.service;

import com.defi.lending.exception.PositionNotLiquidatableException;
import com.defi.lending.model.MarketBook;
import com.defi.lending.model.Portfolio;
import com.defi.lending.oracle.PriceListener;
import com.defi.lending.oracle.PriceTick;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Turns an oracle update into liquidations of positions that the new price pushed below a health factor of 1.
 * Registered with the oracle as a listener so the oracle itself stays free of liquidation policy.
 */
public final class LiquidationEngine implements PriceListener {

    private final MarketBook markets;
    private final Portfolio portfolio;
    private final HealthFactorService health;
    private final InterestAccrualService interest;

    public LiquidationEngine(MarketBook markets, Portfolio portfolio,
                             HealthFactorService health, InterestAccrualService interest) {
        // TODO: reject any null argument.
        this.markets = markets;
        this.portfolio = portfolio;
        this.health = health;
        this.interest = interest;
    }

    @Override
    public void onPriceUpdate(PriceTick tick) {
        // TODO:
        // - Accrue every market to tick's timestamp before judging health, so interest is included.
        // - Consider only positions that supply or borrow the updated symbol; other health factors did not change.
        // - Skip a position whose health factor is missing or >= 1. Exactly 1 is not liquidatable.
        // - The liquidator is portfolio.liquidator(). When that is empty, throw IllegalStateException:
        //   the scenario file must name one, and Application already refuses to start without one.
        // - For each underwater position, choose the debt with the largest USD value
        //   (tie: alphabetical symbol) and the collateral to seize: the updated symbol when the user
        //   supplies it, otherwise the supplied symbol with the largest USD value (tie: alphabetical).
        // - Repay amount is the minimum of (outstanding * debt market's close factor), the liquidator's
        //   wallet balance of the debt asset, and the repay that the borrower's collateral can cover
        //   at the bonus price (see liquidate). When that minimum is zero, skip the position.
        // - Call liquidate. One call per position per tick: the close factor is the cap, not a loop
        //   that wipes the debt. A later tick may continue.
        // - Do not let a refused liquidation abort the remaining positions or the rest of the replay.
        throw new UnsupportedOperationException("LiquidationEngine.onPriceUpdate");
    }

    /**
     * Repays {@code repayAmount} of the borrower's debt and seizes collateral at the bonus.
     * The bonus and the close factor come from different markets: the bonus is a property of the
     * collateral, the close factor is a property of the debt.
     */
    public void liquidate(String liquidatorId, String borrowerId, String debtSymbol,
                          String collateralSymbol, BigDecimal repayAmount, LocalDateTime at)
            throws PositionNotLiquidatableException {
        // TODO:
        // - Reject unknown accounts or markets, a blank argument, a non-positive repayAmount,
        //   or a liquidator and borrower that are the same account.
        // - Accrue both markets to `at` when `at` is not null. A direct call from a test may pass
        //   the timestamp of the price that made the position unhealthy.
        // - When the borrower's health factor is empty or >= 1, throw PositionNotLiquidatableException.
        //   Exactly 1 throws. This method does not re-check the price trend; the caller already saw the tick.
        // - maxRepay = outstanding * debtMarket.closeFactor. A repayAmount above maxRepay is IllegalArgumentException.
        // - seizeUsd = repayAmount * debtUnitValue * (1 + collateralMarket.liquidationBonus).
        //   seizeTokens = seizeUsd / collateralUnitValue.
        //   When seizeTokens would exceed the borrower's supply, scale repayAmount down to
        //   supplyTokens * collateralUnitValue / (debtUnitValue * (1 + bonus)), and seize the whole supply.
        //   When that scaled repay is zero, throw IllegalArgumentException.
        // - Also cap repayAmount at the liquidator's wallet balance of the debt asset.
        // - Debit the liquidator, reduce the borrower's debt, market.onRepay on the debt asset.
        // - Reduce the borrower's collateral, market.onWithdraw of the seized tokens, credit the liquidator.
        //   Seized collateral leaves the pool; it is not a new deposit for the liquidator.
        // Round every product and quotient with Precision.
        throw new UnsupportedOperationException("LiquidationEngine.liquidate");
    }
}
