package com.defi.lending.exception;

/**
 * A startup file is missing, unreadable, or has a row that cannot become a domain object.
 * Checked so {@link com.defi.lending.Application} has to decide what to tell the user
 * before any market is touched.
 */
public class DataLoadException extends Exception {

    public DataLoadException(String message) {
        super(message);
    }

    public DataLoadException(String message, Throwable cause) {
        super(message, cause);
    }
}
