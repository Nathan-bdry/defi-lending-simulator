package com.defi.lending.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * One account's supplies and debts inside the protocol.
 * Supplies are collateral. The wallet on {@link Account} is not included, because coins outside the pool
 * do not back a borrow and are not seized.
 */
public final class Position {

    private final Account account;
    private final Map<String, BigDecimal> supplied;
    private final Map<String, Loan> loans;

    private Position(Account account, Map<String, BigDecimal> supplied, Map<String, Loan> loans) {
        this.account = account;
        this.supplied = new LinkedHashMap<>(supplied);
        this.loans = new LinkedHashMap<>(loans);
    }

    /** A new user: no collateral and no debt. */
    public static Position open(Account account) {
        // DONE: reject a null account.
        if (account == null) {
            throw new IllegalArgumentException("Account cannot be null");
        }
        return new Position(account, Map.of(), Map.of());
    }

    public static Builder builder(Account account) {
        // DONE: reject a null account.
        if (account == null) {
            throw new IllegalArgumentException("Account cannot be null");
        }
        return new Builder(account);
    }

    public Account getAccount() {
        return account;
    }

    /** Zero when the account does not supply that asset. */
    public BigDecimal supplyOf(String symbol) {
        return supplied.getOrDefault(symbol, BigDecimal.ZERO);
    }

    public Collection<String> suppliedSymbols() {
        return List.copyOf(supplied.keySet());
    }

    public Optional<Loan> getLoan(String symbol) {
        return Optional.ofNullable(loans.get(symbol));
    }

    /** Live loan objects, so interest accrual can capitalize them in place. */
    public Collection<Loan> getLoans() {
        return List.copyOf(loans.values());
    }

    public void addSupply(String symbol, BigDecimal amount) {
        // TODO: reject a blank symbol or a non-positive amount.
        // Add the amount to any supply already stored for the symbol.
        throw new UnsupportedOperationException("Position.addSupply");
    }

    public void reduceSupply(String symbol, BigDecimal amount) {
        // TODO: reject a blank symbol, a non-positive amount, or an amount above supplyOf(symbol).
        // Remove the map entry when the balance reaches zero so an empty supply is not collateral.
        throw new UnsupportedOperationException("Position.reduceSupply");
    }

    public void capitalizeSupply(String symbol, BigDecimal factor) {
        // TODO: when the symbol is not supplied, do nothing.
        // Otherwise reject a null factor or a factor strictly below 1, and multiply the stored supply,
        // rounded with Precision. A factor of 1 is a no-op.
        throw new UnsupportedOperationException("Position.capitalizeSupply");
    }

    /**
     * Opens a loan or adds to the one already stored for that asset.
     * The added amount is tokens just borrowed, at {@code at}.
     */
    public void increaseDebt(String symbol, BigDecimal amount, LocalDateTime at) {
        // TODO: reject a blank symbol, a non-positive amount, or a null timestamp.
        // If a loan exists, increase it. If not, build one opened at `at` with that outstanding amount.
        throw new UnsupportedOperationException("Position.increaseDebt");
    }

    public void reduceDebt(String symbol, BigDecimal amount) {
        // TODO: reject a blank symbol, a non-positive amount, or a missing loan.
        // Decrease the loan. When outstanding hits zero, remove it so the health factor sees no debt.
        throw new UnsupportedOperationException("Position.reduceDebt");
    }

    public Builder toBuilder() {
        // TODO: deep-copy supplies and loans into a new builder for this account.
        // A shallow copy would let a health-factor preview capitalize the live loan.
        throw new UnsupportedOperationException("Position.toBuilder");
    }

    /**
     * Stages a position for tests and for a health-factor preview.
     * {@link #supply} replaces the staged token amount; it does not add.
     */
    public static final class Builder {

        private final Account account;
        private final Map<String, BigDecimal> supplied = new LinkedHashMap<>();
        private final Map<String, Loan> loans = new LinkedHashMap<>();

        private Builder(Account account) {
            this.account = account;
        }

        public Builder supply(String symbol, BigDecimal tokens) {
            // DONE: reject a blank symbol or a null or negative amount.
            // Store the amount, replacing any staged supply for that symbol. Zero clears the collateral.
            if (symbol == null || symbol.isBlank()) {
                throw new IllegalArgumentException("Symbol cannot be blank");
            }
            if (tokens == null || tokens.compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("Tokens cannot be null or negative");
            }
            if (tokens.compareTo(BigDecimal.ZERO) == 0) {
                this.supplied.remove(symbol);
            } else {
                this.supplied.put(symbol, tokens);
            }
            return this;
        }

        public Builder loan(Loan loan) {
            // DONE: reject a null loan, and reject a second loan of a symbol already staged.
            // One asset has one debt line.
            if (loan == null) {
                throw new IllegalArgumentException("Loan cannot be null");
            }
            if (loans.containsKey(loan.getAssetSymbol())) {
                throw new IllegalArgumentException("Loan already staged");
            }
            this.loans.put(loan.getAssetSymbol(), loan);
            return this;
        }

        public Position build() {
            // DONE: reject a null account. Copy the staged maps so later builder calls cannot change the position.
            if (account == null) {
                throw new IllegalArgumentException("Account cannot be null");
            }
            if (supplied == null || loans == null) {
                throw new IllegalArgumentException("Supplied or loans cannot be null");
            }
            return new Position(account, supplied, loans);
        }
    }
}
