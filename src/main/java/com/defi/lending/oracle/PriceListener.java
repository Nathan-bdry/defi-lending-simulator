package com.defi.lending.oracle;

/**
 * Receives prices after the oracle has stored them.
 * One method on purpose: a listener that also needed to publish prices would be a second subject, not a listener.
 */
public interface PriceListener {

    /**
     * @param tick the update that is already visible from {@link PriceOracle#getPrice(String)}
     */
    void onPriceUpdate(PriceTick tick);
}
