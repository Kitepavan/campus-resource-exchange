package com.campus.exchange.filter;

import com.campus.exchange.dao.UserDAO;
import com.campus.exchange.model.User;
import com.campus.exchange.util.CsrfUtil;
import com.campus.exchange.util.Flash;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Central authentication, authorization, CSRF, and basic security headers.
 * Service methods still re-check ownership and roles.
 */
@WebFilter(urlPatterns = "/*")
public class SecurityFilter implements Filter {

    private static final Logger LOG = Logger.getLogger(SecurityFilter.class.getName());

    private static final Set<String> STUDENT_PREFIXES = Set.of(
            "/resources/new", "/resources/edit", "/resources/delete", "/resources/mine",
            "/requests/", "/transactions/", "/reviews/");

    private final UserDAO userDAO = new UserDAO();

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse,
                         FilterChain chain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;

        request.setCharacterEncoding("UTF-8");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("X-Frame-Options", "DENY");
        response.setHeader("Referrer-Policy", "same-origin");

        String path = request.getRequestURI().substring(request.getContextPath().length());

        // Assets need no session, user lookup, or database round trip.
        if (isStatic(path)) {
            chain.doFilter(request, response);
            return;
        }

        HttpSession session = request.getSession(true);
        CsrfUtil.ensureToken(session);

        User user = loadUser(request, session);
        if (user != null) {
            request.setAttribute("currentUser", user);
            session.setAttribute("userId", user.getUserId());
            session.setAttribute("role", user.getRole().name());
            session.setAttribute("displayName", user.getFullName());
            session.setAttribute("verified", user.isVerified());
        }

        if (isPost(request) && !isMultipart(request) && !CsrfUtil.validate(request)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN,
                    "The form expired. Please refresh the page and try again.");
            return;
        }

        if (isPublic(path)) {
            chain.doFilter(request, response);
            return;
        }

        if (user == null) {
            session.setAttribute(Flash.ERROR, "Please log in to continue.");
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }

        if (path.startsWith("/admin") && !user.isAdmin()) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN,
                    "Administrator access is required.");
            return;
        }

        if (requiresStudent(path) && !user.isStudent()) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN,
                    "This area is for student accounts.");
            return;
        }

        chain.doFilter(request, response);
    }

    private User loadUser(HttpServletRequest request, HttpSession session) {
        Object value = session.getAttribute("userId");
        if (!(value instanceof Integer userId)) {
            return null;
        }
        try {
            User user = userDAO.findById(userId);
            if (user == null || !user.isActive()) {
                session.invalidate();
                return null;
            }
            return user;
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "Loading session user", e);
            return null;
        }
    }

    private boolean isStatic(String path) {
        return path.startsWith("/assets/")
                || path.startsWith("/images/")
                || path.equals("/favicon.ico");
    }

    private boolean isPublic(String path) {
        return path.isEmpty()
                || path.equals("/")
                || path.equals("/home")
                || path.equals("/login")
                || path.equals("/register")
                || path.equals("/logout")
                || path.equals("/resources")
                || path.equals("/resources/")
                || path.equals("/resources/view");
    }

    private boolean requiresStudent(String path) {
        for (String prefix : STUDENT_PREFIXES) {
            if (path.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    private boolean isPost(HttpServletRequest request) {
        return "POST".equalsIgnoreCase(request.getMethod());
    }

    private boolean isMultipart(HttpServletRequest request) {
        String contentType = request.getContentType();
        return contentType != null
                && contentType.toLowerCase().startsWith("multipart/");
    }
}
