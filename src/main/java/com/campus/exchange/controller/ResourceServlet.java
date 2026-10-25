package com.campus.exchange.controller;

import com.campus.exchange.dao.ResourceSearchCriteria;
import com.campus.exchange.exception.AccessDeniedException;
import com.campus.exchange.exception.AppException;
import com.campus.exchange.exception.ValidationException;
import com.campus.exchange.model.Resource;
import com.campus.exchange.model.Student;
import com.campus.exchange.model.User;
import com.campus.exchange.model.enums.Condition;
import com.campus.exchange.model.enums.ExchangeType;
import com.campus.exchange.model.enums.ResourceType;
import com.campus.exchange.service.AuthService;
import com.campus.exchange.service.ResourceService;
import com.campus.exchange.service.ReviewService;
import com.campus.exchange.util.AppConfig;
import com.campus.exchange.util.CsrfUtil;
import com.campus.exchange.util.UploadUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;

@WebServlet("/resources/*")
@MultipartConfig(maxFileSize = 5L * 1024 * 1024, maxRequestSize = 6L * 1024 * 1024,
        fileSizeThreshold = 1024 * 1024)
public class ResourceServlet extends BaseServlet {

    private final ResourceService resourceService = new ResourceService();
    private final AuthService authService = new AuthService();
    private final ReviewService reviewService = new ReviewService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String path = request.getPathInfo() == null ? "/" : request.getPathInfo();
        switch (path) {
            case "/", "" -> list(request, response);
            case "/view" -> view(request, response);
            case "/new" -> newForm(request, response);
            case "/edit" -> editForm(request, response);
            case "/mine" -> mine(request, response);
            default -> response.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String path = request.getPathInfo() == null ? "/" : request.getPathInfo();
        switch (path) {
            case "/new" -> save(request, response, null);
            case "/edit" -> save(request, response, intParam(request, "id"));
            case "/delete" -> deactivate(request, response);
            default -> response.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    private void list(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        ResourceSearchCriteria criteria = new ResourceSearchCriteria();
        criteria.setKeyword(trimmedParam(request, "keyword"));
        criteria.setCategory(trimmedParam(request, "category"));
        criteria.setCourseCode(trimmedParam(request, "courseCode"));
        criteria.setPickupLocation(trimmedParam(request, "pickupLocation"));
        criteria.setResourceType(safeEnum(request, "resourceType", ResourceType.class));
        criteria.setExchangeType(safeEnum(request, "exchangeType", ExchangeType.class));
        criteria.setCondition(safeEnum(request, "condition", Condition.class));
        criteria.setMinPrice(safeDecimal(request, "minPrice"));
        criteria.setMaxPrice(safeDecimal(request, "maxPrice"));
        criteria.setSort(trimmedParam(request, "sort"));

        List<Resource> resources = resourceService.search(criteria);
        request.setAttribute("resources", resources);
        request.setAttribute("criteria", criteria);
        request.setAttribute("categories", resourceService.categories());
        setFormOptions(request);
        render(request, response, "resources/list.jsp");
    }

    private void view(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        int id = intParam(request, "id");
        Resource resource;
        try {
            resource = resourceService.getRequired(id);
        } catch (AppException e) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, e.getMessage());
            return;
        }
        User owner = authService.findById(resource.getOwnerId());
        request.setAttribute("resource", resource);
        request.setAttribute("owner", owner);
        request.setAttribute("reviews", reviewService.findForOwner(resource.getOwnerId()));
        render(request, response, "resources/view.jsp");
    }

