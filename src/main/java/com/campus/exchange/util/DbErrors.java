package com.campus.exchange.util;

import com.campus.exchange.exception.AppException;

import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Converts database failures into safe user-facing errors while preserving the
 * technical detail in the server log.
 */
public final class DbErrors {

    private DbErrors() {
    }

    public static AppException wrap(Logger log, String operation, SQLException error) {
        log.log(Level.SEVERE, operation, error);
        return new AppException("A database error occurred. Please try again.");
    }
}
