package com.defi.lending.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * One row of {@code operations.csv}: who did what, to which asset, and when.
 * Ordering is the list order from the loader, not something this object re-derives.
 */
public final class Operation {

    private final LocalDateTime timestamp;
    private final String accountId;
    private final OperationType type;
    private final String symbol;
    private final BigDecimal amount;

    public Operation(LocalDateTime timestamp, String accountId, OperationType type,
                     String symbol, BigDecimal amount) {
        // TODO: reject a null timestamp or type, a blank accountId or symbol, or a null or non-positive amount.
        this.timestamp = timestamp;
        this.accountId = accountId;
        this.type = type;
        this.symbol = symbol;
        this.amount = amount;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public String getAccountId() {
        return accountId;
    }

    public OperationType getType() {
        return type;
    }

    public String getSymbol() {
        return symbol;
    }

    public BigDecimal getAmount() {
        return amount;
    }
}
