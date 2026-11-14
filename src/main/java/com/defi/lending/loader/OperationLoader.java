package com.defi.lending.loader;

import com.defi.lending.exception.DataLoadException;
import com.defi.lending.model.Operation;
import com.defi.lending.model.OperationType;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.DateTimeException;
import java.util.ArrayList;
import java.util.List;

/**
 * Reads {@code operations.csv} in file order. The list is the execution order; this loader does not sort it.
 */
public final class OperationLoader {

    /**
     * Columns, in order: timestamp, accountId, action, symbol, amount.
     * timestamp is an ISO-8601 local date-time. action is a name from {@code OperationType}.
     */
    public List<Operation> load(String resourceName) throws DataLoadException {
        // DONE: read the classpath resource with the rules in PLAN.md.
        // Map each data row to an Operation. Throw DataLoadException, with the file name and line,
        // on a missing file, a bad timestamp or number, an unknown action, or a non-positive amount.
        // Keep the file order, including equal timestamps.
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(resourceName)) {
            if (inputStream == null) {
                throw new DataLoadException("File not found: " + resourceName);
            }
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            List<Operation> operations = new ArrayList<>();
            String line;
            int lineNumber = 0;
            boolean headerSeen = false;

            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (line.isBlank() || line.stripLeading().startsWith("#")) {
                    continue;
                }
                if (!headerSeen) {
                    if (!line.equals("timestamp,accountId,action,symbol,amount")) {
                        throw new DataLoadException(resourceName + ":" + lineNumber + ": invalid header");
                    }
                    headerSeen = true;
                    continue;
                }
                String[] parts = line.split(",", -1);
                if (parts.length != 5) {
                    throw new DataLoadException(resourceName + ":" + lineNumber + ": invalid number of columns");
                }
                String timestampText = parts[0];
                String accountId = parts[1];
                String actionText = parts[2];
                String symbol = parts[3];
                String amountText = parts[4];
                if (timestampText.isBlank() || accountId.isBlank() || actionText.isBlank()
                        || symbol.isBlank() || amountText.isBlank()) {
                    throw new DataLoadException(resourceName + ":" + lineNumber + ": missing data");
                }
                LocalDateTime timestamp;
                try {
                    timestamp = LocalDateTime.parse(timestampText);
                } catch (DateTimeException e) {
                    throw new DataLoadException(resourceName + ":" + lineNumber + ": invalid timestamp");
                }
                OperationType action;
                try {
                    action = OperationType.valueOf(actionText);
                } catch (IllegalArgumentException e) {
                    throw new DataLoadException(resourceName + ":" + lineNumber + ": unknown action");
                }
                BigDecimal amount;
                try {
                    amount = new BigDecimal(amountText);
                } catch (NumberFormatException e) {
                    throw new DataLoadException(resourceName + ":" + lineNumber + ": invalid amount");
                }
                if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                    throw new DataLoadException(resourceName + ":" + lineNumber + ": non-positive amount");
                }
                operations.add(new Operation(timestamp, accountId, action, symbol, amount));
            }
            return operations;
        } catch (IOException e) {
            throw new DataLoadException("Error reading file: " + resourceName, e);
        }
    }
}
