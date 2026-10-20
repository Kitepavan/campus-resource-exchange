package com.campus.exchange.controller;

import com.campus.exchange.exception.AppException;
import com.campus.exchange.model.User;
import com.campus.exchange.service.AuthService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@WebServlet("/profile")
public class ProfileServlet extends BaseServlet {

    private final AuthService authService = new AuthService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setAttribute("user", currentUser(request));
        render(request, response, "profile.jsp");
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        User user = currentUser(request);
        try {
            authService.updateProfile(user.getUserId(),
                    request.getParameter("fullName"),
                    request.getParameter("department"),
                    optionalIntParam(request, "yearOfStudy"));
            User refreshed = authService.findById(user.getUserId());
            request.getSession().setAttribute("displayName", refreshed.getFullName());
            success(request, "Profile updated.");
            redirect(request, response, "/profile");
        } catch (AppException e) {
            request.setAttribute("error", e.getMessage());
            request.setAttribute("user", user);
            render(request, response, "profile.jsp");
        }
    }
}
