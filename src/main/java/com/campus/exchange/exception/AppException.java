package com.campus.exchange.exception;

/**
 * Base class for expected application failures. The message is safe to show
 * to the user; internal details belong in the server log.
 */
public class AppException extends RuntimeException {

    public AppException(String message) {
        super(message);
    }

    public AppException(String message, Throwable cause) {
        super(message, cause);
    }
}
