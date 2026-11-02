package com.defi.lending.report;

import java.io.PrintStream;

/**
 * Writes a risk report as plain text. Kept off {@link RiskReportService} so a test can build a report
 * without capturing stdout, and so the console format can change without touching the ranking.
 */
public final class RiskReportPrinter {

    public void print(RiskReport report, PrintStream out) {
        // TODO: reject a null report or stream.
        // Print the timestamp, then one line per position, riskiest first:
        // rank, account id, display name, health factor to 4 decimal places (HALF_UP),
        // band, collateral USD, debt USD.
        // When the list is empty, print a single line that no position has open debt.
        // Use RoundingMode.HALF_UP only for the printed scale. The stored factor stays at Precision.SCALE.
        throw new UnsupportedOperationException("RiskReportPrinter.print");
    }
}
