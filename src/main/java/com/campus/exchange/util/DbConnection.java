package com.campus.exchange.util;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Shared JDBC connection pool. Connections are still obtained and closed per
 * operation; closing returns them to the pool instead of dropping the socket.
 */
public final class DbConnection {

    private static volatile HikariDataSource dataSource;

    private DbConnection() {
    }

    public static Connection getConnection() throws SQLException {
        return dataSource().getConnection();
    }

    private static HikariDataSource dataSource() {
        HikariDataSource current = dataSource;
        if (current != null) {
            return current;
        }
        synchronized (DbConnection.class) {
            if (dataSource == null) {
                HikariConfig config = new HikariConfig();
                // Explicit: DriverManager cannot see a driver from a WAR classloader.
                config.setDriverClassName("com.mysql.cj.jdbc.Driver");
                config.setJdbcUrl(AppConfig.dbUrl());
                config.setUsername(AppConfig.dbUser());
                config.setPassword(AppConfig.dbPassword());
                config.setMaximumPoolSize(AppConfig.dbPoolSize());
                config.setConnectionTimeout(5_000);
                config.setPoolName("campus-exchange");
                dataSource = new HikariDataSource(config);
            }
            return dataSource;
        }
    }

    public static void close() {
        HikariDataSource current = dataSource;
        dataSource = null;
        if (current != null) {
            current.close();
        }
    }
}
