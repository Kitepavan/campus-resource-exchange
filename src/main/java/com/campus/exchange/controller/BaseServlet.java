package com.campus.exchange.controller;

import com.campus.exchange.exception.AccessDeniedException;
import com.campus.exchange.exception.ValidationException;
import com.campus.exchange.model.User;
import com.campus.exchange.util.Flash;
import com.campus.exchange.util.ValidationUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.math.BigDecimal;

/**
 * Shared servlet helpers: views, redirects, flash messages, and parameter parsing.
 */
public abstract class BaseServlet extends HttpServlet {

    protected static final String VIEW_PREFIX = "/WEB-INF/views/";

    protected void render(HttpServletRequest request, HttpServletResponse response,
                          String view) throws ServletException, IOException {
        response.setCharacterEncoding("UTF-8");
        request.getRequestDispatcher(VIEW_PREFIX + view).forward(request, response);
    }

    protected void redirect(HttpServletRequest request, HttpServletResponse response,
                            String path) throws IOException {
        response.sendRedirect(request.getContextPath() + path);
    }

    protected void success(HttpServletRequest request, String message) {
        Flash.success(request.getSession(), message);
    }

    protected void error(HttpServletRequest request, String message) {
        Flash.error(request.getSession(), message);
    }

    protected User currentUser(HttpServletRequest request) {
        Object attribute = request.getAttribute("currentUser");
        if (attribute instanceof User user) {
            return user;
        }
        throw new AccessDeniedException("Please log in to continue.");
    }

    protected int currentUserId(HttpServletRequest request) {
        return currentUser(request).getUserId();
    }

    protected String param(HttpServletRequest request, String name) {
        return request.getParameter(name);
    }

    protected String trimmedParam(HttpServletRequest request, String name) {
        String value = request.getParameter(name);
        return value == null ? null : value.trim();
    }

    protected int intParam(HttpServletRequest request, String name) {
        return ValidationUtil.parseInt(request.getParameter(name), name);
    }

    protected Integer optionalIntParam(HttpServletRequest request, String name) {
        return ValidationUtil.parseIntOrNull(request.getParameter(name), name);
    }

    protected BigDecimal decimalParam(HttpServletRequest request, String name) {
        return ValidationUtil.parseDecimal(request.getParameter(name), name);
    }

    protected <E extends Enum<E>> E enumParam(HttpServletRequest request, String name,
                                               Class<E> type) {
        String value = request.getParameter(name);
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Enum.valueOf(type, value.trim());
        } catch (IllegalArgumentException e) {
            throw new ValidationException("Invalid " + name + ".");
        }
    }

    protected boolean isPost(HttpServletRequest request) {
        return "POST".equalsIgnoreCase(request.getMethod());
    }
}
