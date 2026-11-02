package com.defi.lending.loader;

import com.defi.lending.exception.DataLoadException;
import com.defi.lending.oracle.PriceTick;

import java.util.List;

/**
 * Reads a price scenario such as {@code prices_initial.csv} or {@code prices_crash.csv}.
 * The same loader covers both files; which file is the crash is the caller's choice.
 */
public final class PriceScenarioLoader {

    /**
     * Columns, in order: timestamp, symbol, priceUsd.
     * timestamp is an ISO-8601 local date-time. priceUsd is a non-negative USD price per whole token.
     */
    public List<PriceTick> load(String resourceName) throws DataLoadException {
        // TODO: read the classpath resource with the rules in PLAN.md.
        // Map each data row to a PriceTick, preserving file order.
        // Throw DataLoadException, with the file name and line, on a missing file, a bad timestamp or number,
        // or a negative price. A price of zero is valid.
        throw new UnsupportedOperationException("PriceScenarioLoader.load");
    }
}
