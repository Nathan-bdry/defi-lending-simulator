package com.defi.lending.service;

import com.defi.lending.exception.PositionNotLiquidatableException;
import com.defi.lending.model.Account;
import com.defi.lending.model.AccountRole;
import com.defi.lending.model.Loan;
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

/**
 * Liquidation rules for {@link LiquidationEngine}, including the price-listener path.
 */
class LiquidationEngineTest {

    private static final LocalDateTime AT = LocalDateTime.parse("2026-03-01T00:00:00");

    @Test
    void priceThatKeepsHealthFactorAboveOneDoesNotLiquidate() {
        // Normal: a tick that leaves every holder of that symbol at a health factor above 1
        // changes the stored price and no balances.
        Desk desk = desk();
        Market dai = stable("DAI", "0.10", "0.80", "0.10", "0.50");
        Market usdc = stable("USDC", "0.10", "0.80", "0", "0.50");
        desk.add(dai, usdc);
        Account alice = user("alice", "Alice");
        desk.add(alice, Position.builder(alice).supply("DAI", new BigDecimal("100"))
                .loan(loan("USDC", "40")).build());
        dai.onDeposit(new BigDecimal("100"));
        usdc.onDeposit(new BigDecimal("40"));
        usdc.onBorrow(new BigDecimal("40"));
        desk.bob.credit("USDC", new BigDecimal("1000"));
        desk.oracle.publish(new PriceTick(AT, "USDC", BigDecimal.ONE));
        desk.oracle.publish(new PriceTick(AT, "DAI", BigDecimal.ONE));
        desk.oracle.subscribe(desk.engine);

        desk.oracle.publish(new PriceTick(AT, "DAI", new BigDecimal("1.05")));

        assertEquals(0, new BigDecimal("1.05").compareTo(desk.oracle.getPrice("DAI").orElseThrow()));
        assertEquals(0, new BigDecimal("40").compareTo(loanOf(desk, "alice", "USDC")));
        assertEquals(0, new BigDecimal("100").compareTo(desk.position("alice").supplyOf("DAI")));
        assertEquals(0, new BigDecimal("1000").compareTo(desk.bob.getWalletBalance("USDC")));
        assertEquals(0, BigDecimal.ZERO.compareTo(desk.bob.getWalletBalance("DAI")));
        assertEquals(0, BigDecimal.ZERO.compareTo(usdc.getCash()));
        assertEquals(0, new BigDecimal("100").compareTo(dai.getCash()));
    }

    @Test
    void healthFactorOfExactlyOneIsNotLiquidated() {
        // Edge: a tick that lands a position on a health factor of exactly 1 seizes nothing.
        // A direct liquidate call for that position throws PositionNotLiquidatableException.
        Desk desk = desk();
        Market dai = stable("DAI", "0.10", "0.80", "0.10", "0.50");
        Market usdc = stable("USDC", "0.10", "0.80", "0", "0.50");
        desk.add(dai, usdc);
        Account alice = user("alice", "Alice");
        desk.add(alice, Position.builder(alice).supply("DAI", new BigDecimal("100"))
                .loan(loan("USDC", "80")).build());
        dai.onDeposit(new BigDecimal("100"));
        usdc.onDeposit(new BigDecimal("80"));
        usdc.onBorrow(new BigDecimal("80"));
        desk.bob.credit("USDC", new BigDecimal("1000"));
        desk.oracle.publish(new PriceTick(AT, "USDC", BigDecimal.ONE));
        desk.oracle.publish(new PriceTick(AT, "DAI", BigDecimal.ONE));
        desk.oracle.subscribe(desk.engine);

        desk.oracle.publish(new PriceTick(AT, "DAI", BigDecimal.ONE));

        assertEquals(0, new BigDecimal("80").compareTo(loanOf(desk, "alice", "USDC")));
        assertEquals(0, new BigDecimal("100").compareTo(desk.position("alice").supplyOf("DAI")));
        assertEquals(0, new BigDecimal("1000").compareTo(desk.bob.getWalletBalance("USDC")));
        assertThrows(PositionNotLiquidatableException.class,
                () -> desk.engine.liquidate("bob", "alice", "USDC", "DAI", new BigDecimal("40"), AT));
        assertEquals(0, new BigDecimal("80").compareTo(loanOf(desk, "alice", "USDC")));
        assertEquals(0, new BigDecimal("1000").compareTo(desk.bob.getWalletBalance("USDC")));
        assertEquals(0, BigDecimal.ZERO.compareTo(usdc.getCash()));
    }

