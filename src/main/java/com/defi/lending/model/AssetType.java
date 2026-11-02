package com.defi.lending.model;

/**
 * Which valuation policy an {@link Asset} uses.
 * The text in {@code markets.csv} is the enum name.
 */
public enum AssetType {
    STABLECOIN,
    VOLATILE
}
