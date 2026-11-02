package com.defi.lending.loader;

import com.defi.lending.exception.DataLoadException;
import com.defi.lending.model.Market;

import java.util.List;

/**
 * Builds markets from {@code markets.csv}, including the asset (via {@link AssetFactory}) and the rate model.
 * The rate-model choice stays here, next to the file that names it, rather than inside {@link Market}.
 */
public final class MarketLoader {

    private final AssetFactory assets;

    public MarketLoader(AssetFactory assets) {
        // TODO: reject a null factory. Injected so this class does not construct asset subclasses itself.
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
        // TODO: read the classpath resource with the rules in PLAN.md (comments, header, line numbers).
        // For each data row, ask the factory for the asset, construct a LinearRateModel or a KinkedRateModel
        // from rateModel, then construct a Market. Use `assets` for every row.
        // Throw DataLoadException, naming the file and the line, on a missing file, a bad number,
        // an unknown type or rate model, or a blank cell a required column needs.
        // Do not catch-and-skip a bad row.
        throw new UnsupportedOperationException("MarketLoader.load");
    }
}
