package com.defi.lending.model;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Accounts and their positions, keyed by account id, in file order.
 * The position is stored beside the account so a service can resolve either from the same id.
 */
public final class Portfolio {

    private final Map<String, Account> accounts = new LinkedHashMap<>();
    private final Map<String, Position> positions = new LinkedHashMap<>();

    public void add(Account account, Position position) {
        Objects.requireNonNull(account, "account");
        Objects.requireNonNull(position, "position");
        if (position.getAccount() == null || !Objects.equals(account.getId(), position.getAccount().getId())) {
            throw new IllegalArgumentException("position does not belong to " + account.getId());
        }
        if (accounts.containsKey(account.getId())) {
            throw new IllegalStateException("duplicate account " + account.getId());
        }
        accounts.put(account.getId(), account);
        positions.put(account.getId(), position);
    }

    public Optional<Account> findAccount(String accountId) {
        return Optional.ofNullable(accounts.get(accountId));
    }

    public Optional<Position> findPosition(String accountId) {
        return Optional.ofNullable(positions.get(accountId));
    }

    public Collection<Position> positions() {
        return List.copyOf(positions.values());
    }

    /**
     * The account that may liquidate, when the file names exactly one.
     * Empty means the file named none or more than one; startup treats that as a broken scenario.
     */
    public Optional<Account> liquidator() {
        List<Account> found = accounts.values().stream()
                .filter(account -> account.getRole() == AccountRole.LIQUIDATOR)
                .toList();
        if (found.size() != 1) {
            return Optional.empty();
        }
        return Optional.of(found.get(0));
    }
}
