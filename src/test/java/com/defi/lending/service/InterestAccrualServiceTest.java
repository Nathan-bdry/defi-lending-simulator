package com.defi.lending.service;

import com.defi.lending.Precision;
import com.defi.lending.model.Account;
import com.defi.lending.model.AccountRole;
import com.defi.lending.model.Loan;
import com.defi.lending.model.Market;
import com.defi.lending.model.MarketBook;
import com.defi.lending.model.Portfolio;
import com.defi.lending.model.Position;
import com.defi.lending.model.Stablecoin;
import com.defi.lending.rates.LinearRateModel;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Time and utilization for {@link InterestAccrualService}.
 * The shape of a rate given a utilization is covered on the rate-model tests.
 */
class InterestAccrualServiceTest {

    private static final LocalDateTime OPENED = LocalDateTime.parse("2026-03-01T00:00:00");
    private static final LocalDateTime ONE_YEAR_LATER = OPENED.plusSeconds(Precision.SECONDS_PER_YEAR.longValue());

    @Test
    void utilizationIsBorrowedDividedBySupplied() {
        // Normal: totalBorrowed / totalSupplied on the claim balances, not on cash.
        Market market = usdcMarket("0.10", "0.10");
        market.onDeposit(new BigDecimal("100"));
        market.onBorrow(new BigDecimal("40"));

        assertEquals(0, new BigDecimal("0.4").compareTo(market.utilization()));
        assertEquals(0, new BigDecimal("60").compareTo(market.getCash()));
    }

    @Test
    void utilizationOfAnEmptyPoolIsZero() {
        // Edge: totalSupplied of zero yields utilization zero, including a brand-new market.
        Market market = usdcMarket("0.10", "0.10");

        assertEquals(0, BigDecimal.ZERO.compareTo(market.utilization()));
    }

    @Test
    void zeroDurationDoesNotChangeBalances() {
        // Edge: accruing to the current lastAccrual leaves supplies, debts, reserves, cash, and lastAccrual unchanged.
        Fixture book = funded();

        book.interest.accrue(book.market, OPENED);

        assertEquals(0, new BigDecimal("100").compareTo(book.alice.supplyOf("USDC")));
        assertEquals(0, new BigDecimal("40").compareTo(book.bob.getLoan("USDC").orElseThrow().getOutstanding()));
        assertEquals(0, BigDecimal.ZERO.compareTo(book.market.getReserves()));
        assertEquals(0, new BigDecimal("60").compareTo(book.market.getCash()));
        assertEquals(OPENED, book.market.getLastAccrual());
    }

    @Test
    void timestampBeforeLastAccrualIsRejected() {
        // Invalid: a timestamp before lastAccrual throws IllegalArgumentException and changes nothing.
        Fixture book = funded();

        assertThrows(IllegalArgumentException.class, () -> book.interest.accrue(book.market, OPENED.minusSeconds(1)));
        assertEquals(0, new BigDecimal("100").compareTo(book.alice.supplyOf("USDC")));
        assertEquals(0, new BigDecimal("40").compareTo(book.bob.getLoan("USDC").orElseThrow().getOutstanding()));
        assertEquals(0, new BigDecimal("60").compareTo(book.market.getCash()));
        assertEquals(OPENED, book.market.getLastAccrual());
    }

    @Test
    void accrualGrowsBorrowerDebtAndSupplierBalances() {
        // Normal: over a positive duration, each borrower of that asset is multiplied by the borrow factor
        // and each supplier by the supply factor. A position that does not use the asset is untouched.
        Fixture book = funded();

        book.interest.accrue(book.market, ONE_YEAR_LATER);

        assertEquals(0, new BigDecimal("103.6").compareTo(book.alice.supplyOf("USDC")));
        assertEquals(0, new BigDecimal("44").compareTo(book.bob.getLoan("USDC").orElseThrow().getOutstanding()));
        assertEquals(0, new BigDecimal("7").compareTo(book.carol.supplyOf("ETH")));
    }

