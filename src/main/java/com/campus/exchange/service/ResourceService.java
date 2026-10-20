package com.campus.exchange.service;

import com.campus.exchange.dao.ResourceDAO;
import com.campus.exchange.dao.ResourceSearchCriteria;
import com.campus.exchange.exception.AccessDeniedException;
import com.campus.exchange.exception.AppException;
import com.campus.exchange.exception.NotFoundException;
import com.campus.exchange.exception.ValidationException;
import com.campus.exchange.model.Book;
import com.campus.exchange.model.LabEquipment;
import com.campus.exchange.model.Lendable;
import com.campus.exchange.model.Resource;
import com.campus.exchange.model.Sellable;
import com.campus.exchange.model.Student;
import com.campus.exchange.model.StudyMaterial;
import com.campus.exchange.model.User;
import com.campus.exchange.model.enums.ExchangeType;
import com.campus.exchange.model.enums.ResourceStatus;
import com.campus.exchange.model.enums.ResourceType;
import com.campus.exchange.util.DbErrors;
import com.campus.exchange.util.UploadUtil;
import com.campus.exchange.util.ValidationUtil;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;
import java.util.logging.Logger;

/**
 * Listing validation and ownership rules. Exchange-specific fields are
 * normalized here so the database never receives contradictory values.
 */
public class ResourceService {

    private static final Logger LOG = Logger.getLogger(ResourceService.class.getName());
    private static final BigDecimal MAX_PRICE = new BigDecimal("1000000.00");

    private final ResourceDAO resourceDAO;

    public ResourceService() {
        this(new ResourceDAO());
    }

    public ResourceService(ResourceDAO resourceDAO) {
        this.resourceDAO = resourceDAO;
    }

    public static Resource newResource(ResourceType type) {
        return switch (type) {
            case BOOK -> new Book();
            case LAB_EQUIPMENT -> new LabEquipment();
            case STUDY_MATERIAL -> new StudyMaterial();
        };
    }

    public Resource create(Resource resource, User owner) {
        requireVerifiedStudent(owner);
        resource.setOwnerId(owner.getUserId());
        resource.setStatus(ResourceStatus.AVAILABLE);
        validate(resource);
        try {
            resourceDAO.insert(resource);
            return resource;
        } catch (SQLException e) {
            throw DbErrors.wrap(LOG, "Creating resource", e);
        }
    }

    public Resource update(Resource resource, User actor) {
        requireVerifiedStudent(actor);
        Resource existing = getRequired(resource.getResourceId());
        if (existing.getOwnerId() != actor.getUserId()) {
            throw new AccessDeniedException("You can only edit your own listings.");
        }
        if (existing.isReserved() || existing.isLent()) {
            throw new ValidationException(
                    "A reserved or lent listing cannot be edited until the exchange is complete.");
        }
        resource.setOwnerId(existing.getOwnerId());
        resource.setStatus(existing.getStatus());
        validate(resource);
        try {
            if (!resourceDAO.update(resource)) {
                throw new AccessDeniedException("You can only edit your own listings.");
            }
            return resource;
        } catch (SQLException e) {
            throw DbErrors.wrap(LOG, "Updating resource", e);
        }
    }

    public void deactivate(int resourceId, User actor) {
        requireVerifiedStudent(actor);
        Resource existing = getRequired(resourceId);
        if (existing.getOwnerId() != actor.getUserId()) {
            throw new AccessDeniedException("You can only deactivate your own listings.");
        }
        if (existing.isReserved() || existing.isLent()) {
            throw new ValidationException(
                    "A reserved or lent listing cannot be deactivated.");
        }
        try {
            if (!resourceDAO.updateStatus(resourceId, ResourceStatus.INACTIVE)) {
                throw new NotFoundException("Resource not found.");
            }
        } catch (SQLException e) {
            throw DbErrors.wrap(LOG, "Deactivating resource", e);
        }
    }

