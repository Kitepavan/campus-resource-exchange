package com.campus.exchange.controller;

import com.campus.exchange.exception.AppException;
import com.campus.exchange.model.ResourceRequest;
import com.campus.exchange.service.RequestService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@WebServlet("/requests/*")
public class RequestServlet extends BaseServlet {

    private final RequestService requestService = new RequestService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String path = request.getPathInfo() == null ? "/" : request.getPathInfo();
        switch (path) {
            case "/outgoing" -> outgoing(request, response);
            case "/incoming" -> incoming(request, response);
            default -> response.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String path = request.getPathInfo() == null ? "/" : request.getPathInfo();
        try {
            switch (path) {
                case "/new" -> create(request, response);
                case "/accept" -> accept(request, response);
                case "/reject" -> reject(request, response);
                case "/cancel" -> cancel(request, response);
                default -> response.sendError(HttpServletResponse.SC_NOT_FOUND);
            }
        } catch (AppException e) {
            error(request, e.getMessage());
            redirect(request, response,
                    "/cancel".equals(path) ? "/requests/outgoing" : "/requests/incoming");
        }
    }

    private void outgoing(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setAttribute("requests",
                requestService.findOutgoing(currentUserId(request)));
        render(request, response, "requests/outgoing.jsp");
    }

    private void incoming(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setAttribute("requests",
                requestService.findIncoming(currentUserId(request)));
        render(request, response, "requests/incoming.jsp");
    }

    private void create(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        int resourceId = intParam(request, "resourceId");
        requestService.createRequest(resourceId, currentUserId(request),
                request.getParameter("message"));
        success(request, "Request sent to the owner.");
        redirect(request, response, "/resources/view?id=" + resourceId);
    }

    private void accept(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        int requestId = intParam(request, "requestId");
        int transactionId = requestService.acceptRequest(requestId, currentUserId(request));
        success(request, "Request accepted. Transaction #" + transactionId + " created.");
        redirect(request, response, "/transactions/view?id=" + transactionId);
    }

    private void reject(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        requestService.rejectRequest(intParam(request, "requestId"), currentUserId(request));
        success(request, "Request rejected.");
        redirect(request, response, "/requests/incoming");
    }

    private void cancel(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        requestService.cancelRequest(intParam(request, "requestId"), currentUserId(request));
        success(request, "Request cancelled.");
        redirect(request, response, "/requests/outgoing");
    }
}
