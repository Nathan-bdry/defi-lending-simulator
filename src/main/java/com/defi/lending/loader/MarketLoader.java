package com.defi.lending.loader;

import com.defi.lending.exception.DataLoadException;
import com.defi.lending.model.Asset;
import com.defi.lending.model.AssetType;
import com.defi.lending.model.Market;
import com.defi.lending.rates.InterestRateModel;
import com.defi.lending.rates.KinkedRateModel;
import com.defi.lending.rates.LinearRateModel;
import com.defi.lending.rates.RateModelType;

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
 * Builds markets from {@code markets.csv}, including the asset (via {@link AssetFactory}) and the rate model.
 * The rate-model choice stays here, next to the file that names it, rather than inside {@link Market}.
 */
public final class MarketLoader {

    private static final String HEADER = "openedAt,symbol,name,type,decimals,collateralFactor,liquidationThreshold,"
            + "liquidationBonus,reserveFactor,closeFactor,rateModel,baseRate,slope1,slope2,optimalUtilization,"
            + "pegBand,volatilityBuffer";

    private final AssetFactory assets;

    public MarketLoader(AssetFactory assets) {
        // DONE: reject a null factory. Injected so this class does not construct asset subclasses itself.
        if (assets == null) {
            throw new IllegalArgumentException("Asset factory cannot be null");
        }
        this.assets = assets;
    }

