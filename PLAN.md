# Build plan

Implement the TODOs in the order below. Each step is small enough to compile on its own, and each step has tests that name the cases. Leave a test disabled until the behavior it describes is real, then remove `@Disabled` from that method only.

Getters, the market and portfolio indexes, and the field wiring inside the builders are already in place. Every protocol rule throws `UnsupportedOperationException` until its TODO is replaced. Do not add a CSV library, and do not hard-code a balance, a price, or a risk parameter that the files already carry.

## Suggested build order

1. **Constructors that validate.** `Asset`, `Stablecoin`, `VolatileToken`, `Account`, `Market`, `Operation`, `PriceTick`, and both rate models. Reject nulls, blanks, and out-of-range fractions with `IllegalArgumentException`. Add the same checks at the top of the builder methods, before they store the value.
2. **`Asset.unitValue`.** Stablecoin peg band, then volatile haircut. Enable `StablecoinTest` and `VolatileTokenTest`.
3. **Rate models.** `borrowRate`, then `supplyRate`, linear first, kinked second. Enable the rate-model tests. The kink boundary is the one that usually goes the wrong way: utilization equal to the optimum stays on the first branch.
4. **`AssetFactory` and the four loaders.** Start with `PriceScenarioLoader` (three columns). Copy that loop for the others. `MarketLoader` is the one that must call the factory and construct the rate model. Print the loaded counts from `Application` before writing any lending rule, and delete that print once loading is trusted.
5. **`PriceOracle`.** Subscribe, unsubscribe, get, publish, replay. Publish stores the price and then notifies a copy of the listener list.
6. **Position and loan mutations.** `addSupply`, `reduceSupply`, `increaseDebt`, `reduceDebt`, `capitalize`, `capitalizeSupply`, and both `toBuilder` methods. `toBuilder` deep-copies each `Loan`. A preview that shares the live loan will corrupt it.
7. **`HealthFactorService`.** Collateral, weighted collateral, borrowing power, debt, health factor, band. Empty optional when debt is zero. Enable `HealthFactorServiceTest`.
8. **`Market` transitions and `InterestAccrualService`.** `onDeposit`, `onWithdraw`, `onBorrow`, `onRepay`, `utilization`, then `accrue`. Enable `InterestAccrualServiceTest`. Check the invariant after every accrual.
9. **`LendingService`.** Deposit, then withdraw, then borrow, then repay, then `execute` / `run`. Accrue to the action's timestamp before the health check. Enable `LendingServiceTest`.
10. **`LiquidationEngine`.** `liquidate` first, so it can be called directly, then `onPriceUpdate`. Enable `LiquidationEngineTest`.
11. **Report.** `RiskReportService.build`, then `RiskReportPrinter.print`. Run the jar. The crash file's comments say which prints are meant to cross 1. Interest between 1 March and 15 March moves those figures slightly; do not expect a match to the last decimal of the worked examples below.

## Formulas

Compare `BigDecimal` values with `compareTo`. Round every multiplication and division to `Precision.SCALE` (18) with `Precision.ROUNDING` (half up). A health factor of exactly 1 is allowed for borrow and withdraw, and it is not liquidatable.

### Unit value

Volatile token:

```
unitValue = oraclePrice * (1 - volatilityBuffer)
```

Stablecoin, with peg = 1. The endpoints belong to the peg:

```
if oraclePrice is inside [1 - pegBand, 1 + pegBand]:
    unitValue = oraclePrice
else:
    unitValue = oraclePrice * (1 - pegBand)
```

A negative oracle price is rejected. Zero is allowed.

### Utilization

```
if totalSupplied = 0:
    U = 0
else:
    U = totalBorrowed / totalSupplied
```

Both totals are the claim balances (they grow with interest), not cash.

### Linear borrow rate

`slope1` is the annual rate added at full utilization.

```
borrowRate = baseRate + slope1 * U
```

### Kinked borrow rate

`slope1` is the annual rate at the kink, above the base. `slope2` is the further annual rate added between the kink and full utilization. `Uopt` is `optimalUtilization`.

```
if U <= Uopt:
    borrowRate = baseRate + slope1 * (U / Uopt)
else:
    borrowRate = baseRate + slope1 + slope2 * (U - Uopt) / (1 - Uopt)
```

At `U = Uopt` the first branch returns `baseRate + slope1`. At `U = 1` the second returns `baseRate + slope1 + slope2`.

### Supply rate

Same relationship for both models:

```
supplyRate = borrowRate * U * (1 - reserveFactor)
```

Suppliers earn nothing when `U` is 0, even if `baseRate` is positive.

### Accrual

`dt` is the number of seconds from `lastAccrual` to the target timestamp. `SECONDS_PER_YEAR` is `365 * 24 * 60 * 60`.

