package com.defi.lending.oracle;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * One price observation from a scenario file. Immutable so a listener cannot rewrite a tick
 * that other listeners still have to see.
 */
public final class PriceTick {

    private final LocalDateTime timestamp;
    private final String symbol;
    private final BigDecimal priceUsd;

    public PriceTick(LocalDateTime timestamp, String symbol, BigDecimal priceUsd) {
        // DONE: reject a null timestamp, a blank symbol, or a null or negative price
        // (IllegalArgumentException). A price of zero is allowed: the asset has become worthless.
        if (timestamp == null) {
            throw new IllegalArgumentException("Timestamp cannot be null");
        }
        if (symbol == null || symbol.isBlank()) {
            throw new IllegalArgumentException("Symbol cannot be blank");
        }
        if (priceUsd == null || priceUsd.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Price cannot be null or negative");
        }
        this.timestamp = timestamp;
        this.symbol = symbol;
        this.priceUsd = priceUsd;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public String getSymbol() {
        return symbol;
    }

    public BigDecimal getPriceUsd() {
        return priceUsd;
    }
}