    @Test
    void priceDropBelowOneRepaysDebtAndSeizesCollateralWithTheBonus() {
        // Normal: health factor strictly below 1. The liquidator's wallet pays the debt asset,
        // the borrower's debt shrinks, and the liquidator receives collateral worth
        // repay * debtUnitValue * (1 + liquidationBonus). The bonus is the collateral market's bonus.
        // Pool cash of the debt asset rises; pool cash of the collateral falls.
        Desk desk = desk();
        Market dai = stable("DAI", "0", "0.80", "0.10", "0.20");
        Market usdc = stable("USDC", "0.10", "0.80", "0", "0.50");
        desk.add(dai, usdc);
        Account alice = user("alice", "Alice");
        desk.add(alice, Position.builder(alice).supply("DAI", new BigDecimal("100"))
                .loan(loan("USDC", "50")).build());
        dai.onDeposit(new BigDecimal("100"));
        usdc.onDeposit(new BigDecimal("50"));
        usdc.onBorrow(new BigDecimal("50"));
        desk.bob.credit("USDC", new BigDecimal("1000"));
        desk.oracle.publish(new PriceTick(AT, "USDC", BigDecimal.ONE));
        desk.oracle.publish(new PriceTick(AT, "DAI", BigDecimal.ONE));
        desk.oracle.subscribe(desk.engine);

        desk.oracle.publish(new PriceTick(AT, "DAI", new BigDecimal("0.50")));

        assertEquals(0, new BigDecimal("25").compareTo(loanOf(desk, "alice", "USDC")));
        assertEquals(0, new BigDecimal("45").compareTo(desk.position("alice").supplyOf("DAI")));
        assertEquals(0, new BigDecimal("975").compareTo(desk.bob.getWalletBalance("USDC")));
        assertEquals(0, new BigDecimal("55").compareTo(desk.bob.getWalletBalance("DAI")));
        assertEquals(0, BigDecimal.ZERO.compareTo(desk.position("bob").supplyOf("DAI")));
        assertEquals(0, new BigDecimal("25").compareTo(usdc.getCash()));
        assertEquals(0, new BigDecimal("25").compareTo(usdc.getTotalBorrowed()));
        assertEquals(0, new BigDecimal("45").compareTo(dai.getCash()));
        assertEquals(0, new BigDecimal("45").compareTo(dai.getTotalSupplied()));
    }

    @Test
    void repayOfExactlyTheCloseFactorSucceeds() {
        // Edge: repayAmount equal to outstanding * closeFactor is accepted. The close factor is the debt market's.
        Desk desk = desk();
        Market dai = stable("DAI", "0.10", "0.80", "0.10", "0.25");
        Market usdc = stable("USDC", "0.10", "0.80", "0", "0.50");
        desk.add(dai, usdc);
        Account alice = user("alice", "Alice");
        desk.add(alice, Position.builder(alice).supply("DAI", new BigDecimal("200"))
                .loan(loan("USDC", "200")).build());
        dai.onDeposit(new BigDecimal("200"));
        usdc.onDeposit(new BigDecimal("200"));
        usdc.onBorrow(new BigDecimal("200"));
        desk.bob.credit("USDC", new BigDecimal("1000"));
        desk.oracle.publish(new PriceTick(AT, "DAI", BigDecimal.ONE));
        desk.oracle.publish(new PriceTick(AT, "USDC", BigDecimal.ONE));

        desk.engine.liquidate("bob", "alice", "USDC", "DAI", new BigDecimal("100"), AT);

        assertEquals(0, new BigDecimal("100").compareTo(loanOf(desk, "alice", "USDC")));
        assertEquals(0, new BigDecimal("90").compareTo(desk.position("alice").supplyOf("DAI")));
        assertEquals(0, new BigDecimal("900").compareTo(desk.bob.getWalletBalance("USDC")));
        assertEquals(0, new BigDecimal("110").compareTo(desk.bob.getWalletBalance("DAI")));
    }

