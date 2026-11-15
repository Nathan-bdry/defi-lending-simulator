package com.defi.lending.loader;

import com.defi.lending.exception.DataLoadException;
import com.defi.lending.model.Account;
import com.defi.lending.model.AccountRole;
import com.defi.lending.model.Portfolio;
import com.defi.lending.model.Position;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.util.Optional;

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
        // read the classpath resource.
        // Create each account once, open an empty position for it, and credit the wallet with the row's balance.
        // A non-positive balance is a DataLoadException. Register the account and position on the portfolio.
        // Throw DataLoadException, with the file name and line, on a missing file, a bad number,
        // an unknown role, or a display name / role that disagrees with an earlier row for the same id.
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(resourceName)) {
            if (inputStream == null) {
                throw new DataLoadException("File not found: " + resourceName);
            }
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            Portfolio portfolio = new Portfolio();
            String line;
            int lineNumber = 0;
            boolean headerSeen = false;

            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (line.isBlank() || line.stripLeading().startsWith("#")) {
                    continue;
                }
                if (!headerSeen) {
                    if (!line.equals("accountId,displayName,role,symbol,walletBalance")) {
                        throw new DataLoadException(resourceName + ":" + lineNumber + ": invalid header");
                    }
                    headerSeen = true;
                    continue;
                }
                String[] parts = line.split(",", -1);
                if (parts.length != 5) {
                    throw new DataLoadException(resourceName + ":" + lineNumber + ": invalid number of columns");
                }
                String accountId = parts[0];
                String displayName = parts[1];
                String roleText = parts[2];
                String symbol = parts[3];
                String balanceText = parts[4];
                if (accountId.isBlank() || displayName.isBlank() || roleText.isBlank()
                        || symbol.isBlank() || balanceText.isBlank()) {
                    throw new DataLoadException(resourceName + ":" + lineNumber + ": missing data");
                }
                AccountRole role;
                try {
                    role = AccountRole.valueOf(roleText);
                } catch (IllegalArgumentException e) {
                    throw new DataLoadException(resourceName + ":" + lineNumber + ": unknown role");
                }
                BigDecimal balance;
                try {
                    balance = new BigDecimal(balanceText);
                } catch (NumberFormatException e) {
                    throw new DataLoadException(resourceName + ":" + lineNumber + ": invalid balance");
                }
                if (balance.compareTo(BigDecimal.ZERO) <= 0) {
                    throw new DataLoadException(resourceName + ":" + lineNumber + ": non-positive balance");
                }

                Optional<Account> existing = portfolio.findAccount(accountId);
                if (existing.isPresent()) {
                    Account account = existing.get();
                    if (!account.getDisplayName().equals(displayName) || account.getRole() != role) {
                        throw new DataLoadException(resourceName + ":" + lineNumber + ": account identity mismatch");
                    }
                    account.credit(symbol, balance);
                } else {
                    Account account = new Account(accountId, displayName, role);
                    portfolio.add(account, Position.open(account));
                    account.credit(symbol, balance);
                }
            }
            return portfolio;
        } catch (IOException e) {
            throw new DataLoadException("Error reading file: " + resourceName, e);
        }
    }
}
