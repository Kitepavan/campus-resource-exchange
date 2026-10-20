package com.campus.exchange.controller;

import com.campus.exchange.model.Resource;
import com.campus.exchange.model.ResourceRequest;
import com.campus.exchange.model.Transaction;
import com.campus.exchange.model.User;
import com.campus.exchange.service.RequestService;
import com.campus.exchange.service.ResourceService;
import com.campus.exchange.service.TransactionService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.List;

@WebServlet("/dashboard")
public class DashboardServlet extends BaseServlet {

    private final ResourceService resourceService = new ResourceService();
    private final RequestService requestService = new RequestService();
    private final TransactionService transactionService = new TransactionService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        User user = currentUser(request);
        if (user.isAdmin()) {
            redirect(request, response, "/admin");
            return;
        }

        List<Resource> listings = resourceService.findByOwner(user.getUserId());
        List<ResourceRequest> outgoing = requestService.findOutgoing(user.getUserId());
        List<ResourceRequest> incoming = requestService.findIncoming(user.getUserId());
        List<Transaction> transactions = transactionService.listForUser(user.getUserId());

        long pendingOutgoing = outgoing.stream()
                .filter(ResourceRequest::isPending).count();
        long pendingIncoming = incoming.stream()
                .filter(ResourceRequest::isPending).count();
        long activeTransactions = transactions.stream()
                .filter(tx -> !tx.isCompleted()).count();

        request.setAttribute("listingCount", listings.size());
        request.setAttribute("pendingOutgoing", pendingOutgoing);
        request.setAttribute("pendingIncoming", pendingIncoming);
        request.setAttribute("activeTransactions", activeTransactions);
        request.setAttribute("outgoing", outgoing);
        request.setAttribute("incoming", incoming);
        request.setAttribute("transactions", transactions);
        render(request, response, "student/dashboard.jsp");
    }
}