    @Test
    void repayAboveTheCloseFactorIsRejected() {
        // Invalid: repayAmount strictly above outstanding * closeFactor throws IllegalArgumentException
        // and moves nothing.
        Desk desk = desk();
        Market dai = stable("DAI", "0.10", "0.80", "0.10", "1");
        Market usdc = stable("USDC", "0.10", "0.80", "0", "0.50");
        desk.add(dai, usdc);
        Account alice = user("alice", "Alice");
        desk.add(alice, Position.builder(alice).supply("DAI", new BigDecimal("200"))
                .loan(loan("USDC", "200")).build());
        dai.onDeposit(new BigDecimal("200"));
        usdc.onDeposit(new BigDecimal("200"));
        usdc.onBorrow(new BigDecimal("200"));
        desk.bob.credit("USDC", new BigDecimal("1000"));
        desk.oracle.publish(new PriceTick(AT, "DAI", BigDecimal.ONE));
        desk.oracle.publish(new PriceTick(AT, "USDC", BigDecimal.ONE));

        assertThrows(IllegalArgumentException.class,
                () -> desk.engine.liquidate("bob", "alice", "USDC", "DAI", new BigDecimal("101"), AT));
        assertEquals(0, new BigDecimal("200").compareTo(loanOf(desk, "alice", "USDC")));
        assertEquals(0, new BigDecimal("200").compareTo(desk.position("alice").supplyOf("DAI")));
        assertEquals(0, new BigDecimal("1000").compareTo(desk.bob.getWalletBalance("USDC")));
        assertEquals(0, BigDecimal.ZERO.compareTo(desk.bob.getWalletBalance("DAI")));
        assertEquals(0, BigDecimal.ZERO.compareTo(usdc.getCash()));
        assertEquals(0, new BigDecimal("200").compareTo(dai.getCash()));
    }

    @Test
    void seizedCollateralIsCappedAtWhatTheBorrowerHas() {
        // Edge: when the bonus would seize more tokens than the borrower supplies, the repay is scaled down
        // and the whole collateral line is taken. The liquidator does not receive tokens the pool does not hold.
        Desk desk = desk();
        Market dai = stable("DAI", "0.10", "0.80", "0.10", "0.50");
        Market usdc = stable("USDC", "0.10", "0.80", "0", "0.50");
        desk.add(dai, usdc);
        Account alice = user("alice", "Alice");
        desk.add(alice, Position.builder(alice).supply("DAI", new BigDecimal("10"))
                .loan(loan("USDC", "100")).build());
        dai.onDeposit(new BigDecimal("10"));
        usdc.onDeposit(new BigDecimal("100"));
        usdc.onBorrow(new BigDecimal("100"));
        desk.bob.credit("USDC", new BigDecimal("1000"));
        desk.oracle.publish(new PriceTick(AT, "DAI", BigDecimal.ONE));
        desk.oracle.publish(new PriceTick(AT, "USDC", BigDecimal.ONE));

        desk.engine.liquidate("bob", "alice", "USDC", "DAI", new BigDecimal("50"), AT);

        BigDecimal repay = new BigDecimal("9.090909090909090909");
        assertEquals(0, repay.compareTo(new BigDecimal("1000").subtract(desk.bob.getWalletBalance("USDC"))));
        assertEquals(0, new BigDecimal("100").subtract(repay).compareTo(loanOf(desk, "alice", "USDC")));
        assertEquals(0, BigDecimal.ZERO.compareTo(desk.position("alice").supplyOf("DAI")));
        assertEquals(0, new BigDecimal("10").compareTo(desk.bob.getWalletBalance("DAI")));
        assertEquals(0, BigDecimal.ZERO.compareTo(dai.getCash()));
        assertEquals(0, BigDecimal.ZERO.compareTo(dai.getTotalSupplied()));
    }

