package com.defi.lending.report;

import com.defi.lending.model.Portfolio;
import com.defi.lending.model.Position;
import com.defi.lending.service.HealthFactorService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

/**
 * Builds the ranking the program prints after the price scenario.
 * Reads positions and asks {@link HealthFactorService} for the numbers; it does not move funds.
 */
public final class RiskReportService {

    private final Portfolio portfolio;
    private final HealthFactorService health;

    public RiskReportService(Portfolio portfolio, HealthFactorService health) {
        // reject a null portfolio or health service.
        if (portfolio == null) {
            throw new IllegalArgumentException("Portfolio cannot be null");
        }
        if (health == null) {
            throw new IllegalArgumentException("Health factor service cannot be null");
        }
        this.portfolio = portfolio;
        this.health = health;
    }

    public RiskReport build(LocalDateTime asOf) {
        // reject a null timestamp.
        // Keep positions that have a health factor (those with debt). Drop pure suppliers.
        // Sort by health factor ascending, so the riskiest line is first.
        // Break ties by account id so two equal factors do not swap between runs.
        // Use a stream for the filter and the sort. Map each survivor to a PositionRisk
        // whose collateral value is the unweighted USD total and whose band comes from HealthFactorService.
        if (asOf == null) {
            throw new IllegalArgumentException("Timestamp cannot be null");
        }
        List<PositionRisk> ranked = portfolio.positions().stream()
                .filter(position -> health.healthFactor(position).isPresent())
                .sorted(Comparator
                        .comparing((Position position) -> health.healthFactor(position).orElseThrow())
                        .thenComparing(position -> position.getAccount().getId()))
                .map(position -> new PositionRisk(
                        position.getAccount().getId(),
                        position.getAccount().getDisplayName(),
                        health.healthFactor(position).orElseThrow(),
                        health.band(position),
                        health.collateralValue(position),
                        health.debtValue(position)))
                .toList();
        return new RiskReport(asOf, ranked);
    }
}
