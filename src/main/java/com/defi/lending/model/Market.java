package com.defi.lending.model;

import com.defi.lending.rates.InterestRateModel;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * One asset's lending pool: governance parameters plus the cash and the claims on it.
 * Health and interest are not computed here. The pool only records the transitions those services decide on,
 * so a market can be valued on its own without knowing every user.
 *
 * <p>Amounts are whole tokens. {@code cash} is tokens sitting in the pool.
 * {@code totalSupplied} and {@code totalBorrowed} are claims and grow when interest is capitalized.
 * {@code reserves} is the slice of borrower interest that suppliers do not receive.
 * After every transition the pool should satisfy {@code cash + totalBorrowed = totalSupplied + reserves}.
 */
public final class Market {

    private final Asset asset;
    private final BigDecimal collateralFactor;
    private final BigDecimal liquidationThreshold;
    private final BigDecimal liquidationBonus;
    private final BigDecimal reserveFactor;
    private final BigDecimal closeFactor;
    private final InterestRateModel rateModel;

    private BigDecimal cash;
    private BigDecimal totalSupplied;
    private BigDecimal totalBorrowed;
    private BigDecimal reserves;
    private LocalDateTime lastAccrual;

    public Market(Asset asset, BigDecimal collateralFactor, BigDecimal liquidationThreshold,
                  BigDecimal liquidationBonus, BigDecimal reserveFactor, BigDecimal closeFactor,
                  InterestRateModel rateModel, LocalDateTime openedAt) {
        // DONE: reject a null asset, rateModel, or openedAt.
        // collateralFactor is in (0, 1]. liquidationThreshold is in (0, 1] and >= collateralFactor,
        // otherwise a borrower who used the full loan-to-value would already be underwater.
        // liquidationBonus >= 0. reserveFactor is in [0, 1). closeFactor is in (0, 1].
        // A new pool holds nothing: cash, claims, and reserves start at zero, and interest starts at openedAt.
        if (asset == null) {
            throw new IllegalArgumentException("Asset cannot be null");
        }
        if (rateModel == null) {
            throw new IllegalArgumentException("Rate model cannot be null");
        }
        if (openedAt == null) {
            throw new IllegalArgumentException("Opened at cannot be null");
        }
        if (collateralFactor == null || collateralFactor.compareTo(BigDecimal.ZERO) <= 0 || collateralFactor.compareTo(BigDecimal.ONE) > 0) {
            throw new IllegalArgumentException("Collateral factor must be in the range (0, 1]");
        }
        if (liquidationThreshold == null || liquidationThreshold.compareTo(BigDecimal.ZERO) <= 0 || liquidationThreshold.compareTo(BigDecimal.ONE) > 0 || liquidationThreshold.compareTo(collateralFactor) < 0) {
            throw new IllegalArgumentException("Liquidation threshold must be in the range (0, 1] and >= collateral factor");
        }
        if (liquidationBonus == null || liquidationBonus.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Liquidation bonus must be >= 0");
        }
        if (reserveFactor == null || reserveFactor.compareTo(BigDecimal.ZERO) < 0 || reserveFactor.compareTo(BigDecimal.ONE) >= 0) {
            throw new IllegalArgumentException("Reserve factor must be in the range [0, 1)");
        }
        if (closeFactor == null || closeFactor.compareTo(BigDecimal.ZERO) <= 0 || closeFactor.compareTo(BigDecimal.ONE) > 0) {
            throw new IllegalArgumentException("Close factor must be in the range (0, 1]");
        }
        this.asset = asset;
        this.collateralFactor = collateralFactor;
        this.liquidationThreshold = liquidationThreshold;
        this.liquidationBonus = liquidationBonus;
        this.reserveFactor = reserveFactor;
        this.closeFactor = closeFactor;
        this.rateModel = rateModel;
        this.cash = BigDecimal.ZERO;
        this.totalSupplied = BigDecimal.ZERO;
        this.totalBorrowed = BigDecimal.ZERO;
        this.reserves = BigDecimal.ZERO;
        this.lastAccrual = openedAt;
    }

    public Asset getAsset() {
        return asset;
    }

    /** Loan-to-value cap used on new borrows. A fraction, not a percentage. */
    public BigDecimal getCollateralFactor() {
        return collateralFactor;
    }

    /** Weight of this asset's collateral in the health factor. A fraction, not a percentage. */
    public BigDecimal getLiquidationThreshold() {
        return liquidationThreshold;
    }

    /** Extra collateral, as a fraction of repaid debt value, paid to a liquidator. */
    public BigDecimal getLiquidationBonus() {
        return liquidationBonus;
    }