    @Test
    void liquidatorWithAnEmptyWalletDoesNotSeizeAnything() {
        // Edge: on a price tick, a liquidator who cannot pay the debt asset is skipped.
        // The underwater position stays underwater for the risk report. Other positions in the same tick still run.
        Desk desk = desk();
        Market dai = stable("DAI", "0.10", "0.80", "0.10", "0.50");
        Market usdc = stable("USDC", "0.10", "0.80", "0", "0.50");
        Market usdt = stable("USDT", "0.10", "0.80", "0", "0.50");
        desk.add(dai, usdc, usdt);
        Account alice = user("alice", "Alice");
        Account carol = user("carol", "Carol");
        desk.add(alice, Position.builder(alice).supply("DAI", new BigDecimal("100"))
                .loan(loan("USDC", "100")).build());
        desk.add(carol, Position.builder(carol).supply("DAI", new BigDecimal("100"))
                .loan(loan("USDT", "100")).build());
        dai.onDeposit(new BigDecimal("200"));
        usdc.onDeposit(new BigDecimal("100"));
        usdc.onBorrow(new BigDecimal("100"));
        usdt.onDeposit(new BigDecimal("100"));
        usdt.onBorrow(new BigDecimal("100"));
        desk.bob.credit("USDT", new BigDecimal("1000"));
        desk.oracle.publish(new PriceTick(AT, "DAI", BigDecimal.ONE));
        desk.oracle.publish(new PriceTick(AT, "USDC", BigDecimal.ONE));
        desk.oracle.publish(new PriceTick(AT, "USDT", BigDecimal.ONE));
        desk.oracle.subscribe(desk.engine);

        desk.oracle.publish(new PriceTick(AT, "DAI", BigDecimal.ONE));

        assertEquals(0, new BigDecimal("100").compareTo(loanOf(desk, "alice", "USDC")));
        assertEquals(0, new BigDecimal("100").compareTo(desk.position("alice").supplyOf("DAI")));
        assertEquals(0, BigDecimal.ZERO.compareTo(desk.bob.getWalletBalance("USDC")));
        assertEquals(0, new BigDecimal("50").compareTo(loanOf(desk, "carol", "USDT")));
        assertEquals(0, new BigDecimal("45").compareTo(desk.position("carol").supplyOf("DAI")));
        assertEquals(0, new BigDecimal("950").compareTo(desk.bob.getWalletBalance("USDT")));
        assertEquals(0, new BigDecimal("55").compareTo(desk.bob.getWalletBalance("DAI")));
    }

    @Test
    void oneTickLiquidatesAPositionOnlyOnce() {
        // Edge: a single tick repays at most one close-factor chunk. It does not loop until the health factor
        // is back to 1. A second tick may liquidate the remainder.
        Desk desk = desk();
        Market dai = stable("DAI", "0.10", "0.50", "0", "0.50");
        Market usdc = stable("USDC", "0.10", "0.80", "0", "0.50");
        desk.add(dai, usdc);
        Account alice = user("alice", "Alice");
        desk.add(alice, Position.builder(alice).supply("DAI", new BigDecimal("100"))
                .loan(loan("USDC", "100")).build());
        dai.onDeposit(new BigDecimal("100"));
        usdc.onDeposit(new BigDecimal("100"));
        usdc.onBorrow(new BigDecimal("100"));
        desk.bob.credit("USDC", new BigDecimal("1000"));
        desk.oracle.publish(new PriceTick(AT, "USDC", BigDecimal.ONE));
        desk.oracle.publish(new PriceTick(AT, "DAI", BigDecimal.ONE));
        desk.oracle.subscribe(desk.engine);

        desk.oracle.publish(new PriceTick(AT, "DAI", BigDecimal.ONE));

        assertEquals(0, new BigDecimal("50").compareTo(loanOf(desk, "alice", "USDC")));
        assertEquals(0, new BigDecimal("50").compareTo(desk.position("alice").supplyOf("DAI")));
        assertEquals(0, new BigDecimal("950").compareTo(desk.bob.getWalletBalance("USDC")));

        desk.oracle.publish(new PriceTick(AT, "DAI", BigDecimal.ONE));

        assertEquals(0, new BigDecimal("25").compareTo(loanOf(desk, "alice", "USDC")));
        assertEquals(0, new BigDecimal("25").compareTo(desk.position("alice").supplyOf("DAI")));
        assertEquals(0, new BigDecimal("925").compareTo(desk.bob.getWalletBalance("USDC")));
    }

