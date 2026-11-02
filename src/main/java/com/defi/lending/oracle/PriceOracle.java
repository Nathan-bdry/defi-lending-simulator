package com.defi.lending.oracle;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Subject of the price feed. Holds the latest USD price of each symbol and tells listeners, in subscription order,
 * after the map is updated. Replay is just a sequence of publishes so a crash file and a live tick share one path.
 */
public final class PriceOracle {

    private final Map<String, BigDecimal> prices = new LinkedHashMap<>();
    private final List<PriceListener> listeners = new ArrayList<>();

    public void subscribe(PriceListener listener) {
        // TODO: reject null. A second subscribe of the same instance must not add a duplicate,
        // or one price tick would liquidate the same position twice.
        throw new UnsupportedOperationException("PriceOracle.subscribe");
    }

    public void unsubscribe(PriceListener listener) {
        // TODO: remove that listener if it is present. Do nothing when it was never subscribed.
        throw new UnsupportedOperationException("PriceOracle.unsubscribe");
    }

    public Optional<BigDecimal> getPrice(String symbol) {
        // TODO: return the latest price, or empty when that symbol has never been published.
        // Do not throw for an unknown symbol; the caller decides whether a missing price is fatal.
        throw new UnsupportedOperationException("PriceOracle.getPrice");
    }

    public void publish(PriceTick tick) {
        // TODO: store tick's price under its symbol, then notify every listener with that same tick.
        // Store first, so a listener that calls getPrice sees the new value.
        // Walk a copy of the listener list: a listener is allowed to unsubscribe itself from the callback.
        throw new UnsupportedOperationException("PriceOracle.publish");
    }

    public void replay(List<PriceTick> scenario) {
        // TODO: reject a null list. Publish each tick in list order.
        // Do not sort. The file is the chronology the author of the scenario chose, including two ticks
        // that share a timestamp.
        throw new UnsupportedOperationException("PriceOracle.replay");
    }
}
