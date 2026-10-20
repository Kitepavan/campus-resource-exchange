package com.campus.exchange.util;

import jakarta.servlet.http.HttpSession;

/**
 * One-shot flash messages stored in the session and rendered by messages.jspf.
 */
public final class Flash {

    public static final String SUCCESS = "flashSuccess";
    public static final String ERROR = "flashError";

    private Flash() {
    }

    public static void success(HttpSession session, String message) {
        session.setAttribute(SUCCESS, message);
    }

    public static void error(HttpSession session, String message) {
        session.setAttribute(ERROR, message);
    }
}
