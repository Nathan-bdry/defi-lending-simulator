package com.defi.lending.loader;

import com.defi.lending.exception.DataLoadException;
import com.defi.lending.oracle.PriceTick;

import java.util.ArrayList;
import java.util.List;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.DateTimeException;
import java.time.format.DateTimeFormatter;

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
        // DONE: read the classpath resource with the rules in PLAN.md.
        // Map each data row to a PriceTick, preserving file order.
        // Throw DataLoadException, with the file name and line, on a missing file, a bad timestamp or number,
        // or a negative price. A price of zero is valid.
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(resourceName)) {
            if (inputStream == null) {
                throw new DataLoadException("File not found: " + resourceName);
            }
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            String line;
            List<PriceTick> priceTicks = new ArrayList<>();
            LocalDateTime parsedTime;
            BigDecimal parsedPrice;
            int lineNumber = 0;
            boolean headerSeen = false;
            
            while ((line = reader.readLine()) != null) {
                lineNumber++;
            if (line.isBlank() || line.stripLeading().startsWith("#")) {
                continue;
            }
            if (!headerSeen) {
                if (!line.equals("timestamp,symbol,priceUsd")) {
                    throw new DataLoadException(resourceName + ":" + lineNumber + ": invalid header");
            }
            headerSeen = true;
            continue;
        }
                String[] parts = line.split(",", -1);
                if (parts.length != 3) {
                    throw new DataLoadException(resourceName + ":" + lineNumber + ": invalid number of columns");
                }
                String timestamp = parts[0];
                String symbol = parts[1];
                String priceUsd = parts[2];
                if (timestamp.isBlank() || symbol.isBlank() || priceUsd.isBlank()) {
                    throw new DataLoadException(resourceName + ":" + lineNumber + ": missing data");
                }
                try {
                    parsedTime = LocalDateTime.parse(parts[0], DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                } catch (DateTimeException e) {
                    throw new DataLoadException(resourceName + ":" + lineNumber + ": invalid timestamp");
                }
                try {
                    parsedPrice = new BigDecimal(parts[2]);
                    if (parsedPrice.compareTo(BigDecimal.ZERO) < 0) {
                            throw new DataLoadException(resourceName + ":" + lineNumber + ": negative price");
                    }
                } catch (NumberFormatException e) {
                    throw new DataLoadException(resourceName + ":" + lineNumber + ": invalid price");
                }
                priceTicks.add(new PriceTick(parsedTime, symbol, parsedPrice));
            }
            return priceTicks;
        } catch (IOException e) {
            throw new DataLoadException("Error reading file: " + resourceName, e);
        }
    }
}
