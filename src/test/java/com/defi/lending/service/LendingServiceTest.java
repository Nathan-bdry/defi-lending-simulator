package com.defi.lending.service;

import com.defi.lending.Precision;
import com.defi.lending.exception.InsufficientCollateralException;
import com.defi.lending.exception.InsufficientLiquidityException;
import com.defi.lending.model.Account;
import com.defi.lending.model.AccountRole;
import com.defi.lending.model.Market;
import com.defi.lending.model.MarketBook;
import com.defi.lending.model.Portfolio;
import com.defi.lending.model.Position;
import com.defi.lending.model.Stablecoin;
import com.defi.lending.oracle.PriceOracle;
import com.defi.lending.oracle.PriceTick;
import com.defi.lending.rates.LinearRateModel;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * User actions for {@link LendingService}: wallet versus pool, the health-factor guard, and liquidity.
 */
class LendingServiceTest {

    private static final LocalDateTime AT = LocalDateTime.parse("2026-03-01T00:00:00");
    private static final LocalDateTime ONE_YEAR_LATER = AT.plusSeconds(Precision.SECONDS_PER_YEAR.longValue());

    @Test
    void depositMovesTokensFromTheWalletIntoThePool() {
        // Normal: wallet decreases, position supply increases, market cash and totalSupplied increase by the same amount.
        Desk desk = desk("0.75", "0.80", "0");
        desk.alice.credit("USDC", new BigDecimal("100"));

        desk.lending.deposit("alice", "USDC", new BigDecimal("40"), AT);

        assertEquals(0, new BigDecimal("60").compareTo(desk.alice.getWalletBalance("USDC")));
        assertEquals(0, new BigDecimal("40").compareTo(desk.alicePosition().supplyOf("USDC")));
        assertEquals(0, new BigDecimal("40").compareTo(desk.usdc.getCash()));
        assertEquals(0, new BigDecimal("40").compareTo(desk.usdc.getTotalSupplied()));
    }

    @Test
    void depositRejectsANonPositiveAmount() {
        // Invalid: zero and negative amounts throw IllegalArgumentException and change nothing.
        Desk desk = desk("0.75", "0.80", "0");
        desk.alice.credit("USDC", new BigDecimal("100"));

        assertThrows(IllegalArgumentException.class,
                () -> desk.lending.deposit("alice", "USDC", BigDecimal.ZERO, AT));
        assertThrows(IllegalArgumentException.class,
                () -> desk.lending.deposit("alice", "USDC", new BigDecimal("-1"), AT));
        assertEquals(0, new BigDecimal("100").compareTo(desk.alice.getWalletBalance("USDC")));
        assertEquals(0, BigDecimal.ZERO.compareTo(desk.alicePosition().supplyOf("USDC")));
        assertEquals(0, BigDecimal.ZERO.compareTo(desk.usdc.getCash()));
    }

    @Test
    void depositRejectsAnUnknownAccountOrMarket() {
        // Invalid: an id or symbol that was not loaded throws IllegalArgumentException.
        Desk desk = desk("0.75", "0.80", "0");
        desk.alice.credit("USDC", new BigDecimal("100"));

        assertThrows(IllegalArgumentException.class,
                () -> desk.lending.deposit("nobody", "USDC", new BigDecimal("10"), AT));
        assertThrows(IllegalArgumentException.class,
                () -> desk.lending.deposit("alice", "DOGE", new BigDecimal("10"), AT));
        assertEquals(0, new BigDecimal("100").compareTo(desk.alice.getWalletBalance("USDC")));
    }

    @Test
    void depositRejectsMoreDecimalPlacesThanTheAssetAllows() {
        // Invalid: an amount whose scale is greater than the asset's decimals is rejected.
        // Amounts are whole tokens, so this is about fractional tokens, not wei.
        Desk desk = desk("0.75", "0.80", "0");
        desk.alice.credit("USDC", new BigDecimal("10"));

        assertThrows(IllegalArgumentException.class,
                () -> desk.lending.deposit("alice", "USDC", new BigDecimal("1.1234567"), AT));
        assertEquals(0, new BigDecimal("10").compareTo(desk.alice.getWalletBalance("USDC")));
        assertEquals(0, BigDecimal.ZERO.compareTo(desk.alicePosition().supplyOf("USDC")));
    }