```
borrowFactor = 1 + borrowRate * dt / SECONDS_PER_YEAR
supplyFactor = 1 + supplyRate * dt / SECONDS_PER_YEAR
```

Read the rates from the totals **before** changing any balance. Multiply every supply of that asset by `supplyFactor` and every debt of that asset by `borrowFactor`. Then set the market's totals to the sums of the positions. Add to reserves the borrower interest that was not added to supplies:

```
addedReserves = (newBorrowed - oldBorrowed) - (newSupplied - oldSupplied)
```

Cash does not change. Afterwards `cash + totalBorrowed = totalSupplied + reserves`. A zero duration returns immediately. A timestamp in the past is rejected.

### Health factor

```
collateralUsd(asset) = supplyTokens * asset.unitValue(price)
debtUsd(asset)       = debtTokens * asset.unitValue(price)

weighted        = sum of collateralUsd * liquidationThreshold
borrowingPower  = sum of collateralUsd * collateralFactor
debtValue       = sum of debtUsd

if debtValue = 0:
    health factor is absent (Optional.empty), band NO_DEBT
else:
    health factor = weighted / debtValue
    band is HEALTHY above 1, AT_THRESHOLD at 1, UNDERWATER below 1
```

A missing oracle price is an `IllegalStateException` that names the symbol. It is not a zero price.

### Borrow and withdraw

Deposit moves tokens from the wallet into supply and into cash. It does not consult the health factor.

Withdraw is refused when the amount exceeds the user's supply (`IllegalArgumentException`) or the pool's cash (`InsufficientLiquidityException`). If the draft position still has debt and its health factor is strictly below 1, throw `InsufficientCollateralException`. Exactly 1 is allowed. No debt means the collateral check is skipped.

Borrow is refused with `InsufficientLiquidityException` when the amount exceeds cash. It is refused with `InsufficientCollateralException` when the draft debt value exceeds `borrowingPower`, or when the draft health factor is strictly below 1. Exactly 1 is allowed, but only together with the collateral-factor cap. A user must not be able to open a loan that is already on the liquidation line.

Repay is capped at the outstanding debt. The surplus stays in the wallet. It is not a deposit.

Accrue the affected market to the action's timestamp before any of these checks.

### Liquidation

Allowed only when the health factor is strictly below 1. Otherwise `PositionNotLiquidatableException`, including exactly 1 and including no debt.

`closeFactor` comes from the **debt** market. `liquidationBonus` comes from the **collateral** market.

```
maxRepay    = outstanding * closeFactor
seizeUsd    = repayAmount * debtUnitValue * (1 + liquidationBonus)
seizeTokens = seizeUsd / collateralUnitValue
```

`repayAmount` above `maxRepay` is an `IllegalArgumentException`.

When `seizeTokens` would exceed the borrower's supply, scale the repay down and take the supply:

```
repayAmount = supplyTokens * collateralUnitValue / (debtUnitValue * (1 + liquidationBonus))
seizeTokens = supplyTokens
```

Also cap `repayAmount` at the liquidator's wallet balance of the debt asset. On a price tick, a zero cap means skip that position and continue with the others. One tick liquidates a position once. It does not loop until the position is healthy.