    public List<Resource> search(ResourceSearchCriteria criteria) {
        try {
            return resourceDAO.search(criteria);
        } catch (SQLException e) {
            throw DbErrors.wrap(LOG, "Searching resources", e);
        }
    }

    public List<Resource> findByOwner(int ownerId) {
        try {
            return resourceDAO.findByOwner(ownerId);
        } catch (SQLException e) {
            throw DbErrors.wrap(LOG, "Loading owner resources", e);
        }
    }

    public List<String> categories() {
        try {
            return resourceDAO.findDistinctCategories();
        } catch (SQLException e) {
            throw DbErrors.wrap(LOG, "Loading categories", e);
        }
    }

    public Resource getRequired(int resourceId) {
        try {
            Resource resource = resourceDAO.findById(resourceId);
            if (resource == null) {
                throw new NotFoundException("Resource not found.");
            }
            return resource;
        } catch (SQLException e) {
            throw DbErrors.wrap(LOG, "Loading resource", e);
        }
    }

    public boolean isOwner(Resource resource, User user) {
        return resource != null && user != null
                && resource.getOwnerId() == user.getUserId();
    }

    private void requireVerifiedStudent(User user) {
        if (!(user instanceof Student student) || !student.canListResources()) {
            throw new AccessDeniedException(
                    "Only verified, active students can manage listings.");
        }
    }

    /**
     * Validates and normalizes exchange-specific fields. Public and static so
     * the rules can be tested without a database.
     */
    public static void validate(Resource resource) {
        if (resource == null) {
            throw new ValidationException("Resource data is required.");
        }
        resource.setTitle(ValidationUtil.length(resource.getTitle(), "Title", 3, 150));
        resource.setDescription(ValidationUtil.length(
                resource.getDescription(), "Description", 10, 5000));
        resource.setCategory(ValidationUtil.length(
                resource.getCategory(), "Category", 2, 80));
        resource.setPickupLocation(ValidationUtil.length(
                resource.getPickupLocation(), "Pickup location", 3, 150));
        resource.setCourseCode(ValidationUtil.courseCode(resource.getCourseCode()));
        resource.setLendingTerms(ValidationUtil.optional(
                resource.getLendingTerms(), 500, "Lending terms"));

        if (resource.getResourceType() == null) {
            throw new ValidationException("Resource type is required.");
        }
        if (resource.getCondition() == null) {
            throw new ValidationException("Condition is required.");
        }
        if (resource.getExchangeType() == null) {
            throw new ValidationException("Exchange type is required.");
        }
        if (resource.getStatus() == null) {
            resource.setStatus(ResourceStatus.AVAILABLE);
        }
        if (resource.getImagePath() != null && !resource.getImagePath().isBlank()
                && !UploadUtil.isSafeImageName(resource.getImagePath())) {
            throw new ValidationException("Invalid image reference.");
        }

        ExchangeType exchangeType = resource.getExchangeType();
        switch (exchangeType) {
            case SELL -> {
                if (!(resource instanceof Sellable)) {
                    throw new ValidationException("This resource type cannot be sold.");
                }
                BigDecimal price = ValidationUtil.positive(resource.getPrice(), "Price");
                if (price.compareTo(MAX_PRICE) > 0) {
                    throw new ValidationException("Price must be 1,000,000 or less.");
                }
                resource.setPrice(price);
                resource.setLendingTerms(null);
            }
            case LEND -> {
                if (!(resource instanceof Lendable)) {
                    throw new ValidationException("This resource type cannot be lent.");
                }
                resource.setPrice(null);
                String terms = ValidationUtil.length(
                        resource.getLendingTerms(), "Lending terms", 3, 500);
                resource.setLendingTerms(terms);
            }
            case DONATE -> {
                if (resource.getPrice() != null && resource.getPrice().signum() < 0) {
                    throw new ValidationException("Donated resources cannot have a price.");
                }
                resource.setPrice(null);
                resource.setLendingTerms(null);
            }
        }
    }
}
