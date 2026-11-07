package com.defi.lending.model;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * A person or desk that holds a wallet outside the pool and, separately, a {@link Position} inside it.
 * The wallet is what a liquidator spends and what a borrower receives. Supplied collateral is not in the wallet.
 */
public final class Account {

    private final String id;
    private final String displayName;
    private final AccountRole role;
    private final Map<String, BigDecimal> wallet = new LinkedHashMap<>();

    public Account(String id, String displayName, AccountRole role) {
        // DONE: reject a blank id, a blank displayName, or a null role (IllegalArgumentException).
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Id cannot be null or empty");
        }
        if (displayName == null || displayName.isBlank()) {
            throw new IllegalArgumentException("Display name cannot be null or empty");
        }
        if (role == null) {
            throw new IllegalArgumentException("Role cannot be null");
        }
        this.id = id;
        this.displayName = displayName;
        this.role = role;
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public AccountRole getRole() {
        return role;
    }

    /** Zero when the account has never held the asset. Absence is not an error. */
    public BigDecimal getWalletBalance(String symbol) {
        return wallet.getOrDefault(symbol, BigDecimal.ZERO);
    }

    public Set<String> getWalletSymbols() {
        return Collections.unmodifiableSet(wallet.keySet());
    }

    public void credit(String symbol, BigDecimal amount) {
        // DONE: reject a blank symbol or a non-positive amount.
        // Add the amount to the current balance of that symbol.
        if (symbol == null || symbol.isBlank()) {
            throw new IllegalArgumentException("Symbol cannot be null or empty");
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount cannot be null or non-positive");
        }
        wallet.put(symbol, wallet.getOrDefault(symbol, BigDecimal.ZERO).add(amount));
    }

    public void debit(String symbol, BigDecimal amount) {
        // DONE: reject a blank symbol, a non-positive amount, or an amount above getWalletBalance(symbol).
        // Spending coins the account does not hold is an IllegalArgumentException, not a pool-liquidity failure.
        if (symbol == null || symbol.isBlank()) {
            throw new IllegalArgumentException("Symbol cannot be null or empty");
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount cannot be null or non-positive");
        }
        if (amount.compareTo(getWalletBalance(symbol)) > 0) {
            throw new IllegalArgumentException("Amount cannot be greater than the wallet balance");
        }
        wallet.put(symbol, wallet.getOrDefault(symbol, BigDecimal.ZERO).subtract(amount));
    }

    @Override
    public boolean equals(Object other) {
        // The id is assigned by the data file and does not change. The display name is not part of identity.
        if (this == other) {
            return true;
        }
        if (!(other instanceof Account account)) {
            return false;
        }
        return Objects.equals(id, account.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return id;
    }
}