    private void newForm(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            requireVerified(currentUser(request));
        } catch (AppException e) {
            error(request, e.getMessage());
            redirect(request, response, "/dashboard");
            return;
        }
        request.setAttribute("resource", null);
        request.setAttribute("formAction", "/resources/new");
        setFormOptions(request);
        render(request, response, "resources/form.jsp");
    }

    private void editForm(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            int id = intParam(request, "id");
            Resource resource = resourceService.getRequired(id);
            if (!resourceService.isOwner(resource, currentUser(request))) {
                throw new AccessDeniedException("You can only edit your own listings.");
            }
            request.setAttribute("resource", resource);
            request.setAttribute("formAction", "/resources/edit?id=" + id);
            setFormOptions(request);
            render(request, response, "resources/form.jsp");
        } catch (AppException e) {
            error(request, e.getMessage());
            redirect(request, response, "/resources/mine");
        }
    }

    private void mine(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        List<Resource> listings = resourceService.findByOwner(currentUserId(request));
        request.setAttribute("resources", listings);
        render(request, response, "resources/my-listings.jsp");
    }

    private void save(HttpServletRequest request, HttpServletResponse response, Integer editId)
            throws ServletException, IOException {
        if (!CsrfUtil.validate(request)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN,
                    "The form expired. Please refresh the page and try again.");
            return;
        }

        Resource resource = null;
        String savedImage = null;
        String oldImage = null;
        try {
            User user = currentUser(request);
            requireVerified(user);

            ResourceType type = enumParam(request, "resourceType", ResourceType.class);
            if (type == null) {
                throw new ValidationException("Resource type is required.");
            }
            resource = ResourceService.newResource(type);
            resource.setResourceType(type);
            resource.setTitle(request.getParameter("title"));
            resource.setDescription(request.getParameter("description"));
            resource.setCategory(request.getParameter("category"));
            resource.setCondition(enumParam(request, "condition", Condition.class));
            resource.setExchangeType(enumParam(request, "exchangeType", ExchangeType.class));
            resource.setPrice(decimalParam(request, "price"));
            resource.setLendingTerms(request.getParameter("lendingTerms"));
            resource.setCourseCode(request.getParameter("courseCode"));
            resource.setPickupLocation(request.getParameter("pickupLocation"));
            resource.setOwnerId(user.getUserId());

            if (editId != null) {
                Resource existing = resourceService.getRequired(editId);
                if (existing.getOwnerId() != user.getUserId()) {
                    throw new AccessDeniedException("You can only edit your own listings.");
                }
                resource.setResourceId(editId);
                resource.setStatus(existing.getStatus());
                resource.setImagePath(existing.getImagePath());
                oldImage = existing.getImagePath();
            } else {
                resource.setStatus(com.campus.exchange.model.enums.ResourceStatus.AVAILABLE);
            }

            // Validate before writing the upload so invalid forms do not leave files behind.
            ResourceService.validate(resource);
            savedImage = UploadUtil.saveImage(request.getPart("image"), AppConfig.uploadDir());
            if (savedImage != null) {
                resource.setImagePath(savedImage);
            }

            if (editId == null) {
                resourceService.create(resource, user);
            } else {
                resourceService.update(resource, user);
            }

            if (savedImage != null && oldImage != null && !savedImage.equals(oldImage)) {
                UploadUtil.deleteQuietly(AppConfig.uploadDir(), oldImage);
            }
            success(request, editId == null
                    ? "Listing created successfully."
                    : "Listing updated successfully.");
            redirect(request, response, "/resources/view?id=" + resource.getResourceId());
        } catch (AppException e) {
            if (savedImage != null) {
                UploadUtil.deleteQuietly(AppConfig.uploadDir(), savedImage);
            }
            request.setAttribute("error", e.getMessage());
            request.setAttribute("resource", resource);
            request.setAttribute("formAction", editId == null
                    ? "/resources/new"
                    : "/resources/edit?id=" + editId);
            setFormOptions(request);
            render(request, response, "resources/form.jsp");
        } catch (IllegalStateException e) {
            request.setAttribute("error", "The uploaded image is too large (maximum 5 MB).");
            request.setAttribute("resource", resource);
            request.setAttribute("formAction", editId == null
                    ? "/resources/new"
                    : "/resources/edit?id=" + editId);
            setFormOptions(request);
            render(request, response, "resources/form.jsp");
        }
    }

    private void deactivate(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        try {
            resourceService.deactivate(intParam(request, "id"), currentUser(request));
            success(request, "Listing deactivated.");
        } catch (AppException e) {
            error(request, e.getMessage());
        }
        redirect(request, response, "/resources/mine");
    }

    private void requireVerified(User user) {
        if (!(user instanceof Student student) || !student.canListResources()) {
            throw new AccessDeniedException(
                    "Only verified, active students can manage listings.");
        }
    }

    private void setFormOptions(HttpServletRequest request) {
        request.setAttribute("resourceTypes", ResourceType.values());
        request.setAttribute("conditions", Condition.values());
        request.setAttribute("exchangeTypes", ExchangeType.values());
        request.setAttribute("categories", resourceService.categories());
    }

    private <E extends Enum<E>> E safeEnum(HttpServletRequest request, String name,
                                           Class<E> type) {
        try {
            return enumParam(request, name, type);
        } catch (ValidationException e) {
            return null;
        }
    }

    private BigDecimal safeDecimal(HttpServletRequest request, String name) {
        try {
            return decimalParam(request, name);
        } catch (ValidationException e) {
            return null;
        }
    }
}
