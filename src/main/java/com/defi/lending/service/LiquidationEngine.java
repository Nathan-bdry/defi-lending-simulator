package com.defi.lending.service;

import com.defi.lending.Precision;
import com.defi.lending.exception.PositionNotLiquidatableException;
import com.defi.lending.model.Account;
import com.defi.lending.model.Loan;
import com.defi.lending.model.Market;
import com.defi.lending.model.MarketBook;
import com.defi.lending.model.Portfolio;
import com.defi.lending.model.Position;
import com.defi.lending.oracle.PriceListener;
import com.defi.lending.oracle.PriceTick;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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
        // reject any null argument.
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

    @Override
    public void onPriceUpdate(PriceTick tick) {
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
        if (tick == null) {
            throw new IllegalArgumentException("Tick cannot be null");
        }
        interest.accrueAll(tick.getTimestamp());
        List<Position> underwater = new ArrayList<>();
        for (Position position : portfolio.positions()) {
            if (!involved(position, tick.getSymbol())) {
                continue;
            }
            Optional<BigDecimal> factor = health.healthFactor(position);
            if (factor.isEmpty() || factor.get().compareTo(BigDecimal.ONE) >= 0) {
                continue;
            }
            underwater.add(position);
        }
        if (underwater.isEmpty()) {
            return;
        }
        Account liquidator = portfolio.liquidator().orElseThrow(
                () -> new IllegalStateException("A liquidator is required"));
        for (Position position : underwater) {
            Account borrower = position.getAccount();
            Loan debt = chooseDebt(borrower, position);
            String collateralSymbol = chooseCollateral(borrower, position, tick.getSymbol());
            if (debt == null || collateralSymbol == null) {
                continue;
            }
            Market debtMarket = requireMarket(debt.getAssetSymbol());
            Market collateralMarket = requireMarket(collateralSymbol);
            BigDecimal debtUnit = unitValue(borrower, debt.getAssetSymbol());
            BigDecimal collateralUnit = unitValue(borrower, collateralSymbol);
            BigDecimal maxRepay = debt.getOutstanding().multiply(debtMarket.getCloseFactor())
                    .setScale(Precision.SCALE, Precision.ROUNDING);
            BigDecimal covered = repayCoveredBySupply(position.supplyOf(collateralSymbol), debtUnit, collateralUnit,
                    bonusMultiplier(collateralMarket));
            BigDecimal repay = maxRepay.min(liquidator.getWalletBalance(debt.getAssetSymbol())).min(covered);
            if (repay.compareTo(BigDecimal.ZERO) == 0) {
                continue;
            }
            try {
                liquidate(liquidator.getId(), borrower.getId(), debt.getAssetSymbol(), collateralSymbol, repay,
                        tick.getTimestamp());
            } catch (PositionNotLiquidatableException | IllegalArgumentException ex) {
                // This position was refused. The other positions in the tick still run.
            }
        }
    }

    /**
     * Repays {@code repayAmount} of the borrower's debt and seizes collateral at the bonus.
     * The bonus and the close factor come from different markets: the bonus is a property of the
     * collateral, the close factor is a property of the debt.
     */
    public void liquidate(String liquidatorId, String borrowerId, String debtSymbol,
                          String collateralSymbol, BigDecimal repayAmount, LocalDateTime at)
            throws PositionNotLiquidatableException {
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
        requireText(liquidatorId, "Liquidator");
        requireText(borrowerId, "Borrower");
        requireText(debtSymbol, "Debt symbol");
        requireText(collateralSymbol, "Collateral symbol");
        if (repayAmount == null || repayAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Repay amount cannot be null or non-positive");
        }
        if (liquidatorId.equals(borrowerId)) {
            throw new IllegalArgumentException("Liquidator and borrower cannot be the same account");
        }
        Account liquidator = portfolio.findAccount(liquidatorId).orElseThrow(
                () -> new IllegalArgumentException("Unknown account " + liquidatorId));
        Account borrower = portfolio.findAccount(borrowerId).orElseThrow(
                () -> new IllegalArgumentException("Unknown account " + borrowerId));
        Position position = portfolio.findPosition(borrowerId).orElseThrow(
                () -> new IllegalArgumentException("Unknown account " + borrowerId));
        Market debtMarket = markets.find(debtSymbol).orElseThrow(
                () -> new IllegalArgumentException("Unknown market " + debtSymbol));
        Market collateralMarket = markets.find(collateralSymbol).orElseThrow(
                () -> new IllegalArgumentException("Unknown market " + collateralSymbol));
        if (at != null) {
            interest.accrue(debtMarket, at);
            if (!debtSymbol.equals(collateralSymbol)) {
                interest.accrue(collateralMarket, at);
            }
        }
        Optional<BigDecimal> factor = health.healthFactor(position);
        if (factor.isEmpty() || factor.get().compareTo(BigDecimal.ONE) >= 0) {
            throw new PositionNotLiquidatableException("Health factor is not below 1");
        }
        Loan loan = position.getLoan(debtSymbol).orElseThrow(
                () -> new IllegalArgumentException("No loan for " + debtSymbol));
        BigDecimal maxRepay = loan.getOutstanding().multiply(debtMarket.getCloseFactor())
                .setScale(Precision.SCALE, Precision.ROUNDING);
        if (repayAmount.compareTo(maxRepay) > 0) {
            throw new IllegalArgumentException("Repay amount exceeds the close factor");
        }

        BigDecimal multiplier = bonusMultiplier(collateralMarket);
        BigDecimal debtUnit = unitValue(borrower, debtSymbol);
        BigDecimal collateralUnit = unitValue(borrower, collateralSymbol);
        BigDecimal supply = position.supplyOf(collateralSymbol);
        BigDecimal payment = repayAmount;
        boolean seizeAll = false;
        if (collateralUnit.compareTo(BigDecimal.ZERO) == 0
                || seizeTokens(payment, debtUnit, collateralUnit, multiplier).compareTo(supply) > 0) {
            payment = repayCoveredBySupply(supply, debtUnit, collateralUnit, multiplier);
            if (payment.compareTo(BigDecimal.ZERO) == 0) {
                throw new IllegalArgumentException("Collateral cannot cover a positive repayment");
            }
            seizeAll = true;
        }
        if (payment.compareTo(liquidator.getWalletBalance(debtSymbol)) > 0) {
            payment = liquidator.getWalletBalance(debtSymbol);
            seizeAll = false;
        }
        if (payment.compareTo(BigDecimal.ZERO) == 0) {
            throw new IllegalArgumentException("Liquidator cannot pay the debt");
        }
        BigDecimal seized = seizeAll ? supply : seizeTokens(payment, debtUnit, collateralUnit, multiplier);
        if (seized.compareTo(BigDecimal.ZERO) == 0) {
            throw new IllegalArgumentException("Seized collateral is zero");
        }
        if (seized.compareTo(supply) > 0) {
            seized = supply;
        }

        liquidator.debit(debtSymbol, payment);
        position.reduceDebt(debtSymbol, payment);
        debtMarket.onRepay(payment);
        position.reduceSupply(collateralSymbol, seized);
        collateralMarket.onWithdraw(seized);
        liquidator.credit(collateralSymbol, seized);
    }

    private static boolean involved(Position position, String symbol) {
        return position.supplyOf(symbol).compareTo(BigDecimal.ZERO) > 0
                || position.getLoan(symbol).isPresent();
    }

    private Loan chooseDebt(Account account, Position position) {
        Loan chosen = null;
        BigDecimal chosenUsd = null;
        for (Loan loan : position.getLoans()) {
            BigDecimal usd = health.debtValue(Position.builder(account).loan(loan.toBuilder().build()).build());
            if (chosen == null || usd.compareTo(chosenUsd) > 0
                    || (usd.compareTo(chosenUsd) == 0
                    && loan.getAssetSymbol().compareTo(chosen.getAssetSymbol()) < 0)) {
                chosen = loan;
                chosenUsd = usd;
            }
        }
        return chosen;
    }

    private String chooseCollateral(Account account, Position position, String updated) {
        if (position.supplyOf(updated).compareTo(BigDecimal.ZERO) > 0) {
            return updated;
        }
        String chosen = null;
        BigDecimal chosenUsd = null;
        for (String symbol : position.suppliedSymbols()) {
            BigDecimal usd = health.collateralValue(
                    Position.builder(account).supply(symbol, position.supplyOf(symbol)).build());
            if (chosen == null || usd.compareTo(chosenUsd) > 0
                    || (usd.compareTo(chosenUsd) == 0 && symbol.compareTo(chosen) < 0)) {
                chosen = symbol;
                chosenUsd = usd;
            }
        }
        return chosen;
    }

    private Market requireMarket(String symbol) {
        return markets.find(symbol).orElseThrow(
                () -> new IllegalStateException("No market for " + symbol));
    }

    /** USD value of one token, rounded the same way {@link HealthFactorService} values a balance. */
    private BigDecimal unitValue(Account account, String symbol) {
        return health.collateralValue(Position.builder(account).supply(symbol, BigDecimal.ONE).build());
    }

    private static BigDecimal bonusMultiplier(Market collateralMarket) {
        return BigDecimal.ONE.add(collateralMarket.getLiquidationBonus());
    }

    private static BigDecimal seizeTokens(BigDecimal repay, BigDecimal debtUnit, BigDecimal collateralUnit,
                                          BigDecimal multiplier) {
        return repay.multiply(debtUnit)
                .setScale(Precision.SCALE, Precision.ROUNDING)
                .multiply(multiplier)
                .setScale(Precision.SCALE, Precision.ROUNDING)
                .divide(collateralUnit, Precision.SCALE, Precision.ROUNDING);
    }

    private static BigDecimal repayCoveredBySupply(BigDecimal supply, BigDecimal debtUnit,
                                                   BigDecimal collateralUnit, BigDecimal multiplier) {
        if (debtUnit.compareTo(BigDecimal.ZERO) == 0 || collateralUnit.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal denominator = debtUnit.multiply(multiplier).setScale(Precision.SCALE, Precision.ROUNDING);
        return supply.multiply(collateralUnit)
                .setScale(Precision.SCALE, Precision.ROUNDING)
                .divide(denominator, Precision.SCALE, Precision.ROUNDING);
    }

    private static void requireText(String value, String label) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(label + " cannot be blank");
        }
    }
}
