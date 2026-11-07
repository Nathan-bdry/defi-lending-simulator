package com.defi.lending.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Debt of one asset inside a position. Interest is capitalized into {@code outstanding},
 * so the field is the token amount currently owed, not the original principal.
 */
public final class Loan {

    private final String assetSymbol;
    private final LocalDateTime openedAt;
    private BigDecimal outstanding;

    private Loan(Builder builder) {
        this.assetSymbol = builder.assetSymbol;
        this.openedAt = builder.openedAt;
        this.outstanding = builder.outstanding;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getAssetSymbol() {
        return assetSymbol;
    }

    public LocalDateTime getOpenedAt() {
        return openedAt;
    }

    public BigDecimal getOutstanding() {
        return outstanding;
    }

    public void capitalize(BigDecimal factor) {
        // TODO: reject a null factor or a factor strictly below 1 (interest never shrinks a debt).
        // A factor of exactly 1 leaves the balance unchanged.
        // Otherwise set outstanding to outstanding * factor, rounded with Precision.
        throw new UnsupportedOperationException("Loan.capitalize");
    }

    public void increase(BigDecimal amount) {
        // TODO: reject a null or non-positive amount. Add it to outstanding.
        throw new UnsupportedOperationException("Loan.increase");
    }

    public void decrease(BigDecimal amount) {
        // TODO: reject a null or non-positive amount, or an amount above outstanding.
        // Subtract it. A repayment that clears the debt leaves outstanding at zero;
        // the position, not the loan, decides when to drop the entry.
        throw new UnsupportedOperationException("Loan.decrease");
    }

    /** Deep copy so a health-factor preview cannot mutate the live debt. */
    public Builder toBuilder() {
        // TODO: return a builder with the same symbol, openedAt, and outstanding value (not this instance).
        throw new UnsupportedOperationException("Loan.toBuilder");
    }

    /**
     * Assembles a loan before it is attached to a position.
     * A new borrow starts here with the borrowed token amount; later interest goes through {@link Loan#capitalize}.
     */
    public static final class Builder {

        private String assetSymbol;
        private LocalDateTime openedAt;
        private BigDecimal outstanding;

        private Builder() {
        }

        public Builder assetSymbol(String assetSymbol) {
            // DONE: reject a blank symbol before storing it.
            if (assetSymbol == null || assetSymbol.isBlank()) {
                throw new IllegalArgumentException("Asset symbol cannot be blank");
            }
            this.assetSymbol = assetSymbol;
            return this;
        }

        public Builder openedAt(LocalDateTime openedAt) {
            // DONE: reject null before storing it.
            if (openedAt == null) {
                throw new IllegalArgumentException("Opened at cannot be null");
            }
            this.openedAt = openedAt;
            return this;
        }

        public Builder outstanding(BigDecimal outstanding) {
            // DONE: reject a null or negative amount before storing it. Zero is a repaid loan and is allowed.
            if (outstanding == null || outstanding.compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("Outstanding cannot be null or negative");
            }
            this.outstanding = outstanding;
            return this;
        }

        public Loan build() {
            // DONE: reject a missing symbol, openedAt, or outstanding (IllegalStateException).
            // Those checks belong here, once, so every construction path goes through them.
            if (assetSymbol == null || assetSymbol.isBlank()) {
                throw new IllegalStateException("Asset symbol is missing");
            }
            if (openedAt == null) {
                throw new IllegalStateException("Opened at is missing");
            }
            if (outstanding == null || outstanding.compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalStateException("Outstanding is missing");
            }
            return new Loan(this);
        }
    }
}
