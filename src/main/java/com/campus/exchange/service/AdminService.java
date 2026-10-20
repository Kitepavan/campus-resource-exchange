package com.campus.exchange.service;

import com.campus.exchange.dao.ResourceDAO;
import com.campus.exchange.dao.ResourceRequestDAO;
import com.campus.exchange.dao.ReviewDAO;
import com.campus.exchange.dao.TransactionDAO;
import com.campus.exchange.dao.UserDAO;
import com.campus.exchange.exception.NotFoundException;
import com.campus.exchange.exception.ValidationException;
import com.campus.exchange.model.Resource;
import com.campus.exchange.model.ResourceRequest;
import com.campus.exchange.model.Review;
import com.campus.exchange.model.Student;
import com.campus.exchange.model.Transaction;
import com.campus.exchange.model.User;
import com.campus.exchange.model.enums.RequestStatus;
import com.campus.exchange.model.enums.ResourceStatus;
import com.campus.exchange.model.enums.TransactionStatus;
import com.campus.exchange.model.enums.VerificationStatus;
import com.campus.exchange.util.DbErrors;

import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Administrator operations: verification, suspension, moderation, statistics.
 */
public class AdminService {

    private static final Logger LOG = Logger.getLogger(AdminService.class.getName());

    private final UserDAO userDAO;
    private final ResourceDAO resourceDAO;
    private final ResourceRequestDAO requestDAO;
    private final TransactionDAO transactionDAO;
    private final ReviewDAO reviewDAO;

    public AdminService() {
        this(new UserDAO(), new ResourceDAO(), new ResourceRequestDAO(),
                new TransactionDAO(), new ReviewDAO());
    }

    public AdminService(UserDAO userDAO, ResourceDAO resourceDAO,
                        ResourceRequestDAO requestDAO, TransactionDAO transactionDAO,
                        ReviewDAO reviewDAO) {
        this.userDAO = userDAO;
        this.resourceDAO = resourceDAO;
        this.requestDAO = requestDAO;
        this.transactionDAO = transactionDAO;
        this.reviewDAO = reviewDAO;
    }

    public List<User> listUsers(String roleFilter, String verificationFilter) {
        try {
            return userDAO.findAll(roleFilter, verificationFilter);
        } catch (SQLException e) {
            throw DbErrors.wrap(LOG, "Listing users", e);
        }
    }

    public void verifyStudent(int userId) {
        Student student = requireStudent(userId);
        updateVerification(student, VerificationStatus.VERIFIED);
    }

    public void rejectStudent(int userId) {
        Student student = requireStudent(userId);
        updateVerification(student, VerificationStatus.REJECTED);
    }

    private Student requireStudent(int userId) {
        try {
            User user = userDAO.findById(userId);
            if (user == null) {
                throw new NotFoundException("User not found.");
            }
            if (!(user instanceof Student student)) {
                throw new ValidationException("Only student accounts can be verified.");
            }
            return student;
        } catch (SQLException e) {
            throw DbErrors.wrap(LOG, "Loading student", e);
        }
    }

    private void updateVerification(Student student, VerificationStatus status) {
        try {
            if (!userDAO.updateVerificationStatus(student.getUserId(), status)) {
                throw new NotFoundException("Student not found.");
            }
        } catch (SQLException e) {
            throw DbErrors.wrap(LOG, "Updating verification", e);
        }
    }

    public void setActive(int userId, boolean active) {
        try {
            if (userDAO.findById(userId) == null) {
                throw new NotFoundException("User not found.");
            }
            if (!userDAO.updateActive(userId, active)) {
                throw new NotFoundException("User not found.");
            }
        } catch (SQLException e) {
            throw DbErrors.wrap(LOG, "Updating account status", e);
        }
    }

    public List<Resource> listResources() {
        try {
            return resourceDAO.findAll();
        } catch (SQLException e) {
            throw DbErrors.wrap(LOG, "Listing resources", e);
        }
    }

    public void moderateListing(int resourceId, boolean deactivate) {
        try {
            Resource resource = resourceDAO.findById(resourceId);
            if (resource == null) {
                throw new NotFoundException("Resource not found.");
            }
            if (deactivate) {
                resourceDAO.updateStatus(resourceId, ResourceStatus.INACTIVE);
                return;
            }
            if (!resource.isInactive()) {
                throw new ValidationException(
                        "Only an inactive listing can be made available again.");
            }
            resourceDAO.updateStatus(resourceId, ResourceStatus.AVAILABLE);
        } catch (SQLException e) {
            throw DbErrors.wrap(LOG, "Moderating listing", e);
        }
    }

    public List<ResourceRequest> listRequests() {
        try {
            return requestDAO.findAll();
        } catch (SQLException e) {
            throw DbErrors.wrap(LOG, "Listing requests", e);
        }
    }

    public List<Transaction> listTransactions() {
        try {
            return transactionDAO.findAll();
        } catch (SQLException e) {
            throw DbErrors.wrap(LOG, "Listing transactions", e);
        }
    }

    public List<Review> listReviews() {
        try {
            return reviewDAO.findAll();
        } catch (SQLException e) {
            throw DbErrors.wrap(LOG, "Listing reviews", e);
        }
    }

    public Map<String, Integer> stats() {
        Map<String, Integer> stats = new LinkedHashMap<>();
        try {
            stats.put("totalUsers", userDAO.countAll());
            stats.put("verifiedStudents", userDAO.countByVerification(VerificationStatus.VERIFIED));
            stats.put("pendingStudents", userDAO.countByVerification(VerificationStatus.PENDING));
            stats.put("totalResources", resourceDAO.countAll());
            stats.put("availableResources", resourceDAO.countByStatus(ResourceStatus.AVAILABLE));
            stats.put("totalRequests", requestDAO.countAll());
            stats.put("pendingRequests", requestDAO.countByStatus(RequestStatus.PENDING));
            stats.put("totalTransactions", transactionDAO.countAll());
            stats.put("completedTransactions",
                    transactionDAO.countByStatus(TransactionStatus.COMPLETED));
            stats.put("totalReviews", reviewDAO.countAll());
            return stats;
        } catch (SQLException e) {
            throw DbErrors.wrap(LOG, "Loading statistics", e);
        }
    }
}
