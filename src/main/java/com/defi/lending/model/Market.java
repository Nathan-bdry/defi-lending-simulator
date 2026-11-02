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
        // TODO: reject a null asset, rateModel, or openedAt.
        // collateralFactor is in (0, 1]. liquidationThreshold is in (0, 1] and >= collateralFactor,
        // otherwise a borrower who used the full loan-to-value would already be underwater.
        // liquidationBonus >= 0. reserveFactor is in [0, 1). closeFactor is in (0, 1].
        // A new pool holds nothing: cash, claims, and reserves start at zero, and interest starts at openedAt.
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
        // TODO: when totalSupplied is zero, return zero. An empty pool is not "fully used".
        // Otherwise return totalBorrowed / totalSupplied, rounded with Precision.
        // Compare with compareTo, not equals.
        throw new UnsupportedOperationException("Market.utilization");
    }

    public void onDeposit(BigDecimal amount) {
        // TODO: reject a null or non-positive amount. Add it to cash and to totalSupplied.
        throw new UnsupportedOperationException("Market.onDeposit");
    }

    public void onWithdraw(BigDecimal amount) {
        // TODO: reject a null or non-positive amount, an amount above cash, or an amount above totalSupplied
        // (IllegalArgumentException). Subtract it from cash and from totalSupplied.
        // This does not check health. LendingService turns a cash shortage into InsufficientLiquidityException
        // before it calls this method, so the market stays free of that exception type.
        throw new UnsupportedOperationException("Market.onWithdraw");
    }

    public void onBorrow(BigDecimal amount) {
        // TODO: reject a null or non-positive amount, or an amount above cash, with IllegalArgumentException.
        // The pool cannot lend tokens it does not hold. Subtract from cash and add to totalBorrowed.
        // LendingService is what throws InsufficientLiquidityException, and it does so before calling this.
        throw new UnsupportedOperationException("Market.onBorrow");
    }

    public void onRepay(BigDecimal amount) {
        // TODO: reject a null or non-positive amount, or an amount above totalBorrowed.
        // Add to cash and subtract from totalBorrowed.
        throw new UnsupportedOperationException("Market.onRepay");
    }

    /**
     * Books an accrual that {@code InterestAccrualService} has already applied to every position.
     * Cash is not a parameter: interest creates claims, not new tokens.
     */
    public void bookAccrual(BigDecimal newTotalSupplied, BigDecimal newTotalBorrowed,
                            BigDecimal addedReserves, LocalDateTime at) {
        // TODO: reject nulls, a timestamp before lastAccrual, a negative reserve increment,
        // or totals that would break cash + newTotalBorrowed = newTotalSupplied + (reserves + addedReserves).
        // Then store the new totals, add the reserves, and set lastAccrual to `at`.
        throw new UnsupportedOperationException("Market.bookAccrual");
    }
}