The debt asset's cash increases (the repayment lands in the pool). The collateral asset's cash decreases (the seized tokens leave the pool and go to the liquidator's wallet, not into a new supply).

Which debt and which collateral, when the tick has to choose: the debt with the largest USD value, and the updated symbol if the user supplies it, otherwise the collateral with the largest USD value. Ties break by symbol, alphabetically, so the choice does not depend on map order.

## Worked examples

These ignore interest. Accrual between 1 March and 15 March on this book is small (USDC utilization is about 0.15 and the kinked rate there is well under 1% a year), so the inequalities survive, but a test of the full scenario should not pin a health factor to many decimals without accruing.

Opening prices: ETH 3200, WBTC 95000, SOL 145, USDC 1, DAI 1.

Alice deposits 10 ETH (buffer 0.02, threshold 0.83, collateral factor 0.80) and borrows 15000 USDC.

```
unit value       = 3200 * 0.98 = 3136
collateral       = 31360
weighted         = 31360 * 0.83 = 26028.8
borrowing power  = 31360 * 0.80 = 25088
health factor    = 26028.8 / 15000 = 1.73525...
```

Dave deposits 500 SOL (buffer 0.05, threshold 0.70, collateral factor 0.65) and borrows 44000 USDC.

```
unit value       = 145 * 0.95 = 137.75
collateral       = 68875
weighted         = 48212.5
borrowing power  = 68875 * 0.65 = 44768.75
health factor    = 48212.5 / 44000 = 1.0957...
```

44000 is under the borrowing power. 45000 would be refused by the collateral factor even though the health factor would still be above 1.

Frank deposits 20000 DAI (peg band 0.01, threshold 0.80, collateral factor 0.75) and borrows 15000 USDC. The price is inside the band, so the unit value is 1. Borrowing power is exactly 15000. Health factor is `16000 / 15000 = 1.0666...`.

At SOL 120, Dave:

```
unit value = 120 * 0.95 = 114
weighted   = 500 * 114 * 0.70 = 39900
health     = 39900 / 44000 = 0.9068...   underwater
```

At ETH 2200, Alice's health factor is still about 1.19. At ETH 1600 it is about 0.868, underwater.

At DAI 0.90, Frank is outside the band:

```
unit value = 0.90 * (1 - 0.01) = 0.891
weighted   = 20000 * 0.891 * 0.80 = 14256
health     = 14256 / 15000 = 0.9504   underwater
```

A liquidation of Dave at SOL 120 with close factor 0.50 repays `44000 * 0.50 = 22000` USDC. Bonus 0.10, debt unit value 1, collateral unit value 114:

```
seize USD    = 22000 * 1 * 1.10 = 24200
seize tokens = 24200 / 114 ≈ 212.28 SOL
```

He has 500 SOL, so the cap does not bind. Bob's wallet holds 100000 USDC, so the wallet cap does not bind either.

## Reading the CSV files

Shared rules for every loader:

- Open the classpath resource with `ClassLoader.getResourceAsStream`. A null stream is `DataLoadException` naming the file.
- Use `BufferedReader` and `String.split`. Split with limit `-1`, or a blank trailing cell disappears and `markets.csv` loses `volatilityBuffer`.
- Skip blank lines and lines whose first non-space character is `#`.
- The first remaining line is the header. Check the column names. Do not rely on column position alone if you can check the header too; the position is still the contract in each loader's Javadoc.
- Number the physical lines, starting at 1, and put the file name and that number in every `DataLoadException`. Include the `IOException` as the cause when the read itself fails.
- Parse decimals with `new BigDecimal(text)`, not `Double`. An empty optional cell is null. An empty required cell is a `DataLoadException`.
- Do not skip a bad row. Do not sort the rows.

Column contracts live on `MarketLoader`, `AccountLoader`, `OperationLoader`, and `PriceScenarioLoader`. Timestamps are `LocalDateTime` parsed with `LocalDateTime.parse` (ISO-8601, no offset).

`Application` already refuses to continue when the portfolio does not contain exactly one `LIQUIDATOR`.

## Checklist

| Requirement | Where it lives |
| --- | --- |
| Encapsulation, constructors, validating setters only where they fit | Private fields and getters. There is no `setCash` or `setOutstanding`: a bare setter would skip the rule the transition is there to enforce. Constructors, builders, and methods such as `credit` and `onDeposit` are where validation goes. |
| Abstract class and inheritance | `Asset`, `Stablecoin`, `VolatileToken` |
| Interface and polymorphism (strategy) | `InterestRateModel`, `LinearRateModel`, `KinkedRateModel` |
| Observer | `PriceOracle` (subject), `PriceListener`, `LiquidationEngine` |
| Simple factory | `AssetFactory` |
| Builder | `Loan.Builder`, `Position.Builder` |
| Custom exceptions | `exception` package: the three protocol exceptions plus `DataLoadException`. Argument checks use `IllegalArgumentException`. |
| Files at startup, nothing hard-coded | `src/main/resources`: `markets.csv`, `accounts.csv`, `operations.csv`, `prices_initial.csv`, `prices_crash.csv` |
| List, set, map | `MarketBook`, `Portfolio`, supplies and loans on `Position`, listener list on `PriceOracle` |
| Stream | `Portfolio.liquidator` is a filter. The risk ranking is a stream in `RiskReportService.build`. |
| Optional | `MarketBook.find`, `Portfolio.findAccount`, `Portfolio.findPosition`, `Position.getLoan`, `healthFactor` when there is no debt |
| Enum | `AssetType`, `OperationType`, `AccountRole`, `HealthBand`, `RateModelType` |
| `LocalDateTime` | Operations, price ticks, loan open time, market accrual, the report timestamp |
| Generics | `Map<String, Market>`, `Map<String, Loan>`, `Optional<BigDecimal>`, `List<PriceTick>`. No generic repository: nothing in the design needs one. |
| SOLID, no god class | Package split in the README. `Application` only wires the flow. |
| JUnit 5, business rules only | Disabled tests under `src/test/java` for the asset valuation, both rate models, and the four services. |
| Java 21, Maven, no extra dependencies | `pom.xml`. CSV is parsed with the JDK. |
