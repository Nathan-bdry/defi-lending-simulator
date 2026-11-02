package com.defi.lending.loader;

import com.defi.lending.exception.DataLoadException;
import com.defi.lending.model.Portfolio;

/**
 * Builds the portfolio from {@code accounts.csv}.
 * Several rows share an account id: one row per asset the wallet already holds.
 */
public final class AccountLoader {

    /**
     * Columns, in order: accountId, displayName, role, symbol, walletBalance.
     * role is USER or LIQUIDATOR. Repeating an id adds another wallet balance;
     * the display name and role must match the first row for that id.
     */
    public Portfolio load(String resourceName) throws DataLoadException {
        // TODO: read the classpath resource with the rules in PLAN.md.
        // Create each account once, open an empty position for it, and credit the wallet with the row's balance.
        // A non-positive balance is a DataLoadException. Register the account and position on the portfolio.
        // Throw DataLoadException, with the file name and line, on a missing file, a bad number,
        // an unknown role, or a display name / role that disagrees with an earlier row for the same id.
        throw new UnsupportedOperationException("AccountLoader.load");
    }
}