    /** Fraction of borrower interest withheld from suppliers. */
    public BigDecimal getReserveFactor() {
        return reserveFactor;
    }

    /** Largest share of a debt that one liquidation may repay. */
    public BigDecimal getCloseFactor() {
        return closeFactor;
    }

    public InterestRateModel getRateModel() {
        return rateModel;
    }

    public BigDecimal getCash() {
        return cash;
    }

    public BigDecimal getTotalSupplied() {
        return totalSupplied;
    }

    public BigDecimal getTotalBorrowed() {
        return totalBorrowed;
    }

    public BigDecimal getReserves() {
        return reserves;
    }

    public LocalDateTime getLastAccrual() {
        return lastAccrual;
    }

    public BigDecimal utilization() {
        // DONE: when totalSupplied is zero, return zero. An empty pool is not "fully used".
        // Otherwise return totalBorrowed / totalSupplied, rounded with Precision.
        // Compare with compareTo, not equals.
        if (totalSupplied.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return totalBorrowed.divide(totalSupplied, RoundingMode.HALF_UP);
    }

    public void onDeposit(BigDecimal amount) {
        // DONE: reject a null or non-positive amount. Add it to cash and to totalSupplied.
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount cannot be null or non-positive");
        }
        cash = cash.add(amount);
        totalSupplied = totalSupplied.add(amount);
    }

    public void onWithdraw(BigDecimal amount) {
        // DONE: reject a null or non-positive amount, an amount above cash, or an amount above totalSupplied
        // (IllegalArgumentException). Subtract it from cash and from totalSupplied.
        // This does not check health. LendingService turns a cash shortage into InsufficientLiquidityException
        // before it calls this method, so the market stays free of that exception type.
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount cannot be null or non-positive");
        }
        if (amount.compareTo(cash) > 0) {
            throw new IllegalArgumentException("Amount cannot be greater than cash");
        }
        if (amount.compareTo(totalSupplied) > 0) {
            throw new IllegalArgumentException("Amount cannot be greater than totalSupplied");
        }
        cash = cash.subtract(amount);
        totalSupplied = totalSupplied.subtract(amount);
    }

    public void onBorrow(BigDecimal amount) {
        // DONE: reject a null or non-positive amount, or an amount above cash, with IllegalArgumentException.
        // The pool cannot lend tokens it does not hold. Subtract from cash and add to totalBorrowed.
        // LendingService is what throws InsufficientLiquidityException, and it does so before calling this.
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount cannot be null or non-positive");
        }
        if (amount.compareTo(cash) > 0) {
            throw new IllegalArgumentException("Amount cannot be greater than cash");
        }
        cash = cash.subtract(amount);
        totalBorrowed = totalBorrowed.add(amount);
    }

    public void onRepay(BigDecimal amount) {
        // DONE: reject a null or non-positive amount, or an amount above totalBorrowed.
        // Add to cash and subtract from totalBorrowed.
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount cannot be null or non-positive");
        }
        if (amount.compareTo(totalBorrowed) > 0) {
            throw new IllegalArgumentException("Amount cannot be greater than totalBorrowed");
        }
        cash = cash.add(amount);
        totalBorrowed = totalBorrowed.subtract(amount);
    }

    /**
     * Books an accrual that {@code InterestAccrualService} has already applied to every position.
     * Cash is not a parameter: interest creates claims, not new tokens.
     */
    public void bookAccrual(BigDecimal newTotalSupplied, BigDecimal newTotalBorrowed,
                            BigDecimal addedReserves, LocalDateTime at) {
        // DONE: reject nulls, a timestamp before lastAccrual, a negative reserve increment,
        // or totals that would break cash + newTotalBorrowed = newTotalSupplied + (reserves + addedReserves).
        // Then store the new totals, add the reserves, and set lastAccrual to `at`.
        if (newTotalSupplied == null || newTotalBorrowed == null || addedReserves == null || at == null) {
            throw new IllegalArgumentException("Nulls cannot be passed");
        }
        if (at.compareTo(lastAccrual) < 0) {
            throw new IllegalArgumentException("Timestamp cannot be before last accrual");
        }
        if (addedReserves.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Reserve increment cannot be negative");
        }
        if (cash.add(newTotalBorrowed).compareTo(newTotalSupplied.add(reserves.add(addedReserves))) != 0) {
            throw new IllegalArgumentException("Cash + newTotalBorrowed != newTotalSupplied + (reserves + addedReserves)");
        }
        totalSupplied = newTotalSupplied;
        totalBorrowed = newTotalBorrowed;
        reserves = reserves.add(addedReserves);
        lastAccrual = at;
    }
}
