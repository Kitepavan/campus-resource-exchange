package com.campus.exchange.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Thin JDBC connection factory. Connections are short-lived and always used
 * with try-with-resources except inside explicit service transactions.
 */
public final class DbConnection {

    static {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("MySQL JDBC driver is not on the classpath", e);
        }
    }

    private DbConnection() {
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(
                AppConfig.dbUrl(), AppConfig.dbUser(), AppConfig.dbPassword());
    }
}