    @Test
    void reserveFactorWithholdsPartOfBorrowerInterestFromSuppliers() {
        // Normal: supplier growth is borrow interest times (1 - reserveFactor).
        // The residual is added to reserves, not paid to any wallet.
        Fixture book = funded();

        book.interest.accrue(book.market, ONE_YEAR_LATER);

        BigDecimal borrowerInterest = new BigDecimal("4");
        BigDecimal supplierGrowth = book.alice.supplyOf("USDC").subtract(new BigDecimal("100"));
        assertEquals(0, borrowerInterest.multiply(new BigDecimal("0.90")).compareTo(supplierGrowth));
        assertEquals(0, new BigDecimal("0.4").compareTo(book.market.getReserves()));
        assertEquals(0, BigDecimal.ZERO.compareTo(book.alice.getAccount().getWalletBalance("USDC")));
    }

    @Test
    void cashDoesNotChangeWhenInterestAccrues() {
        // Edge: interest creates claims. Cash, the tokens physically in the pool, stays put.
        Fixture book = funded();
        BigDecimal cash = book.market.getCash();

        book.interest.accrue(book.market, ONE_YEAR_LATER);

        assertEquals(0, cash.compareTo(book.market.getCash()));
        assertEquals(0, new BigDecimal("60").compareTo(book.market.getCash()));
    }

    @Test
    void poolInvariantHoldsAfterAccrual() {
        // Normal: after accrual, cash + totalBorrowed compares equal to totalSupplied + reserves.
        // Market totals equal the sum of position supplies and the sum of position debts for that asset.
        Fixture book = funded();

        book.interest.accrueAll(ONE_YEAR_LATER);

        BigDecimal left = book.market.getCash().add(book.market.getTotalBorrowed());
        BigDecimal right = book.market.getTotalSupplied().add(book.market.getReserves());
        assertEquals(0, left.compareTo(right));
        assertEquals(0, book.alice.supplyOf("USDC").compareTo(book.market.getTotalSupplied()));
        assertEquals(0, book.bob.getLoan("USDC").orElseThrow().getOutstanding().compareTo(book.market.getTotalBorrowed()));
    }

    private static Market usdcMarket(String baseRate, String reserveFactor) {
        Stablecoin usdc = new Stablecoin("USDC", "USD Coin", 6, new BigDecimal("0.005"));
        return new Market(usdc, new BigDecimal("0.80"), new BigDecimal("0.85"), new BigDecimal("0.05"),
                new BigDecimal(reserveFactor), new BigDecimal("0.50"),
                new LinearRateModel(new BigDecimal(baseRate), BigDecimal.ZERO), OPENED);
    }

    private static Loan loan(String symbol, String outstanding) {
        return Loan.builder()
                .assetSymbol(symbol)
                .openedAt(OPENED)
                .outstanding(new BigDecimal(outstanding))
                .build();
    }

    private static Fixture funded() {
        Market market = usdcMarket("0.10", "0.10");
        market.onDeposit(new BigDecimal("100"));
        market.onBorrow(new BigDecimal("40"));
        Account aliceAccount = new Account("alice", "Alice", AccountRole.USER);
        Account bobAccount = new Account("bob", "Bob", AccountRole.USER);
        Account carolAccount = new Account("carol", "Carol", AccountRole.USER);
        Position alice = Position.builder(aliceAccount).supply("USDC", new BigDecimal("100")).build();
        Position bob = Position.builder(bobAccount).loan(loan("USDC", "40")).build();
        Position carol = Position.builder(carolAccount).supply("ETH", new BigDecimal("7")).build();
        Portfolio portfolio = new Portfolio();
        portfolio.add(aliceAccount, alice);
        portfolio.add(bobAccount, bob);
        portfolio.add(carolAccount, carol);
        MarketBook book = new MarketBook();
        book.register(market);
        return new Fixture(market, alice, bob, carol, new InterestAccrualService(book, portfolio));
    }

    private record Fixture(Market market, Position alice, Position bob, Position carol, InterestAccrualService interest) {
    }
}
