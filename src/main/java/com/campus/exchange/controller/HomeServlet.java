package com.campus.exchange.controller;

import com.campus.exchange.dao.ResourceSearchCriteria;
import com.campus.exchange.model.Resource;
import com.campus.exchange.service.ResourceService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.List;

@WebServlet(urlPatterns = {"/home"})
public class HomeServlet extends BaseServlet {

    private final ResourceService resourceService = new ResourceService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        List<Resource> latest = resourceService.search(new ResourceSearchCriteria());
        request.setAttribute("resources", latest);
        render(request, response, "home.jsp");
    }
}
