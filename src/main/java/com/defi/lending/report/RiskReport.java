package com.defi.lending.report;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Positions ordered from the smallest health factor to the largest, at one moment.
 * The list is copied so a caller cannot reorder a report that has already been printed.
 */
public final class RiskReport {

    private final LocalDateTime asOf;
    private final List<PositionRisk> ranked;

    public RiskReport(LocalDateTime asOf, List<PositionRisk> ranked) {
        // TODO: reject a null timestamp or a null list.
        this.asOf = asOf;
        this.ranked = List.copyOf(ranked);
    }

    public LocalDateTime getAsOf() {
        return asOf;
    }

    public List<PositionRisk> getRanked() {
        return ranked;
    }
}
