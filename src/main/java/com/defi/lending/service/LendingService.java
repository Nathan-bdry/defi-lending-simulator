package com.defi.lending.service;

import com.defi.lending.exception.InsufficientCollateralException;
import com.defi.lending.exception.InsufficientLiquidityException;
import com.defi.lending.model.Account;
import com.defi.lending.model.Loan;
import com.defi.lending.model.Market;
import com.defi.lending.model.MarketBook;
import com.defi.lending.model.Operation;
import com.defi.lending.model.Portfolio;
import com.defi.lending.model.Position;

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
        // DONE: reject any null argument.
        if (markets == null) {
            throw new IllegalArgumentException("Market book cannot be null");
        }
        if (portfolio == null) {
            throw new IllegalArgumentException("Portfolio cannot be null");
        }
        if (health == null) {
            throw new IllegalArgumentException("Health factor service cannot be null");
        }
        if (interest == null) {
            throw new IllegalArgumentException("Interest accrual service cannot be null");
        }
        this.markets = markets;
        this.portfolio = portfolio;
        this.health = health;
        this.interest = interest;
    }

    public void run(List<Operation> operations)
            throws InsufficientCollateralException, InsufficientLiquidityException {
        // DONE: reject a null list. Execute each operation in list order. Do not sort:
        // the file is already chronological, and two actions can share a timestamp.
        if (operations == null) {
            throw new IllegalArgumentException("Operations cannot be null");
        }
        for (Operation operation : operations) {
            execute(operation);
        }
    }

    public void execute(Operation operation)
            throws InsufficientCollateralException, InsufficientLiquidityException {
        // DONE: dispatch on the operation type to deposit, withdraw, borrow, or repay.
        // Accrue that asset's market up to the operation timestamp before the balance change.
        if (operation == null) {
            throw new IllegalArgumentException("Operation cannot be null");
        }
        switch (operation.getType()) {
            case DEPOSIT -> deposit(operation.getAccountId(), operation.getSymbol(), operation.getAmount(),
                    operation.getTimestamp());
            case WITHDRAW -> withdraw(operation.getAccountId(), operation.getSymbol(), operation.getAmount(),
                    operation.getTimestamp());
            case BORROW -> borrow(operation.getAccountId(), operation.getSymbol(), operation.getAmount(),
                    operation.getTimestamp());
            case REPAY -> repay(operation.getAccountId(), operation.getSymbol(), operation.getAmount(),
                    operation.getTimestamp());
        }
    }

    public void deposit(String accountId, String symbol, BigDecimal amount, LocalDateTime at) {
        // DONE:
        // - Reject an unknown account or market, a null timestamp, a non-positive amount,
        //   or an amount whose scale is greater than the asset's decimals (IllegalArgumentException).
        // - Accrue the market to `at`.
        // - Debit the wallet, add the tokens to the position's supply, and market.onDeposit.
        // - A deposit can only improve health, so do not compute the health factor.
        Account account = requireAccount(accountId);
        Position position = requirePosition(accountId);
        Market market = requireMarket(symbol);
        requireTimestamp(at);
        requireAmount(market, amount);
        interest.accrue(market, at);
        account.debit(symbol, amount);
        position.addSupply(symbol, amount);
        market.onDeposit(amount);
    }

    public void withdraw(String accountId, String symbol, BigDecimal amount, LocalDateTime at)
            throws InsufficientCollateralException, InsufficientLiquidityException {
        // DONE:
        // - Reject an unknown account or market, a bad amount (non-positive, or finer than `decimals`),
        //   or an amount above the user's supply (IllegalArgumentException).
        //   An amount above the pool's cash throws InsufficientLiquidityException.
        // - Accrue the market to `at`.
        // - Build a draft position with the supply reduced by the amount (deep copy) and value it.
        //   Refuse with InsufficientCollateralException when the draft has debt and its health factor
        //   is strictly below 1. A factor of exactly 1 is allowed. No debt means the withdraw is fine.
        // - Reduce the position supply, credit the wallet, and market.onWithdraw.
        Account account = requireAccount(accountId);
        Position position = requirePosition(accountId);
        Market market = requireMarket(symbol);
        requireTimestamp(at);
        requireAmount(market, amount);
        interest.accrue(market, at);
        if (amount.compareTo(position.supplyOf(symbol)) > 0) {
            throw new IllegalArgumentException("Amount cannot be greater than the supply");
        }
        if (amount.compareTo(market.getCash()) > 0) {
            throw new InsufficientLiquidityException("Insufficient cash for " + symbol);
        }
        Position draft = position.toBuilder().build();
        draft.reduceSupply(symbol, amount);
        if (health.debtValue(draft).compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal factor = health.healthFactor(draft).orElseThrow();
            if (factor.compareTo(BigDecimal.ONE) < 0) {
                throw new InsufficientCollateralException("Health factor below 1");
            }
        }
        position.reduceSupply(symbol, amount);
        account.credit(symbol, amount);
        market.onWithdraw(amount);
    }

    public void borrow(String accountId, String symbol, BigDecimal amount, LocalDateTime at)
            throws InsufficientCollateralException, InsufficientLiquidityException {
        // DONE:
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
        Account account = requireAccount(accountId);
        Position position = requirePosition(accountId);
        Market market = requireMarket(symbol);
        requireTimestamp(at);
        requireAmount(market, amount);
        interest.accrue(market, at);
        if (amount.compareTo(market.getCash()) > 0) {
            throw new InsufficientLiquidityException("Insufficient cash for " + symbol);
        }
        Position draft = position.toBuilder().build();
        draft.increaseDebt(symbol, amount, at);
        if (health.debtValue(draft).compareTo(health.borrowingPower(draft)) > 0) {
            throw new InsufficientCollateralException("Debt exceeds borrowing power");
        }
        BigDecimal factor = health.healthFactor(draft).orElseThrow();
        if (factor.compareTo(BigDecimal.ONE) < 0) {
            throw new InsufficientCollateralException("Health factor below 1");
        }
        account.credit(symbol, amount);
        position.increaseDebt(symbol, amount, at);
        market.onBorrow(amount);
    }

    public void repay(String accountId, String symbol, BigDecimal amount, LocalDateTime at) {
        // DONE:
        // - Reject an unknown account or market, a bad amount, or a repay when the account has no loan
        //   of that asset (IllegalArgumentException).
        // - Accrue the market to `at` first, so the cap below includes interest.
        // - Cap the payment at the outstanding debt. Repaying extra is not a deposit.
        // - Debit the wallet by the capped amount, reduce the position's debt, and market.onRepay.
        Account account = requireAccount(accountId);
        Position position = requirePosition(accountId);
        Market market = requireMarket(symbol);
        requireTimestamp(at);
        requireAmount(market, amount);
        if (position.getLoan(symbol).isEmpty()) {
            throw new IllegalArgumentException("No loan for " + symbol);
        }
        interest.accrue(market, at);
        Loan loan = position.getLoan(symbol).orElseThrow(
                () -> new IllegalArgumentException("No loan for " + symbol));
        BigDecimal payment = amount.min(loan.getOutstanding());
        account.debit(symbol, payment);
        position.reduceDebt(symbol, payment);
        market.onRepay(payment);
    }

    private Account requireAccount(String accountId) {
        return portfolio.findAccount(accountId).orElseThrow(
                () -> new IllegalArgumentException("Unknown account " + accountId));
    }

    private Position requirePosition(String accountId) {
        return portfolio.findPosition(accountId).orElseThrow(
                () -> new IllegalArgumentException("Unknown account " + accountId));
    }

    private Market requireMarket(String symbol) {
        if (symbol == null) {
            throw new IllegalArgumentException("Unknown market null");
        }
        return markets.find(symbol).orElseThrow(
                () -> new IllegalArgumentException("Unknown market " + symbol));
    }

    private static void requireTimestamp(LocalDateTime at) {
        if (at == null) {
            throw new IllegalArgumentException("Timestamp cannot be null");
        }
    }

    private static void requireAmount(Market market, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount cannot be null or non-positive");
        }
        if (amount.scale() > market.getAsset().getDecimals()) {
            throw new IllegalArgumentException(
                    "Amount has more decimal places than " + market.getAsset().getSymbol() + " allows");
        }
    }
}
