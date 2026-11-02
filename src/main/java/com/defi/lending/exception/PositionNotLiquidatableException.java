package com.defi.lending.exception;

/**
 * Liquidation was requested for a position whose health factor is undefined or at least 1.
 * A health factor of exactly 1 is still solvent; only a value strictly below 1 may be liquidated.
 */
public class PositionNotLiquidatableException extends RuntimeException {

    public PositionNotLiquidatableException(String message) {
        super(message);
    }
}
