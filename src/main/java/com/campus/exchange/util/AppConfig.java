package com.campus.exchange.util;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Environment-driven configuration. Secrets are never bundled in the WAR.
 */
public final class AppConfig {

    private static final String DEFAULT_DB_URL =
            "jdbc:mysql://localhost:3306/campus_exchange"
                    + "?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true";

    private AppConfig() {
    }

    public static String dbUrl() {
        return read("DB_URL", DEFAULT_DB_URL);
    }

    public static String dbUser() {
        return read("DB_USER", "campus_app");
    }

    public static String dbPassword() {
        String value = read("DB_PASSWORD", null);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(
                    "DB_PASSWORD is not configured. Set it in the Tomcat environment.");
        }
        return value;
    }

    public static Path uploadDir() {
        return Paths.get(read("UPLOAD_DIR",
                System.getProperty("user.home") + "/campus-exchange/uploads"))
                .toAbsolutePath().normalize();
    }

    public static Path backupDir() {
        return Paths.get(read("BACKUP_DIR",
                System.getProperty("user.home") + "/campus-exchange/backups"))
                .toAbsolutePath().normalize();
    }

    private static String read(String key, String fallback) {
        String value = System.getenv(key);
        if (value == null || value.isBlank()) {
            value = System.getProperty(key);
        }
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}
