package com.campus.exchange.util;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Session-bound CSRF token. Every non-multipart POST is checked by
 * SecurityFilter; multipart forms are checked by ResourceServlet after the
 * parts have been parsed.
 */
public final class CsrfUtil {

    public static final String SESSION_KEY = "csrfToken";
    public static final String PARAM = "_csrf";

    private static final SecureRandom RANDOM = new SecureRandom();

    private CsrfUtil() {
    }

    public static String ensureToken(HttpSession session) {
        Object existing = session.getAttribute(SESSION_KEY);
        if (existing instanceof String token && !token.isBlank()) {
            return token;
        }
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        session.setAttribute(SESSION_KEY, token);
        return token;
    }

    public static boolean validate(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return false;
        }
        Object expected = session.getAttribute(SESSION_KEY);
        String supplied = request.getParameter(PARAM);
        if (!(expected instanceof String expectedToken) || supplied == null) {
            return false;
        }
        return MessageDigest.isEqual(
                expectedToken.getBytes(StandardCharsets.UTF_8),
                supplied.getBytes(StandardCharsets.UTF_8));
    }
}
