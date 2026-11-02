package com.defi.lending.exception;

/**
 * A borrow or withdraw would leave the position able to take on more debt than its collateral allows,
 * or would push the health factor below 1.
 * Thrown only after prices and balances are known; a missing price is an {@link IllegalStateException}.
 */
public class InsufficientCollateralException extends RuntimeException {

    public InsufficientCollateralException(String message) {
        super(message);
    }
}
