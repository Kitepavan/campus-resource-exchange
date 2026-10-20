package com.campus.exchange.util;

/**
 * Small utility for creating an administrator password hash without exposing
 * credentials in the repository.
 *
 * Usage:
 *   java -cp target/classes:<path-to-jbcrypt.jar> \
 *        com.campus.exchange.util.PasswordTool 'YourStrongPassword1'
 */
public final class PasswordTool {

    private PasswordTool() {
    }

    public static void main(String[] args) {
        String password = args.length > 0 ? args[0] : System.getenv("ADMIN_PASSWORD");
        if (password == null || password.isBlank()) {
            System.err.println("Provide a password as the first argument "
                    + "or set ADMIN_PASSWORD.");
            return;
        }
        String hash = PasswordUtil.hash(password);
        System.out.println(hash);
        System.out.println();
        System.out.println("SQL example:");
        System.out.println("INSERT INTO users (full_name, email, password_hash, role, "
                + "verification_status) VALUES");
        System.out.println("('Campus Administrator', 'admin@example.edu', '" + hash
                + "', 'ADMIN', 'VERIFIED');");
    }
}
