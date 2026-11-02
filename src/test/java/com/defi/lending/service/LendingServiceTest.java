package com.defi.lending.service;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * User actions for {@link LendingService}: wallet versus pool, the health-factor guard, and liquidity.
 */
class LendingServiceTest {

    @Test
    @Disabled("TODO")
    void depositMovesTokensFromTheWalletIntoThePool() {
        // Normal: wallet decreases, position supply increases, market cash and totalSupplied increase by the same amount.
        fail("TODO");
    }

    @Test
    @Disabled("TODO")
    void depositRejectsANonPositiveAmount() {
        // Invalid: zero and negative amounts throw IllegalArgumentException and change nothing.
        fail("TODO");
    }

    @Test
    @Disabled("TODO")
    void depositRejectsAnUnknownAccountOrMarket() {
        // Invalid: an id or symbol that was not loaded throws IllegalArgumentException.
        fail("TODO");
    }

    @Test
    @Disabled("TODO")
    void depositRejectsMoreDecimalPlacesThanTheAssetAllows() {
        // Invalid: an amount whose scale is greater than the asset's decimals is rejected.
        // Amounts are whole tokens, so this is about fractional tokens, not wei.
        fail("TODO");
    }

    @Test
    @Disabled("TODO")
    void withdrawWithNoDebtReturnsTheSupply() {
        // Normal: a supplier who has not borrowed can withdraw the full supply. Wallet and cash move together.
        fail("TODO");
    }

    @Test
    @Disabled("TODO")
    void withdrawThatLeavesHealthFactorExactlyOneSucceeds() {
        // Edge: a withdraw whose draft health factor compares equal to 1 is allowed.
        fail("TODO");
    }

    @Test
    @Disabled("TODO")
    void withdrawThatWouldDropHealthFactorBelowOneIsRefused() {
        // Exception: a withdraw whose draft health factor is strictly below 1 throws
        // InsufficientCollateralException and leaves balances unchanged.
        fail("TODO");
    }

    @Test
    @Disabled("TODO")
    void withdrawAboveTheUsersSupplyIsRejected() {
        // Invalid: more than the position supplies throws IllegalArgumentException, even when the pool has the cash.
        fail("TODO");
    }

    @Test
    @Disabled("TODO")
    void withdrawAbovePoolCashThrowsInsufficientLiquidity() {
        // Edge: the user supplied the tokens, but they have been borrowed by someone else.
        // InsufficientLiquidityException, and the position is unchanged.
        fail("TODO");
    }

    @Test
    @Disabled("TODO")
    void borrowWithinCollateralAndLiquidityCreditsTheWallet() {
        // Normal: debt and totalBorrowed increase, cash decreases, and the wallet receives the tokens.
        fail("TODO");
    }

    @Test
    @Disabled("TODO")
    void borrowThatLeavesHealthFactorExactlyOneSucceeds() {
        // Edge: a draft health factor of exactly 1 is allowed when the collateral-factor cap is also met.
        fail("TODO");
    }

    @Test
    @Disabled("TODO")
    void borrowPastTheCollateralFactorIsRefusedEvenIfHealthFactorStaysAboveOne() {
        // Exception: debt value above borrowing power throws InsufficientCollateralException.
        // This is the loan-to-value cap. The health factor can still be above 1 because the
        // liquidation threshold is looser than the collateral factor, and the borrow is still refused.
        fail("TODO");
    }

    @Test
    @Disabled("TODO")
    void borrowThatWouldDropHealthFactorBelowOneIsRefused() {
        // Exception: draft health factor strictly below 1 throws InsufficientCollateralException.
        fail("TODO");
    }

    @Test
    @Disabled("TODO")
    void borrowLargerThanPoolCashThrowsInsufficientLiquidity() {
        // Exception: amount above market cash throws InsufficientLiquidityException before any balance changes.
        fail("TODO");
    }

    @Test
    @Disabled("TODO")
    void repayReducesDebtAndPullsTokensFromTheWallet() {
        // Normal: outstanding debt and totalBorrowed decrease, cash increases, wallet decreases.
        fail("TODO");
    }

    @Test
    @Disabled("TODO")
    void repayAboveOutstandingIsCappedAtTheDebt() {
        // Edge: paying more than the debt repays the debt and only that much leaves the wallet.
        // The surplus is not deposited.
        fail("TODO");
    }

    @Test
    @Disabled("TODO")
    void actionAccruesInterestUpToItsTimestampBeforeTheHealthCheck() {
        // Normal: a later borrow sees debt that has grown since the previous action.
        // The health check uses the accrued outstanding, not the original principal.
        fail("TODO");
    }
}
