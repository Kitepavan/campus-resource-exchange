package com.campus.exchange.controller;

import com.campus.exchange.exception.AppException;
import com.campus.exchange.model.Student;
import com.campus.exchange.model.User;
import com.campus.exchange.service.AuthService;
import com.campus.exchange.util.CsrfUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

@WebServlet(urlPatterns = {"/login", "/register", "/logout"})
public class AuthServlet extends BaseServlet {

    private final AuthService authService = new AuthService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        switch (request.getServletPath()) {
            case "/login" -> render(request, response, "auth/login.jsp");
            case "/register" -> render(request, response, "auth/register.jsp");
            case "/logout" -> logout(request, response);
            default -> response.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        switch (request.getServletPath()) {
            case "/login" -> login(request, response);
            case "/register" -> register(request, response);
            case "/logout" -> logout(request, response);
            default -> response.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    private void login(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            User user = authService.login(
                    request.getParameter("email"), request.getParameter("password"));
            HttpSession session = request.getSession();
            request.changeSessionId();
            session.setAttribute("userId", user.getUserId());
            session.setAttribute("role", user.getRole().name());
            session.setAttribute("displayName", user.getFullName());
            session.setAttribute("verified", user.isVerified());
            session.removeAttribute(CsrfUtil.SESSION_KEY);
            CsrfUtil.ensureToken(session);
            response.sendRedirect(request.getContextPath() + user.getDashboardPath());
        } catch (AppException e) {
            request.setAttribute("error", e.getMessage());
            render(request, response, "auth/login.jsp");
        }
    }

    private void register(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            Student student = authService.register(
                    request.getParameter("fullName"),
                    request.getParameter("email"),
                    request.getParameter("studentId"),
                    request.getParameter("password"),
                    request.getParameter("department"),
                    optionalIntParam(request, "yearOfStudy"));
            success(request, "Account created for " + student.getFullName()
                    + ". An administrator must verify it before you can list or request resources.");
            redirect(request, response, "/login");
        } catch (AppException e) {
            request.setAttribute("error", e.getMessage());
            render(request, response, "auth/register.jsp");
        }
    }

    private void logout(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        response.sendRedirect(request.getContextPath() + "/login");
    }
}
