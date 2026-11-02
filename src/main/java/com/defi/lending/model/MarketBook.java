package com.defi.lending.model;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Markets keyed by asset symbol, in file order.
 * A second listing of the same symbol is refused: utilization of one pool must have a single total.
 */
public final class MarketBook {

    private final Map<String, Market> markets = new LinkedHashMap<>();

    public void register(Market market) {
        Objects.requireNonNull(market, "market");
        String symbol = market.getAsset().getSymbol();
        Market previous = markets.putIfAbsent(symbol, market);
        if (previous != null) {
            throw new IllegalStateException("duplicate market " + symbol);
        }
    }

    public Optional<Market> find(String symbol) {
        return Optional.ofNullable(markets.get(symbol));
    }

    public Collection<Market> all() {
        return List.copyOf(markets.values());
    }
}
