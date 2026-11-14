package com.defi.lending.service;

import com.defi.lending.model.Account;
import com.defi.lending.model.Asset;
import com.defi.lending.model.AccountRole;
import com.defi.lending.model.HealthBand;
import com.defi.lending.model.Loan;
import com.defi.lending.model.Market;
import com.defi.lending.model.MarketBook;
import com.defi.lending.model.Position;
import com.defi.lending.model.Stablecoin;
import com.defi.lending.model.VolatileToken;
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
 * Valuation rules for {@link HealthFactorService}.
 * Enable a method only after the rule it names is implemented.
 */
class HealthFactorServiceTest {

    private static final LocalDateTime OPENED = LocalDateTime.parse("2026-03-01T00:00:00");

    @Test
    void noDebtYieldsEmptyHealthFactor() {
        // Normal: a supplier who has not borrowed has an empty health factor, band NO_DEBT,
        // and zero debt value. Collateral value is still reported.
        VolatileToken eth = new VolatileToken("ETH", "Ether", 18, new BigDecimal("0.02"));
        PriceOracle oracle = new PriceOracle();
        publish(oracle, "ETH", "3200");
        HealthFactorService health = service(oracle, market(eth, "0.80", "0.83"));
        Position position = Position.builder(account()).supply("ETH", new BigDecimal("10")).build();

        assertTrue(health.healthFactor(position).isEmpty());
        assertEquals(HealthBand.NO_DEBT, health.band(position));
        assertEquals(0, BigDecimal.ZERO.compareTo(health.debtValue(position)));
        assertEquals(0, new BigDecimal("31360").compareTo(health.collateralValue(position)));
    }

    @Test
    void singleCollateralMatchesTheThresholdFormula() {
        // Normal: one supplied asset and one debt.
        // health factor = (supply * unitValue(price) * liquidationThreshold) / (debt * unitValue(debtPrice)).
        VolatileToken eth = new VolatileToken("ETH", "Ether", 18, new BigDecimal("0.02"));
        Stablecoin usdc = new Stablecoin("USDC", "USD Coin", 6, new BigDecimal("0.005"));
        PriceOracle oracle = new PriceOracle();
        publish(oracle, "ETH", "3200");
        publish(oracle, "USDC", "1");
        HealthFactorService health = service(oracle, market(eth, "0.80", "0.83"), market(usdc, "0.75", "0.80"));
        Position position = Position.builder(account())
                .supply("ETH", new BigDecimal("10"))
                .loan(loan("USDC", "15000"))
                .build();

        assertEquals(0, new BigDecimal("31360").compareTo(health.collateralValue(position)));
        assertEquals(0, new BigDecimal("26028.8").compareTo(health.weightedCollateral(position)));
        assertEquals(0, new BigDecimal("25088").compareTo(health.borrowingPower(position)));
        assertEquals(0, new BigDecimal("15000").compareTo(health.debtValue(position)));
        assertEquals(0, new BigDecimal("1.735253333333333333").compareTo(health.healthFactor(position).orElseThrow()));
        assertEquals(HealthBand.HEALTHY, health.band(position));
    }

