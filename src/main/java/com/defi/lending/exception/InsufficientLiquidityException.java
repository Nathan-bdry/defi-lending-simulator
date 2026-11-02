package com.defi.lending.exception;

/**
 * The pool does not hold enough cash of the requested asset to pay out a borrow.
 * The caller's own wallet is not this case: spending coins the caller does not hold
 * is an {@link IllegalArgumentException}.
 */
public class InsufficientLiquidityException extends RuntimeException {

    public InsufficientLiquidityException(String message) {
        super(message);
    }
}
