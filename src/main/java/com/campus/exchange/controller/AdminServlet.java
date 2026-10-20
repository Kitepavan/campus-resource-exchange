package com.campus.exchange.controller;

import com.campus.exchange.exception.AppException;
import com.campus.exchange.model.User;
import com.campus.exchange.model.enums.Role;
import com.campus.exchange.model.enums.VerificationStatus;
import com.campus.exchange.service.AdminService;
import com.campus.exchange.service.BackupService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Map;
import java.util.stream.Collectors;

@WebServlet("/admin/*")
public class AdminServlet extends BaseServlet {

    private final AdminService adminService = new AdminService();
    private final BackupService backupService = new BackupService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String path = request.getPathInfo() == null ? "/" : request.getPathInfo();
        switch (path) {
            case "/", "" -> dashboard(request, response);
            case "/users" -> users(request, response);
            case "/listings" -> listings(request, response);
            case "/activity" -> activity(request, response);
            case "/backup" -> backup(request, response);
            default -> response.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String path = request.getPathInfo() == null ? "/" : request.getPathInfo();
        try {
            switch (path) {
                case "/users/verify" -> {
                    adminService.verifyStudent(intParam(request, "userId"));
                    success(request, "Student verified.");
                }
                case "/users/reject" -> {
                    adminService.rejectStudent(intParam(request, "userId"));
                    success(request, "Student verification rejected.");
                }
                case "/users/status" -> {
                    boolean active = Boolean.parseBoolean(request.getParameter("active"));
                    adminService.setActive(intParam(request, "userId"), active);
                    success(request, active ? "Account activated." : "Account suspended.");
                }
                case "/listings/status" -> {
                    boolean deactivate = Boolean.parseBoolean(
                            request.getParameter("deactivate"));
                    adminService.moderateListing(intParam(request, "resourceId"), deactivate);
                    success(request, deactivate ? "Listing deactivated." : "Listing restored.");
                }
                case "/backup/create" -> {
                    String filename = backupService.createBackup();
                    success(request, "Backup created: " + filename);
                }
                case "/backup/restore" -> {
                    int restored = backupService.restore(request.getParameter("filename"));
                    success(request, "Backup restored. " + restored
                            + " listing(s) added; duplicates were skipped.");
                }
                default -> {
                    response.sendError(HttpServletResponse.SC_NOT_FOUND);
                    return;
                }
            }
        } catch (AppException e) {
            error(request, e.getMessage());
        }
        redirect(request, response, path.startsWith("/users") ? "/admin/users"
                : path.startsWith("/listings") ? "/admin/listings"
                : path.startsWith("/backup") ? "/admin/backup" : "/admin");
    }

    private void dashboard(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setAttribute("stats", adminService.stats());
        request.setAttribute("requests", adminService.listRequests());
        request.setAttribute("transactions", adminService.listTransactions());
        render(request, response, "admin/dashboard.jsp");
    }

    private void users(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String role = request.getParameter("role");
        String verification = request.getParameter("verification");
        request.setAttribute("users", adminService.listUsers(role, verification));
        request.setAttribute("roleFilter", role);
        request.setAttribute("verificationFilter", verification);
        request.setAttribute("roles", Role.values());
        request.setAttribute("verificationStatuses", VerificationStatus.values());
        render(request, response, "admin/users.jsp");
    }

    private void listings(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setAttribute("resources", adminService.listResources());
        Map<Integer, String> userNames = adminService.listUsers(null, null).stream()
                .collect(Collectors.toMap(User::getUserId, User::getFullName));
        request.setAttribute("userNames", userNames);
        render(request, response, "admin/listings.jsp");
    }

    private void activity(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setAttribute("requests", adminService.listRequests());
        request.setAttribute("transactions", adminService.listTransactions());
        request.setAttribute("reviews", adminService.listReviews());
        render(request, response, "admin/activity.jsp");
    }

    private void backup(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setAttribute("backups", backupService.listBackups());
        render(request, response, "admin/backup.jsp");
    }
}
