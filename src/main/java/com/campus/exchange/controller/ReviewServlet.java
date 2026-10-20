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

@WebServlet("/reviews/*")
public class ReviewServlet extends BaseServlet {

    private final ReviewService reviewService = new ReviewService();
    private final TransactionService transactionService = new TransactionService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String path = request.getPathInfo() == null ? "/" : request.getPathInfo();
        if (!"/new".equals(path)) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        try {
            int transactionId = intParam(request, "transactionId");
            Transaction transaction = transactionService.getForUser(
                    transactionId, currentUserId(request));
            if (transaction.getRequesterId() != currentUserId(request)
                    || !transaction.isCompleted()) {
                throw new com.campus.exchange.exception.AccessDeniedException(
                        "Only the requester can review a completed transaction.");
            }
            if (reviewService.findByTransaction(transactionId) != null) {
                throw new com.campus.exchange.exception.ConflictException(
                        "This transaction has already been reviewed.");
            }
            request.setAttribute("transaction", transaction);
            render(request, response, "reviews/form.jsp");
        } catch (AppException e) {
            error(request, e.getMessage());
            redirect(request, response, "/transactions");
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String path = request.getPathInfo() == null ? "/" : request.getPathInfo();
        if (!"/new".equals(path)) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        try {
            int transactionId = intParam(request, "transactionId");
            int rating = intParam(request, "rating");
            reviewService.createReview(transactionId, currentUserId(request), rating,
                    request.getParameter("comment"));
            success(request, "Thank you. Your review was saved.");
            redirect(request, response, "/transactions/view?id=" + transactionId);
        } catch (AppException e) {
            error(request, e.getMessage());
            redirect(request, response, "/transactions");
        }
    }
}
