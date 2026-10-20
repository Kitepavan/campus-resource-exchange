package com.campus.exchange.controller;

import com.campus.exchange.exception.AppException;
import com.campus.exchange.model.Transaction;
import com.campus.exchange.service.ReviewService;
import com.campus.exchange.service.TransactionService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@WebServlet("/transactions/*")
public class TransactionServlet extends BaseServlet {

    private final TransactionService transactionService = new TransactionService();
    private final ReviewService reviewService = new ReviewService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String path = request.getPathInfo() == null ? "/" : request.getPathInfo();
        switch (path) {
            case "/", "" -> list(request, response);
            case "/view" -> view(request, response);
            default -> response.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String path = request.getPathInfo() == null ? "/" : request.getPathInfo();
        int id;
        try {
            id = intParam(request, "id");
            switch (path) {
                case "/complete" -> {
                    transactionService.completeHandover(id, currentUserId(request));
                    success(request, "Handover completed.");
                }
                case "/lend" -> {
                    transactionService.markLent(id, currentUserId(request));
                    success(request, "Loan started. The resource is now marked as lent.");
                }
                case "/return" -> {
                    transactionService.markReturned(id, currentUserId(request));
                    success(request, "Return recorded. The resource is available again.");
                }
                default -> {
                    response.sendError(HttpServletResponse.SC_NOT_FOUND);
                    return;
                }
            }
        } catch (AppException e) {
            error(request, e.getMessage());
        }
        redirect(request, response, "/transactions");
    }

    private void list(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setAttribute("transactions",
                transactionService.listForUser(currentUserId(request)));
        render(request, response, "transactions/list.jsp");
    }

    private void view(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            int id = intParam(request, "id");
            int userId = currentUserId(request);
            Transaction transaction = transactionService.getForUser(id, userId);
            request.setAttribute("transaction", transaction);
            request.setAttribute("review", reviewService.findByTransaction(id));
            request.setAttribute("canReview", transaction.isCompleted()
                    && transaction.getRequesterId() == userId
                    && reviewService.findByTransaction(id) == null);
            render(request, response, "transactions/view.jsp");
        } catch (AppException e) {
            error(request, e.getMessage());
            redirect(request, response, "/transactions");
        }
    }
}