    @Test
    void withdrawWithNoDebtReturnsTheSupply() {
        // Normal: a supplier who has not borrowed can withdraw the full supply. Wallet and cash move together.
        Desk desk = desk("0.75", "0.80", "0");
        desk.alice.credit("USDC", new BigDecimal("100"));
        desk.lending.deposit("alice", "USDC", new BigDecimal("40"), AT);

        desk.lending.withdraw("alice", "USDC", new BigDecimal("40"), AT);

        assertEquals(0, new BigDecimal("100").compareTo(desk.alice.getWalletBalance("USDC")));
        assertEquals(0, BigDecimal.ZERO.compareTo(desk.alicePosition().supplyOf("USDC")));
        assertEquals(0, BigDecimal.ZERO.compareTo(desk.usdc.getCash()));
    }

    @Test
    void withdrawThatLeavesHealthFactorExactlyOneSucceeds() {
        // Edge: a withdraw whose draft health factor compares equal to 1 is allowed.
        Desk desk = desk("0.75", "0.80", "0");
        fundPool(desk);
        desk.lending.borrow("alice", "USDC", new BigDecimal("50"), AT);

        desk.lending.withdraw("alice", "DAI", new BigDecimal("37.5"), AT);

        assertEquals(0, new BigDecimal("62.5").compareTo(desk.alicePosition().supplyOf("DAI")));
        assertEquals(0, new BigDecimal("37.5").compareTo(desk.alice.getWalletBalance("DAI")));
        assertEquals(0, BigDecimal.ONE.compareTo(desk.health.healthFactor(desk.alicePosition()).orElseThrow()));
    }

    @Test
    void withdrawThatWouldDropHealthFactorBelowOneIsRefused() {
        // Exception: a withdraw whose draft health factor is strictly below 1 throws
        // InsufficientCollateralException and leaves balances unchanged.
        Desk desk = desk("0.75", "0.80", "0");
        fundPool(desk);
        desk.lending.borrow("alice", "USDC", new BigDecimal("50"), AT);

        assertThrows(InsufficientCollateralException.class,
                () -> desk.lending.withdraw("alice", "DAI", new BigDecimal("40"), AT));
        assertEquals(0, new BigDecimal("100").compareTo(desk.alicePosition().supplyOf("DAI")));
        assertEquals(0, BigDecimal.ZERO.compareTo(desk.alice.getWalletBalance("DAI")));
        assertEquals(0, new BigDecimal("100").compareTo(desk.dai.getCash()));
    }

    @Test
    void withdrawAboveTheUsersSupplyIsRejected() {
        // Invalid: more than the position supplies throws IllegalArgumentException, even when the pool has the cash.
        Desk desk = desk("0.75", "0.80", "0");
        desk.alice.credit("USDC", new BigDecimal("40"));
        desk.bob.credit("USDC", new BigDecimal("100"));
        desk.lending.deposit("alice", "USDC", new BigDecimal("40"), AT);
        desk.lending.deposit("bob", "USDC", new BigDecimal("100"), AT);

        assertThrows(IllegalArgumentException.class,
                () -> desk.lending.withdraw("alice", "USDC", new BigDecimal("41"), AT));
        assertEquals(0, new BigDecimal("40").compareTo(desk.alicePosition().supplyOf("USDC")));
        assertEquals(0, new BigDecimal("140").compareTo(desk.usdc.getCash()));
    }

    @Test
    void withdrawAbovePoolCashThrowsInsufficientLiquidity() {
        // Edge: the user supplied the tokens, but they have been borrowed by someone else.
        // InsufficientLiquidityException, and the position is unchanged.
        Desk desk = desk("0.80", "0.80", "0");
        desk.bob.credit("DAI", new BigDecimal("1000"));
        desk.lending.deposit("bob", "DAI", new BigDecimal("1000"), AT);
        desk.alice.credit("USDC", new BigDecimal("100"));
        desk.lending.deposit("alice", "USDC", new BigDecimal("100"), AT);
        desk.lending.borrow("bob", "USDC", new BigDecimal("80"), AT);

        assertThrows(InsufficientLiquidityException.class,
                () -> desk.lending.withdraw("alice", "USDC", new BigDecimal("30"), AT));
        assertEquals(0, new BigDecimal("100").compareTo(desk.alicePosition().supplyOf("USDC")));
        assertEquals(0, BigDecimal.ZERO.compareTo(desk.alice.getWalletBalance("USDC")));
        assertEquals(0, new BigDecimal("20").compareTo(desk.usdc.getCash()));
    }

