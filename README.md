# DeFi Lending Protocol Simulator

A console simulator of an overcollateralized money market in the style of Aave. Depositors supply crypto assets and earn interest that rises with utilization. Borrowers post collateral, and a position that falls through a health factor of 1 can be liquidated, with a bonus for the liquidator. Prices are replayed from a file, so a crash can be demonstrated without a network, a wallet, or a chain.

The domain is the one used by production lending protocols: collateral factor, liquidation threshold, kinked interest, close factor, and an oracle. The structure is a Java OOP project aimed at that same domain.

The numeric rules are specified as `// TODO` notes on the methods that own them, and those methods throw until the notes are implemented. The project compiles, the sample books are real, and the startup flow is already wired.

## Purpose

Show, in a program small enough to defend line by line, how a lending pool prices risk. Supply is not the same thing as a wallet balance. Borrowing is limited by collateral and by the cash actually sitting in the pool. Interest is a function of utilization. A price move is what makes a previously healthy loan liquidatable. The closing risk report ranks whatever debt survived the scenario.

## How to run

Requires Java 21 and Maven. No other libraries.

```bash
mvn -q package
java -jar target/defi-lending-simulator-0.1.0-SNAPSHOT.jar
```

The argument, when present, is the classpath name of a price scenario. It defaults to `prices_crash.csv`.

```bash
java -jar target/defi-lending-simulator-0.1.0-SNAPSHOT.jar prices_crash.csv
```

Until the TODOs are filled in, the process prints `Not implemented yet` and the name of the first method that still throws. Files are read from the classpath (`src/main/resources`), not from a working-directory path.

## Main features

- Deposit, withdraw, borrow, and repay, with balances loaded from files rather than written into the code.
- Health factor = weighted collateral / debt. A borrow or withdraw that would push it below 1 is refused. A factor of exactly 1 is still allowed.
- A second, tighter cap on new borrows: debt value may not exceed collateral value times the collateral factor.
- Interest from pool utilization, with a linear model and a kinked model.
- A price oracle that replays a scenario and notifies listeners. The liquidation engine is a listener.
- Liquidation of positions below a health factor of 1, capped by a close factor and paying the liquidator a collateral bonus.
- A final risk report, riskiest open debt first.

## Main classes

| Package | Types | Responsibility |
| --- | --- | --- |
| *(root)* | `Application`, `Precision` | Startup order, and the shared rounding scale |
| `model` | `Asset`, `Stablecoin`, `VolatileToken`, `Market`, `MarketBook`, `Account`, `Portfolio`, `Position`, `Loan`, `Operation` | State: tokens, pools, wallets, positions. Enums: `AssetType`, `OperationType`, `AccountRole`, `HealthBand` |
| `rates` | `InterestRateModel`, `LinearRateModel`, `KinkedRateModel` | How utilization becomes an annual rate |
| `oracle` | `PriceOracle`, `PriceListener`, `PriceTick` | Latest prices, and who gets told when one changes |
| `service` | `LendingService`, `HealthFactorService`, `InterestAccrualService`, `LiquidationEngine` | The protocol rules |
| `loader` | `AssetFactory`, `MarketLoader`, `AccountLoader`, `OperationLoader`, `PriceScenarioLoader` | Classpath CSVs to objects |
| `report` | `RiskReportService`, `RiskReport`, `PositionRisk`, `RiskReportPrinter` | Ranking, then the text of that ranking |
| `exception` | `InsufficientCollateralException`, `InsufficientLiquidityException`, `PositionNotLiquidatableException`, `DataLoadException` | Rule failures, and a file that cannot be read |

`MarketBook` and `Portfolio` are the in-memory indexes (map, optional lookup, one stream to find the liquidator). They do not decide whether a borrow is safe.

## Technical decisions

1. **Amounts are whole tokens, not base units.** The files say `10` ETH, not wei. `decimals` is metadata plus a check that an amount is not finer than the asset allows. `BigDecimal` with scale 18 and half-up rounding (`Precision`) keeps the health-factor comparison identical in every service. `double` is not used.

2. **Interest is capitalized into the balances.** Each accrual multiplies supply and debt by a factor derived from the annual rate and the elapsed seconds. A balance read is the amount owed. The alternative, Aave's scaled index, avoids touching every position but hides the amount behind a division. Here the pool invariant `cash + totalBorrowed = totalSupplied + reserves` is something a test can check with `compareTo`. Cash does not grow when interest accrues: interest is a claim, not new tokens.

3. **The health factor uses the liquidation threshold. A new borrow also has to respect the collateral factor.** Liquidation, and the guard on borrow and withdraw, use the threshold, so a position can sit between loan-to-value and the liquidation line after a price move and still not be liquidatable. A new borrow must also stay within `sum(collateral value × collateral factor)`, which is what stops a user from borrowing all the way to the line. Exactly 1.0 is healthy. Only a factor strictly below 1 can be liquidated.