    /**
     * Columns, in order: openedAt, symbol, name, type, decimals, collateralFactor, liquidationThreshold,
     * liquidationBonus, reserveFactor, closeFactor, rateModel, baseRate, slope1, slope2, optimalUtilization,
     * pegBand, volatilityBuffer.
     * LINEAR leaves slope2 and optimalUtilization blank and uses baseRate and slope1.
     * KINKED requires all four rate fields. STABLECOIN requires pegBand. VOLATILE requires volatilityBuffer.
     * openedAt is an ISO-8601 local date-time, the moment interest starts.
     */
    public List<Market> load(String resourceName) throws DataLoadException {
        // DONE: read the classpath resource (comments, header, line numbers).
        // For each data row, ask the factory for the asset, construct a LinearRateModel or a KinkedRateModel
        // from rateModel, then construct a Market. Use `assets` for every row.
        // Throw DataLoadException, naming the file and the line, on a missing file, a bad number,
        // an unknown type or rate model, or a blank cell a required column needs.
        // Do not catch-and-skip a bad row.
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(resourceName)) {
            if (inputStream == null) {
                throw new DataLoadException("File not found: " + resourceName);
            }
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            List<Market> markets = new ArrayList<>();
            String line;
            int lineNumber = 0;
            boolean headerSeen = false;

            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (line.isBlank() || line.stripLeading().startsWith("#")) {
                    continue;
                }
                if (!headerSeen) {
                    if (!line.equals(HEADER)) {
                        throw new DataLoadException(resourceName + ":" + lineNumber + ": invalid header");
                    }
                    headerSeen = true;
                    continue;
                }
                String[] parts = line.split(",", -1);
                if (parts.length != 17) {
                    throw new DataLoadException(resourceName + ":" + lineNumber + ": invalid number of columns");
                }
                markets.add(readMarket(resourceName, lineNumber, parts));
            }
            return markets;
        } catch (IOException e) {
            throw new DataLoadException("Error reading file: " + resourceName, e);
        }
    }

    private Market readMarket(String resourceName, int lineNumber, String[] parts) throws DataLoadException {
        LocalDateTime openedAt = requiredTimestamp(resourceName, lineNumber, parts[0]);
        String symbol = requiredText(resourceName, lineNumber, parts[1]);
        String name = requiredText(resourceName, lineNumber, parts[2]);
        AssetType type = requiredAssetType(resourceName, lineNumber, parts[3]);
        int decimals = requiredInt(resourceName, lineNumber, parts[4]);
        BigDecimal collateralFactor = requiredDecimal(resourceName, lineNumber, parts[5]);
        BigDecimal liquidationThreshold = requiredDecimal(resourceName, lineNumber, parts[6]);
        BigDecimal liquidationBonus = requiredDecimal(resourceName, lineNumber, parts[7]);
        BigDecimal reserveFactor = requiredDecimal(resourceName, lineNumber, parts[8]);
        BigDecimal closeFactor = requiredDecimal(resourceName, lineNumber, parts[9]);
        RateModelType rateModelType = requiredRateModel(resourceName, lineNumber, parts[10]);
        BigDecimal baseRate = requiredDecimal(resourceName, lineNumber, parts[11]);
        BigDecimal slope1 = requiredDecimal(resourceName, lineNumber, parts[12]);
        BigDecimal slope2 = optionalDecimal(resourceName, lineNumber, parts[13]);
        BigDecimal optimalUtilization = optionalDecimal(resourceName, lineNumber, parts[14]);
        BigDecimal pegBand = optionalDecimal(resourceName, lineNumber, parts[15]);
        BigDecimal volatilityBuffer = optionalDecimal(resourceName, lineNumber, parts[16]);

        if (type == AssetType.STABLECOIN && pegBand == null) {
            throw new DataLoadException(resourceName + ":" + lineNumber + ": missing pegBand");
        }
        if (type == AssetType.VOLATILE && volatilityBuffer == null) {
            throw new DataLoadException(resourceName + ":" + lineNumber + ": missing volatilityBuffer");
        }
        if (rateModelType == RateModelType.KINKED && (slope2 == null || optimalUtilization == null)) {
            throw new DataLoadException(resourceName + ":" + lineNumber + ": missing kink fields");
        }

        try {
            InterestRateModel rateModel = rateModelType == RateModelType.LINEAR
                    ? new LinearRateModel(baseRate, slope1)
                    : new KinkedRateModel(baseRate, slope1, slope2, optimalUtilization);
            Asset asset = assets.create(symbol, name, type, decimals, pegBand, volatilityBuffer);
            return new Market(asset, collateralFactor, liquidationThreshold, liquidationBonus,
                    reserveFactor, closeFactor, rateModel, openedAt);
        } catch (IllegalArgumentException e) {
            throw new DataLoadException(resourceName + ":" + lineNumber + ": " + e.getMessage(), e);
        }
    }

    private static String requiredText(String resourceName, int lineNumber, String text) throws DataLoadException {
        if (text.isBlank()) {
            throw new DataLoadException(resourceName + ":" + lineNumber + ": missing data");
        }
        return text;
    }

    private static LocalDateTime requiredTimestamp(String resourceName, int lineNumber, String text)
            throws DataLoadException {
        if (text.isBlank()) {
            throw new DataLoadException(resourceName + ":" + lineNumber + ": missing data");
        }
        try {
            return LocalDateTime.parse(text);
        } catch (DateTimeException e) {
            throw new DataLoadException(resourceName + ":" + lineNumber + ": invalid timestamp");
        }
    }

    private static int requiredInt(String resourceName, int lineNumber, String text) throws DataLoadException {
        if (text.isBlank()) {
            throw new DataLoadException(resourceName + ":" + lineNumber + ": missing data");
        }
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException e) {
            throw new DataLoadException(resourceName + ":" + lineNumber + ": invalid number");
        }
    }

    private static BigDecimal requiredDecimal(String resourceName, int lineNumber, String text)
            throws DataLoadException {
        if (text.isBlank()) {
            throw new DataLoadException(resourceName + ":" + lineNumber + ": missing data");
        }
        return parseDecimal(resourceName, lineNumber, text);
    }

    private static BigDecimal optionalDecimal(String resourceName, int lineNumber, String text)
            throws DataLoadException {
        if (text.isBlank()) {
            return null;
        }
        return parseDecimal(resourceName, lineNumber, text);
    }

    private static BigDecimal parseDecimal(String resourceName, int lineNumber, String text) throws DataLoadException {
        try {
            return new BigDecimal(text);
        } catch (NumberFormatException e) {
            throw new DataLoadException(resourceName + ":" + lineNumber + ": invalid number");
        }
    }

    private static AssetType requiredAssetType(String resourceName, int lineNumber, String text)
            throws DataLoadException {
        if (text.isBlank()) {
            throw new DataLoadException(resourceName + ":" + lineNumber + ": missing data");
        }
        try {
            return AssetType.valueOf(text);
        } catch (IllegalArgumentException e) {
            throw new DataLoadException(resourceName + ":" + lineNumber + ": unknown asset type");
        }
    }

    private static RateModelType requiredRateModel(String resourceName, int lineNumber, String text)
            throws DataLoadException {
        if (text.isBlank()) {
            throw new DataLoadException(resourceName + ":" + lineNumber + ": missing data");
        }
        try {
            return RateModelType.valueOf(text);
        } catch (IllegalArgumentException e) {
            throw new DataLoadException(resourceName + ":" + lineNumber + ": unknown rate model");
        }
    }
}