    @Test
    void borrowWithinCollateralAndLiquidityCreditsTheWallet() {
        // Normal: debt and totalBorrowed increase, cash decreases, and the wallet receives the tokens.
        Desk desk = desk("0.75", "0.80", "0");
        fundPool(desk);

        desk.lending.borrow("alice", "USDC", new BigDecimal("50"), AT);

        assertEquals(0, new BigDecimal("50").compareTo(desk.alice.getWalletBalance("USDC")));
        assertEquals(0, new BigDecimal("50").compareTo(desk.alicePosition().getLoan("USDC").orElseThrow().getOutstanding()));
        assertEquals(0, new BigDecimal("50").compareTo(desk.usdc.getCash()));
        assertEquals(0, new BigDecimal("50").compareTo(desk.usdc.getTotalBorrowed()));
    }

    @Test
    void borrowThatLeavesHealthFactorExactlyOneSucceeds() {
        // Edge: a draft health factor of exactly 1 is allowed when the collateral-factor cap is also met.
        Desk desk = desk("0.80", "0.80", "0");
        fundPool(desk);

        desk.lending.borrow("alice", "USDC", new BigDecimal("80"), AT);

        assertEquals(0, new BigDecimal("80").compareTo(desk.alicePosition().getLoan("USDC").orElseThrow().getOutstanding()));
        assertEquals(0, BigDecimal.ONE.compareTo(desk.health.healthFactor(desk.alicePosition()).orElseThrow()));
    }

    @Test
    void borrowPastTheCollateralFactorIsRefusedEvenIfHealthFactorStaysAboveOne() {
        // Exception: debt value above borrowing power throws InsufficientCollateralException.
        // This is the loan-to-value cap. The health factor can still be above 1 because the
        // liquidation threshold is looser than the collateral factor, and the borrow is still refused.
        Desk desk = desk("0.75", "0.80", "0");
        fundPool(desk);

        assertThrows(InsufficientCollateralException.class,
                () -> desk.lending.borrow("alice", "USDC", new BigDecimal("76"), AT));
        assertTrue(desk.alicePosition().getLoan("USDC").isEmpty());
        assertEquals(0, new BigDecimal("100").compareTo(desk.usdc.getCash()));
        assertEquals(0, BigDecimal.ZERO.compareTo(desk.alice.getWalletBalance("USDC")));
    }

    @Test
    void borrowThatWouldDropHealthFactorBelowOneIsRefused() {
        // Exception: draft health factor strictly below 1 throws InsufficientCollateralException.
        Desk desk = desk("0.75", "0.80", "0");
        fundPool(desk);

        assertThrows(InsufficientCollateralException.class,
                () -> desk.lending.borrow("alice", "USDC", new BigDecimal("81"), AT));
        assertTrue(desk.alicePosition().getLoan("USDC").isEmpty());
        assertEquals(0, new BigDecimal("100").compareTo(desk.usdc.getCash()));
    }

    @Test
    void borrowLargerThanPoolCashThrowsInsufficientLiquidity() {
        // Exception: amount above market cash throws InsufficientLiquidityException before any balance changes.
        Desk desk = desk("0.75", "0.80", "0");
        desk.alice.credit("DAI", new BigDecimal("100"));
        desk.bob.credit("USDC", new BigDecimal("10"));
        desk.lending.deposit("alice", "DAI", new BigDecimal("100"), AT);
        desk.lending.deposit("bob", "USDC", new BigDecimal("10"), AT);

        assertThrows(InsufficientLiquidityException.class,
                () -> desk.lending.borrow("alice", "USDC", new BigDecimal("11"), AT));
        assertTrue(desk.alicePosition().getLoan("USDC").isEmpty());
        assertEquals(0, new BigDecimal("10").compareTo(desk.usdc.getCash()));
        assertEquals(0, BigDecimal.ZERO.compareTo(desk.alice.getWalletBalance("USDC")));
    }

    @Test
    void repayReducesDebtAndPullsTokensFromTheWallet() {
        // Normal: outstanding debt and totalBorrowed decrease, cash increases, wallet decreases.
        Desk desk = desk("0.75", "0.80", "0");
        fundPool(desk);
        desk.lending.borrow("alice", "USDC", new BigDecimal("50"), AT);

        desk.lending.repay("alice", "USDC", new BigDecimal("20"), AT);

        assertEquals(0, new BigDecimal("30").compareTo(desk.alice.getWalletBalance("USDC")));
        assertEquals(0, new BigDecimal("30").compareTo(desk.alicePosition().getLoan("USDC").orElseThrow().getOutstanding()));
        assertEquals(0, new BigDecimal("70").compareTo(desk.usdc.getCash()));
        assertEquals(0, new BigDecimal("30").compareTo(desk.usdc.getTotalBorrowed()));
    }

