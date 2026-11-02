package com.defi.lending.report;

import com.defi.lending.model.Portfolio;
import com.defi.lending.service.HealthFactorService;

import java.time.LocalDateTime;

/**
 * Builds the ranking the program prints after the price scenario.
 * Reads positions and asks {@link HealthFactorService} for the numbers; it does not move funds.
 */
public final class RiskReportService {

    private final Portfolio portfolio;
    private final HealthFactorService health;

    public RiskReportService(Portfolio portfolio, HealthFactorService health) {
        // TODO: reject a null portfolio or health service.
        this.portfolio = portfolio;
        this.health = health;
    }

    public RiskReport build(LocalDateTime asOf) {
        // TODO: reject a null timestamp.
        // Keep positions that have a health factor (those with debt). Drop pure suppliers.
        // Sort by health factor ascending, so the riskiest line is first.
        // Break ties by account id so two equal factors do not swap between runs.
        // Use a stream for the filter and the sort. Map each survivor to a PositionRisk
        // whose collateral value is the unweighted USD total and whose band comes from HealthFactorService.
        throw new UnsupportedOperationException("RiskReportService.build");
    }
}
