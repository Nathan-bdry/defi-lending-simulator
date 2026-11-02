package com.defi.lending;

import com.defi.lending.exception.DataLoadException;
import com.defi.lending.exception.InsufficientCollateralException;
import com.defi.lending.exception.InsufficientLiquidityException;
import com.defi.lending.exception.PositionNotLiquidatableException;
import com.defi.lending.loader.AccountLoader;
import com.defi.lending.loader.AssetFactory;
import com.defi.lending.loader.MarketLoader;
import com.defi.lending.loader.OperationLoader;
import com.defi.lending.loader.PriceScenarioLoader;
import com.defi.lending.model.Market;
import com.defi.lending.model.MarketBook;
import com.defi.lending.model.Operation;
import com.defi.lending.model.Portfolio;
import com.defi.lending.oracle.PriceOracle;
import com.defi.lending.oracle.PriceTick;
import com.defi.lending.report.RiskReport;
import com.defi.lending.report.RiskReportPrinter;
import com.defi.lending.report.RiskReportService;
import com.defi.lending.service.HealthFactorService;
import com.defi.lending.service.InterestAccrualService;
import com.defi.lending.service.LendingService;
import com.defi.lending.service.LiquidationEngine;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Wires the simulator and nothing else.
 * Load files, publish the opening prices, run the user actions, replay the crash, print the risk report.
 * Each of those steps is another class; this one only orders them and reports a failure in one line.
 */
public final class Application {

    static final String MARKETS = "markets.csv";
    static final String ACCOUNTS = "accounts.csv";
    static final String OPERATIONS = "operations.csv";
    static final String INITIAL_PRICES = "prices_initial.csv";
    static final String DEFAULT_SCENARIO = "prices_crash.csv";

    private Application() {
    }

    public static void main(String[] args) {
        if (args.length > 1) {
            System.err.println("Usage: java -jar defi-lending-simulator.jar [scenario-resource]");
            return;
        }
        String scenario = args.length == 1 ? args[0] : DEFAULT_SCENARIO;
        try {
            run(scenario);
        } catch (DataLoadException ex) {
            System.err.println("Could not load protocol data: " + ex.getMessage());
        } catch (InsufficientCollateralException | InsufficientLiquidityException
                | PositionNotLiquidatableException ex) {
            System.err.println("Operation refused: " + ex.getMessage());
        } catch (IllegalArgumentException | IllegalStateException ex) {
            System.err.println("Cannot continue: " + ex.getMessage());
        } catch (UnsupportedOperationException ex) {
            // Rule methods throw this until their TODO is replaced, so a run explains what is left.
            System.err.println("Not implemented yet: " + ex.getMessage());
            System.err.println("The project compiles. Complete the TODOs to run the simulation.");
        }
    }

    static void run(String scenarioResource) throws DataLoadException {
        AssetFactory factory = new AssetFactory();
        List<Market> markets = new MarketLoader(factory).load(MARKETS);
        Portfolio portfolio = new AccountLoader().load(ACCOUNTS);
        List<Operation> operations = new OperationLoader().load(OPERATIONS);
        List<PriceTick> initialPrices = new PriceScenarioLoader().load(INITIAL_PRICES);
        List<PriceTick> scenario = new PriceScenarioLoader().load(scenarioResource);

        MarketBook book = new MarketBook();
        for (Market market : markets) {
            book.register(market);
        }
        if (portfolio.liquidator().isEmpty()) {
            throw new DataLoadException("accounts.csv must name exactly one account with role LIQUIDATOR");
        }

        PriceOracle oracle = new PriceOracle();
        HealthFactorService health = new HealthFactorService(book, oracle);
        InterestAccrualService interest = new InterestAccrualService(book, portfolio);
        LendingService lending = new LendingService(book, portfolio, health, interest);
        // Subscribed before either replay so opening prices and the crash share the liquidation path.
        oracle.subscribe(new LiquidationEngine(book, portfolio, health, interest));

        oracle.replay(initialPrices);
        lending.run(operations);
        oracle.replay(scenario);

        LocalDateTime asOf = scenario.isEmpty()
                ? LocalDateTime.now()
                : scenario.get(scenario.size() - 1).getTimestamp();
        RiskReport report = new RiskReportService(portfolio, health).build(asOf);
        new RiskReportPrinter().print(report, System.out);
    }
}