    @Test
    void severalCollateralAssetsAreSummed() {
        // Normal: weighted collateral is the sum across assets, not the max, and debt is summed the same way.
        VolatileToken eth = new VolatileToken("ETH", "Ether", 18, new BigDecimal("0.02"));
        VolatileToken sol = new VolatileToken("SOL", "Solana", 9, new BigDecimal("0.05"));
        Stablecoin usdc = new Stablecoin("USDC", "USD Coin", 6, new BigDecimal("0.005"));
        Stablecoin dai = new Stablecoin("DAI", "Dai", 18, new BigDecimal("0.01"));
        PriceOracle oracle = new PriceOracle();
        publish(oracle, "ETH", "3200");
        publish(oracle, "SOL", "145");
        publish(oracle, "USDC", "1");
        publish(oracle, "DAI", "1");
        HealthFactorService health = service(oracle,
                market(eth, "0.80", "0.83"),
                market(sol, "0.65", "0.70"),
                market(usdc, "0.75", "0.80"),
                market(dai, "0.75", "0.80"));
        Position position = Position.builder(account())
                .supply("ETH", new BigDecimal("10"))
                .supply("SOL", new BigDecimal("500"))
                .loan(loan("USDC", "15000"))
                .loan(loan("DAI", "10000"))
                .build();

        assertEquals(0, new BigDecimal("74241.3").compareTo(health.weightedCollateral(position)));
        assertTrue(health.weightedCollateral(position).compareTo(new BigDecimal("48212.5")) > 0);
        assertEquals(0, new BigDecimal("25000").compareTo(health.debtValue(position)));
    }

    @Test
    void healthFactorOfExactlyOneIsAtThreshold() {
        // Edge: weighted collateral equal to debt value yields a factor that compares equal to 1
        // and band AT_THRESHOLD. It must not count as UNDERWATER.
        Stablecoin dai = new Stablecoin("DAI", "Dai", 18, new BigDecimal("0.01"));
        Stablecoin usdc = new Stablecoin("USDC", "USD Coin", 6, new BigDecimal("0.005"));
        PriceOracle oracle = new PriceOracle();
        publish(oracle, "DAI", "1");
        publish(oracle, "USDC", "1");
        HealthFactorService health = service(oracle, market(dai, "0.75", "0.80"), market(usdc, "0.75", "0.80"));
        Position position = Position.builder(account())
                .supply("DAI", new BigDecimal("100"))
                .loan(loan("USDC", "80"))
                .build();

        assertEquals(0, BigDecimal.ONE.compareTo(health.healthFactor(position).orElseThrow()));
        assertEquals(HealthBand.AT_THRESHOLD, health.band(position));
    }

    @Test
    void healthFactorJustBelowOneIsUnderwater() {
        // Edge: a factor strictly below 1, even by a single unit at Precision.SCALE, is UNDERWATER.
        Stablecoin dai = new Stablecoin("DAI", "Dai", 18, new BigDecimal("0.01"));
        Stablecoin usdc = new Stablecoin("USDC", "USD Coin", 6, new BigDecimal("0.005"));
        PriceOracle oracle = new PriceOracle();
        publish(oracle, "DAI", "1");
        publish(oracle, "USDC", "1");
        HealthFactorService health = service(oracle, market(dai, "1", "1"), market(usdc, "0.75", "0.80"));
        BigDecimal oneUnitBelow = BigDecimal.ONE.subtract(BigDecimal.ONE.movePointLeft(18));
        Position position = Position.builder(account())
                .supply("DAI", oneUnitBelow)
                .loan(loan("USDC", "1"))
                .build();

        assertTrue(health.healthFactor(position).orElseThrow().compareTo(BigDecimal.ONE) < 0);
        assertEquals(HealthBand.UNDERWATER, health.band(position));
    }

    @Test
    void volatileHaircutLowersCollateralValue() {
        // Normal: a VolatileToken is valued at oraclePrice * (1 - volatilityBuffer), so the same
        // nominal price produces less collateral than a raw oracle print would.
        VolatileToken eth = new VolatileToken("ETH", "Ether", 18, new BigDecimal("0.02"));
        PriceOracle oracle = new PriceOracle();
        publish(oracle, "ETH", "3200");
        HealthFactorService health = service(oracle, market(eth, "0.80", "0.83"));
        Position position = Position.builder(account()).supply("ETH", new BigDecimal("10")).build();

        assertEquals(0, new BigDecimal("31360").compareTo(health.collateralValue(position)));
        assertTrue(health.collateralValue(position).compareTo(new BigDecimal("32000")) < 0);
    }

