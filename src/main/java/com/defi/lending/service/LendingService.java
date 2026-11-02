package com.defi.lending.service;

import com.defi.lending.exception.InsufficientCollateralException;
import com.defi.lending.exception.InsufficientLiquidityException;
import com.defi.lending.model.MarketBook;
import com.defi.lending.model.Operation;
import com.defi.lending.model.Portfolio;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Deposit, withdraw, borrow, and repay for one account at a time.
 * Checks health and liquidity, then asks the market and the position to record the primitive change.
 * Interest is accrued to the action's timestamp first, so the check sees current debt.
 */
public final class LendingService {

    private final MarketBook markets;
    private final Portfolio portfolio;
    private final HealthFactorService health;
    private final InterestAccrualService interest;

    public LendingService(MarketBook markets, Portfolio portfolio,
                          HealthFactorService health, InterestAccrualService interest) {
        // TODO: reject any null argument.
        this.markets = markets;
        this.portfolio = portfolio;
        this.health = health;
        this.interest = interest;
    }

    public void run(List<Operation> operations)
            throws InsufficientCollateralException, InsufficientLiquidityException {
        // TODO: reject a null list. Execute each operation in list order. Do not sort:
        // the file is already chronological, and two actions can share a timestamp.
        throw new UnsupportedOperationException("LendingService.run");
    }

    public void execute(Operation operation)
            throws InsufficientCollateralException, InsufficientLiquidityException {
        // TODO: dispatch on the operation type to deposit, withdraw, borrow, or repay.
        // Accrue that asset's market up to the operation timestamp before the balance change.
        throw new UnsupportedOperationException("LendingService.execute");
    }

    public void deposit(String accountId, String symbol, BigDecimal amount, LocalDateTime at) {
        // TODO:
        // - Reject an unknown account or market, a null timestamp, a non-positive amount,
        //   or an amount whose scale is greater than the asset's decimals (IllegalArgumentException).
        // - Accrue the market to `at`.
        // - Debit the wallet, add the tokens to the position's supply, and market.onDeposit.
        // - A deposit can only improve health, so do not compute the health factor.
        throw new UnsupportedOperationException("LendingService.deposit");
    }

    public void withdraw(String accountId, String symbol, BigDecimal amount, LocalDateTime at)
            throws InsufficientCollateralException, InsufficientLiquidityException {
        // TODO:
        // - Reject an unknown account or market, a bad amount (non-positive, or finer than `decimals`),
        //   or an amount above the user's supply (IllegalArgumentException).
        //   An amount above the pool's cash throws InsufficientLiquidityException.
        // - Accrue the market to `at`.
        // - Build a draft position with the supply reduced by the amount (deep copy) and value it.
        //   Refuse with InsufficientCollateralException when the draft has debt and its health factor
        //   is strictly below 1. A factor of exactly 1 is allowed. No debt means the withdraw is fine.
        // - Reduce the position supply, credit the wallet, and market.onWithdraw.
        throw new UnsupportedOperationException("LendingService.withdraw");
    }

    public void borrow(String accountId, String symbol, BigDecimal amount, LocalDateTime at)
            throws InsufficientCollateralException, InsufficientLiquidityException {
        // TODO:
        // - Reject an unknown account or market, or a bad amount, as in deposit.
        // - Accrue the market to `at` before reading cash or debt.
        // - When amount is above the pool's cash, throw InsufficientLiquidityException.
        // - Draft the position with the extra debt and value the draft.
        //   Throw InsufficientCollateralException when the draft debt value is above borrowingPower
        //   (the collateral-factor cap) OR the health factor is strictly below 1.
        //   A health factor of exactly 1 is allowed. Both checks are required: the collateral factor
        //   stops a new borrow from opening already on the liquidation line, and the health factor
        //   is the line itself.
        // - Credit the wallet, increase the position's debt opened at `at`, and market.onBorrow.
        throw new UnsupportedOperationException("LendingService.borrow");
    }

    public void repay(String accountId, String symbol, BigDecimal amount, LocalDateTime at) {
        // TODO:
        // - Reject an unknown account or market, a bad amount, or a repay when the account has no loan
        //   of that asset (IllegalArgumentException).
        // - Accrue the market to `at` first, so the cap below includes interest.
        // - Cap the payment at the outstanding debt. Repaying extra is not a deposit.
        // - Debit the wallet by the capped amount, reduce the position's debt, and market.onRepay.
        throw new UnsupportedOperationException("LendingService.repay");
    }
}