4. **Valuation is polymorphic. Governance numbers sit on the market.** `Asset.unitValue` is where a stablecoin and a volatile token differ. Loan-to-value, threshold, bonus, close factor, reserve factor, and the rate model are per market and come from the file, so a new listing does not need a new subclass.

5. **The oracle pushes. It does not liquidate.** `PriceOracle.replay` publishes ticks in file order and does not sort them. `LiquidationEngine` is a `PriceListener`. A second listener can subscribe without an edit to the oracle. One tick liquidates a position at most once, up to the close factor, so a single print cannot wipe the debt.

## Inheritance, polymorphism, abstract classes, and interfaces

- **Abstract class.** `Asset` holds the symbol, name, and decimals. `Stablecoin` and `VolatileToken` implement `unitValue` differently: a peg band versus a volatility haircut. Callers value collateral and debt through `Asset` only.
- **Interface, strategy.** `InterestRateModel` has two implementations, `LinearRateModel` and `KinkedRateModel`. `Market` stores the interface. `LendingService` never branches on the concrete model.
- **Interface, observer.** `PriceListener.onPriceUpdate` is the notification. `PriceOracle` is the subject. `LiquidationEngine` is the implementation that reacts.
- **Simple factory.** `AssetFactory.create` is the only method that constructs a `Stablecoin` or a `VolatileToken`. `MarketLoader` depends on the factory.
- **Builder.** `Loan.Builder` and `Position.Builder` stage a debt line and a position. A health-factor preview builds a draft position instead of mutating the live one and rolling back. `Position.toBuilder` has to deep-copy loans, or the preview would change the real debt.

## Custom exceptions

- `InsufficientCollateralException` — a borrow or withdraw whose draft health factor is below 1, or a borrow that exceeds borrowing power.
- `InsufficientLiquidityException` — a borrow (or a withdraw) the pool cannot pay because `cash` is short. An empty user wallet is not this exception.
- `PositionNotLiquidatableException` — liquidation of a position whose health factor is missing or at least 1.
- `DataLoadException` — a missing file or a row that cannot become an object. It is checked, so `main` has to handle it.

Validation of arguments (blank symbol, negative amount, factor out of range) uses `IllegalArgumentException`. A broken startup invariant, such as two markets with the same symbol, uses `IllegalStateException`.

## How data is loaded

On startup `Application` loads five classpath resources, in this order: `markets.csv`, `accounts.csv`, `operations.csv`, `prices_initial.csv`, and the scenario (default `prices_crash.csv`). Nothing in the Java sources is a balance, a price, or a risk parameter.

The sample book is five listings (ETH, WBTC, SOL, USDC, DAI), six accounts, a short sequence of deposits and borrows, opening prices, and a crash. Carol supplies USDC and DAI. Alice, Dave, Erin, and Frank borrow against ETH, SOL, WBTC, and DAI. Bob is the liquidator and keeps USDC in his wallet so he can repay other people's debt. The crash file is commented with the prints that are meant to cross a health factor of 1. Those comments assume the formulas in `PLAN.md`; accrued interest moves the exact figures slightly.

Reading rules, column lists, and the formulas are in `PLAN.md`. The loaders are where parsing is implemented. There is no CSV library.

## How SOLID is applied

- **Single responsibility.** `Market` stores a pool. `LendingService` performs user actions. `HealthFactorService` only values. `InterestAccrualService` only moves time. `LiquidationEngine` only reacts to prices. Loaders only parse. `RiskReportPrinter` only formats. `Application` only orders the steps.
- **Open/closed.** A new rate curve is a new `InterestRateModel`. A new valuation policy is a new `Asset` subclass. A new reaction to prices is a new `PriceListener`. The oracle and the lending service stay as they are.
- **Liskov.** Any `Asset` can be listed. Both subclasses honor `unitValue`: one non-negative USD value per token, given a non-negative oracle price. Both rate models honor the same supply-rate relationship.
- **Interface segregation.** `PriceListener` is one method. `InterestRateModel` is the rate question and nothing about accounts.
- **Dependency inversion.** `LendingService` is given a health service and an accrual service. It reads rates through `InterestRateModel`, not through `LinearRateModel`. `MarketLoader` is given an `AssetFactory` and does not choose subclasses on its own.

There is no type that both parses files and liquidates loans.

## How to run tests

```bash
mvn test
```

Tests cover the protocol rules (valuation, rates, accrual, lending, liquidation). They are marked `@Disabled("TODO")` until the method they describe exists. Each disabled method says whether it is the normal case, a boundary (including a health factor of exactly 1), or an invalid or exception case. Enable them one at a time as the matching TODO is implemented. `mvn test` passes while they stay disabled.
