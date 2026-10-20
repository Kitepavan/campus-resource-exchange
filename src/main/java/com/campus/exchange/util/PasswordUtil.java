package com.campus.exchange.util;

import org.mindrot.jbcrypt.BCrypt;

/**
 * BCrypt password hashing. Work factor 12 is intentionally slower than the
 * library default while remaining practical for a campus application.
 */
public final class PasswordUtil {

    private static final int WORK_FACTOR = 12;

    private PasswordUtil() {
    }

    public static String hash(String rawPassword) {
        return BCrypt.hashpw(rawPassword, BCrypt.gensalt(WORK_FACTOR));
    }

    public static boolean matches(String rawPassword, String storedHash) {
        if (rawPassword == null || storedHash == null) {
            return false;
        }
        try {
            return BCrypt.checkpw(rawPassword, storedHash);
        } catch (IllegalArgumentException e) {
            // Corrupt or non-BCrypt hash in the database.
            return false;
        }
    }
}
