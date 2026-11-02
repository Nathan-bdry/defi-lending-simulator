package com.defi.lending.service;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * Liquidation rules for {@link LiquidationEngine}, including the price-listener path.
 */
class LiquidationEngineTest {

    @Test
    @Disabled("TODO")
    void priceThatKeepsHealthFactorAboveOneDoesNotLiquidate() {
        // Normal: a tick that leaves every holder of that symbol at a health factor above 1
        // changes the stored price and no balances.
        fail("TODO");
    }

    @Test
    @Disabled("TODO")
    void healthFactorOfExactlyOneIsNotLiquidated() {
        // Edge: a tick that lands a position on a health factor of exactly 1 seizes nothing.
        // A direct liquidate call for that position throws PositionNotLiquidatableException.
        fail("TODO");
    }

    @Test
    @Disabled("TODO")
    void priceDropBelowOneRepaysDebtAndSeizesCollateralWithTheBonus() {
        // Normal: health factor strictly below 1. The liquidator's wallet pays the debt asset,
        // the borrower's debt shrinks, and the liquidator receives collateral worth
        // repay * debtUnitValue * (1 + liquidationBonus). The bonus is the collateral market's bonus.
        // Pool cash of the debt asset rises; pool cash of the collateral falls.
        fail("TODO");
    }

    @Test
    @Disabled("TODO")
    void repayOfExactlyTheCloseFactorSucceeds() {
        // Edge: repayAmount equal to outstanding * closeFactor is accepted. The close factor is the debt market's.
        fail("TODO");
    }

    @Test
    @Disabled("TODO")
    void repayAboveTheCloseFactorIsRejected() {
        // Invalid: repayAmount strictly above outstanding * closeFactor throws IllegalArgumentException
        // and moves nothing.
        fail("TODO");
    }

    @Test
    @Disabled("TODO")
    void seizedCollateralIsCappedAtWhatTheBorrowerHas() {
        // Edge: when the bonus would seize more tokens than the borrower supplies, the repay is scaled down
        // and the whole collateral line is taken. The liquidator does not receive tokens the pool does not hold.
        fail("TODO");
    }

    @Test
    @Disabled("TODO")
    void liquidatorWithAnEmptyWalletDoesNotSeizeAnything() {
        // Edge: on a price tick, a liquidator who cannot pay the debt asset is skipped.
        // The underwater position stays underwater for the risk report. Other positions in the same tick still run.
        fail("TODO");
    }

    @Test
    @Disabled("TODO")
    void oneTickLiquidatesAPositionOnlyOnce() {
        // Edge: a single tick repays at most one close-factor chunk. It does not loop until the health factor
        // is back to 1. A second tick may liquidate the remainder.
        fail("TODO");
    }

    @Test
    @Disabled("TODO")
    void directLiquidationOfAHealthyPositionIsRefused() {
        // Exception: liquidate on a position with no debt, or with a health factor above 1,
        // throws PositionNotLiquidatableException.
        fail("TODO");
    }
}
