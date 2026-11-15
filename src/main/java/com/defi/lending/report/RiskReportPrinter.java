package com.defi.lending.report;

import java.io.PrintStream;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Writes a risk report as plain text. Kept off {@link RiskReportService} so a test can build a report
 * without capturing stdout, and so the console format can change without touching the ranking.
 */
public final class RiskReportPrinter {

    public void print(RiskReport report, PrintStream out) {
        // DONE: reject a null report or stream.
        // Print the timestamp, then one line per position, riskiest first:
        // rank, account id, display name, health factor to 4 decimal places (HALF_UP),
        // band, collateral USD, debt USD.
        // When the list is empty, print a single line that no position has open debt.
        // Use RoundingMode.HALF_UP only for the printed scale. The stored factor stays at Precision.SCALE.
        if (report == null) {
            throw new IllegalArgumentException("Report cannot be null");
        }
        if (out == null) {
            throw new IllegalArgumentException("Stream cannot be null");
        }
        out.println(DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(report.getAsOf()));
        List<PositionRisk> ranked = report.getRanked();
        if (ranked.isEmpty()) {
            out.println("No position has open debt.");
            return;
        }
        int rank = 1;
        for (PositionRisk row : ranked) {
            String factor = row.getHealthFactor().setScale(4, RoundingMode.HALF_UP).toPlainString();
            out.println(rank
                    + " | " + row.getAccountId()
                    + " | " + row.getDisplayName()
                    + " | " + factor
                    + " | " + row.getBand()
                    + " | " + row.getCollateralValue().toPlainString()
                    + " | " + row.getDebtValue().toPlainString());
            rank++;
        }
    }
}