    @Test
    void repayAboveOutstandingIsCappedAtTheDebt() {
        // Edge: paying more than the debt repays the debt and only that much leaves the wallet.
        // The surplus is not deposited.
        Desk desk = desk("0.75", "0.80", "0");
        fundPool(desk);
        desk.lending.borrow("alice", "USDC", new BigDecimal("50"), AT);
        desk.alice.credit("USDC", new BigDecimal("30"));

        desk.lending.repay("alice", "USDC", new BigDecimal("70"), AT);

        assertEquals(0, new BigDecimal("30").compareTo(desk.alice.getWalletBalance("USDC")));
        assertTrue(desk.alicePosition().getLoan("USDC").isEmpty());
        assertEquals(0, BigDecimal.ZERO.compareTo(desk.alicePosition().supplyOf("USDC")));
        assertEquals(0, new BigDecimal("100").compareTo(desk.bobPosition().supplyOf("USDC")));
        assertEquals(0, new BigDecimal("100").compareTo(desk.usdc.getCash()));
        assertEquals(0, BigDecimal.ZERO.compareTo(desk.usdc.getTotalBorrowed()));
    }

    @Test
    void actionAccruesInterestUpToItsTimestampBeforeTheHealthCheck() {
        // Normal: a later borrow sees debt that has grown since the previous action.
        // The health check uses the accrued outstanding, not the original principal.
        Desk desk = desk("0.80", "0.80", "0.10");
        fundPool(desk);
        desk.lending.borrow("alice", "USDC", new BigDecimal("70"), AT);

        assertThrows(InsufficientCollateralException.class,
                () -> desk.lending.borrow("alice", "USDC", new BigDecimal("4"), ONE_YEAR_LATER));
        assertEquals(0, new BigDecimal("77").compareTo(desk.alicePosition().getLoan("USDC").orElseThrow().getOutstanding()));
        assertEquals(0, new BigDecimal("70").compareTo(desk.alice.getWalletBalance("USDC")));
        assertEquals(0, new BigDecimal("30").compareTo(desk.usdc.getCash()));
    }

    private static void fundPool(Desk desk) {
        desk.alice.credit("DAI", new BigDecimal("100"));
        desk.bob.credit("USDC", new BigDecimal("100"));
        desk.lending.deposit("bob", "USDC", new BigDecimal("100"), AT);
        desk.lending.deposit("alice", "DAI", new BigDecimal("100"), AT);
    }

    private static Desk desk(String collateralFactor, String threshold, String baseRate) {
        Stablecoin dai = new Stablecoin("DAI", "Dai", 18, new BigDecimal("0.01"));
        Stablecoin usdc = new Stablecoin("USDC", "USD Coin", 6, new BigDecimal("0.005"));
        Market daiMarket = market(dai, collateralFactor, threshold, baseRate);
        Market usdcMarket = market(usdc, "0.80", "0.85", baseRate);
        MarketBook book = new MarketBook();
        book.register(daiMarket);
        book.register(usdcMarket);
        Account alice = new Account("alice", "Alice", AccountRole.USER);
        Account bob = new Account("bob", "Bob", AccountRole.USER);
        Portfolio portfolio = new Portfolio();
        portfolio.add(alice, Position.open(alice));
        portfolio.add(bob, Position.open(bob));
        PriceOracle oracle = new PriceOracle();
        oracle.publish(new PriceTick(AT, "DAI", BigDecimal.ONE));
        oracle.publish(new PriceTick(AT, "USDC", BigDecimal.ONE));
        HealthFactorService health = new HealthFactorService(book, oracle);
        InterestAccrualService interest = new InterestAccrualService(book, portfolio);
        return new Desk(daiMarket, usdcMarket, alice, bob, portfolio, health,
                new LendingService(book, portfolio, health, interest));
    }

    private static Market market(Stablecoin asset, String collateralFactor, String threshold, String baseRate) {
        return new Market(asset, new BigDecimal(collateralFactor), new BigDecimal(threshold),
                new BigDecimal("0.05"), new BigDecimal("0.10"), new BigDecimal("0.50"),
                new LinearRateModel(new BigDecimal(baseRate), BigDecimal.ZERO), AT);
    }

    private record Desk(Market dai, Market usdc, Account alice, Account bob, Portfolio portfolio,
                        HealthFactorService health, LendingService lending) {

        private Position alicePosition() {
            return portfolio.findPosition("alice").orElseThrow();
        }

        private Position bobPosition() {
            return portfolio.findPosition("bob").orElseThrow();
        }
    }
}