    @Test
    void directLiquidationOfAHealthyPositionIsRefused() {
        // Exception: liquidate on a position with no debt, or with a health factor above 1,
        // throws PositionNotLiquidatableException.
        Desk solvent = desk();
        Market dai = stable("DAI", "0.10", "0.80", "0.10", "0.50");
        Market usdc = stable("USDC", "0.10", "0.80", "0", "0.50");
        solvent.add(dai, usdc);
        Account alice = user("alice", "Alice");
        solvent.add(alice, Position.builder(alice).supply("DAI", new BigDecimal("100")).build());
        dai.onDeposit(new BigDecimal("100"));
        solvent.bob.credit("USDC", new BigDecimal("1000"));
        solvent.oracle.publish(new PriceTick(AT, "DAI", BigDecimal.ONE));
        solvent.oracle.publish(new PriceTick(AT, "USDC", BigDecimal.ONE));

        assertThrows(PositionNotLiquidatableException.class,
                () -> solvent.engine.liquidate("bob", "alice", "USDC", "DAI", new BigDecimal("10"), AT));
        assertEquals(0, new BigDecimal("100").compareTo(solvent.position("alice").supplyOf("DAI")));
        assertEquals(0, new BigDecimal("1000").compareTo(solvent.bob.getWalletBalance("USDC")));

        Desk healthy = desk();
        Market healthyDai = stable("DAI", "0.10", "0.80", "0.10", "0.50");
        Market healthyUsdc = stable("USDC", "0.10", "0.80", "0", "0.50");
        healthy.add(healthyDai, healthyUsdc);
        Account dave = user("dave", "Dave");
        healthy.add(dave, Position.builder(dave).supply("DAI", new BigDecimal("100"))
                .loan(loan("USDC", "40")).build());
        healthyDai.onDeposit(new BigDecimal("100"));
        healthyUsdc.onDeposit(new BigDecimal("40"));
        healthyUsdc.onBorrow(new BigDecimal("40"));
        healthy.bob.credit("USDC", new BigDecimal("1000"));
        healthy.oracle.publish(new PriceTick(AT, "DAI", BigDecimal.ONE));
        healthy.oracle.publish(new PriceTick(AT, "USDC", BigDecimal.ONE));

        assertThrows(PositionNotLiquidatableException.class,
                () -> healthy.engine.liquidate("bob", "dave", "USDC", "DAI", new BigDecimal("10"), AT));
        assertEquals(0, new BigDecimal("40").compareTo(loanOf(healthy, "dave", "USDC")));
        assertEquals(0, new BigDecimal("100").compareTo(healthy.position("dave").supplyOf("DAI")));
        assertEquals(0, new BigDecimal("1000").compareTo(healthy.bob.getWalletBalance("USDC")));
    }

    private static BigDecimal loanOf(Desk desk, String accountId, String symbol) {
        return desk.position(accountId).getLoan(symbol).orElseThrow().getOutstanding();
    }

    private static Loan loan(String symbol, String amount) {
        return Loan.builder().assetSymbol(symbol).openedAt(AT).outstanding(new BigDecimal(amount)).build();
    }

    private static Account user(String id, String name) {
        return new Account(id, name, AccountRole.USER);
    }

    private static Market stable(String symbol, String peg, String threshold, String bonus, String close) {
        return new Market(new Stablecoin(symbol, symbol, 18, new BigDecimal(peg)),
                new BigDecimal(threshold), new BigDecimal(threshold),
                new BigDecimal(bonus), new BigDecimal("0.10"), new BigDecimal(close),
                new LinearRateModel(BigDecimal.ZERO, BigDecimal.ZERO), AT);
    }

    private static Desk desk() {
        MarketBook markets = new MarketBook();
        Portfolio portfolio = new Portfolio();
        PriceOracle oracle = new PriceOracle();
        Account bob = new Account("bob", "Bob", AccountRole.LIQUIDATOR);
        portfolio.add(bob, Position.open(bob));
        HealthFactorService health = new HealthFactorService(markets, oracle);
        LiquidationEngine engine = new LiquidationEngine(markets, portfolio, health,
                new InterestAccrualService(markets, portfolio));
        return new Desk(markets, portfolio, oracle, bob, engine);
    }

    private record Desk(MarketBook markets, Portfolio portfolio, PriceOracle oracle, Account bob,
                        LiquidationEngine engine) {

        private void add(Market... listed) {
            for (Market market : listed) {
                markets.register(market);
            }
        }

        private void add(Account account, Position position) {
            portfolio.add(account, position);
        }

        private Position position(String accountId) {
            return portfolio.findPosition(accountId).orElseThrow();
        }
    }
}