    @Test
    void stablecoinInsideThePegBandUsesTheOraclePrice() {
        // Edge: a price exactly at 1 - pegBand and exactly at 1 + pegBand is still inside the band
        // and is used unchanged.
        Stablecoin dai = new Stablecoin("DAI", "Dai", 18, new BigDecimal("0.01"));
        PriceOracle oracle = new PriceOracle();
        HealthFactorService health = service(oracle, market(dai, "0.75", "0.80"));
        Position position = Position.builder(account()).supply("DAI", new BigDecimal("10")).build();

        publish(oracle, "DAI", "0.99");
        assertEquals(0, new BigDecimal("9.9").compareTo(health.collateralValue(position)));
        publish(oracle, "DAI", "1.01");
        assertEquals(0, new BigDecimal("10.1").compareTo(health.collateralValue(position)));
    }

    @Test
    void depeggedStablecoinTakesThePegHaircut() {
        // Normal: a price outside [1 - pegBand, 1 + pegBand] is valued at price * (1 - pegBand).
        // Covers both a price above the band and a price below it.
        Stablecoin dai = new Stablecoin("DAI", "Dai", 18, new BigDecimal("0.01"));
        PriceOracle oracle = new PriceOracle();
        HealthFactorService health = service(oracle, market(dai, "0.75", "0.80"));
        Position position = Position.builder(account()).supply("DAI", new BigDecimal("20000")).build();

        publish(oracle, "DAI", "0.90");
        assertEquals(0, new BigDecimal("17820").compareTo(health.collateralValue(position)));
        publish(oracle, "DAI", "1.05");
        assertEquals(0, new BigDecimal("20790").compareTo(health.collateralValue(position)));
    }

    @Test
    void missingOraclePriceIsRejected() {
        // Invalid: collateral or debt whose symbol was never published throws IllegalStateException
        // and the message names the symbol. Do not treat a missing price as zero.
        VolatileToken eth = new VolatileToken("ETH", "Ether", 18, new BigDecimal("0.02"));
        Stablecoin usdc = new Stablecoin("USDC", "USD Coin", 6, new BigDecimal("0.005"));
        HealthFactorService health = service(new PriceOracle(), market(eth, "0.80", "0.83"), market(usdc, "0.75", "0.80"));
        Position collateral = Position.builder(account()).supply("ETH", new BigDecimal("10")).build();
        Position debt = Position.builder(account()).loan(loan("USDC", "15000")).build();

        IllegalStateException missingCollateral = assertThrows(IllegalStateException.class,
                () -> health.collateralValue(collateral));
        IllegalStateException missingDebt = assertThrows(IllegalStateException.class,
                () -> health.debtValue(debt));
        assertTrue(missingCollateral.getMessage().contains("ETH"));
        assertTrue(missingDebt.getMessage().contains("USDC"));
    }

    private static Account account() {
        return new Account("alice", "Alice", AccountRole.USER);
    }

    private static Loan loan(String symbol, String outstanding) {
        return Loan.builder()
                .assetSymbol(symbol)
                .openedAt(OPENED)
                .outstanding(new BigDecimal(outstanding))
                .build();
    }

    private static Market market(Asset asset, String collateralFactor, String threshold) {
        return new Market(asset, new BigDecimal(collateralFactor), new BigDecimal(threshold),
                new BigDecimal("0.05"), new BigDecimal("0.10"), new BigDecimal("0.50"),
                new LinearRateModel(BigDecimal.ZERO, new BigDecimal("0.04")), OPENED);
    }

    private static HealthFactorService service(PriceOracle oracle, Market... listed) {
        MarketBook book = new MarketBook();
        for (Market market : listed) {
            book.register(market);
        }
        return new HealthFactorService(book, oracle);
    }

    private static void publish(PriceOracle oracle, String symbol, String price) {
        oracle.publish(new PriceTick(OPENED, symbol, new BigDecimal(price)));
    }
}
