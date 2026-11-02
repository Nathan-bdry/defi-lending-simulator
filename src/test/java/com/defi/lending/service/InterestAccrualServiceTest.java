package com.defi.lending.service;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * Time and utilization for {@link InterestAccrualService}.
 * The shape of a rate given a utilization is covered on the rate-model tests.
 */
class InterestAccrualServiceTest {

    @Test
    @Disabled("TODO")
    void utilizationIsBorrowedDividedBySupplied() {
        // Normal: totalBorrowed / totalSupplied on the claim balances, not on cash.
        fail("TODO");
    }

    @Test
    @Disabled("TODO")
    void utilizationOfAnEmptyPoolIsZero() {
        // Edge: totalSupplied of zero yields utilization zero, including a brand-new market.
        fail("TODO");
    }

    @Test
    @Disabled("TODO")
    void zeroDurationDoesNotChangeBalances() {
        // Edge: accruing to the current lastAccrual leaves supplies, debts, reserves, cash, and lastAccrual unchanged.
        fail("TODO");
    }

    @Test
    @Disabled("TODO")
    void timestampBeforeLastAccrualIsRejected() {
        // Invalid: a timestamp before lastAccrual throws IllegalArgumentException and changes nothing.
        fail("TODO");
    }

    @Test
    @Disabled("TODO")
    void accrualGrowsBorrowerDebtAndSupplierBalances() {
        // Normal: over a positive duration, each borrower of that asset is multiplied by the borrow factor
        // and each supplier by the supply factor. A position that does not use the asset is untouched.
        fail("TODO");
    }

    @Test
    @Disabled("TODO")
    void reserveFactorWithholdsPartOfBorrowerInterestFromSuppliers() {
        // Normal: supplier growth is borrow interest times (1 - reserveFactor).
        // The residual is added to reserves, not paid to any wallet.
        fail("TODO");
    }

    @Test
    @Disabled("TODO")
    void cashDoesNotChangeWhenInterestAccrues() {
        // Edge: interest creates claims. Cash, the tokens physically in the pool, stays put.
        fail("TODO");
    }

    @Test
    @Disabled("TODO")
    void poolInvariantHoldsAfterAccrual() {
        // Normal: after accrual, cash + totalBorrowed compares equal to totalSupplied + reserves.
        // Market totals equal the sum of position supplies and the sum of position debts for that asset.
        fail("TODO");
    }
}
