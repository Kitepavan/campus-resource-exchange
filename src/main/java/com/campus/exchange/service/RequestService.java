package com.campus.exchange.service;

import com.campus.exchange.dao.ResourceDAO;
import com.campus.exchange.dao.ResourceRequestDAO;
import com.campus.exchange.dao.TransactionDAO;
import com.campus.exchange.dao.UserDAO;
import com.campus.exchange.exception.AccessDeniedException;
import com.campus.exchange.exception.AppException;
import com.campus.exchange.exception.ConflictException;
import com.campus.exchange.exception.NotFoundException;
import com.campus.exchange.exception.ValidationException;
import com.campus.exchange.model.Resource;
import com.campus.exchange.model.ResourceRequest;
import com.campus.exchange.model.Student;
import com.campus.exchange.model.Transaction;
import com.campus.exchange.model.User;
import com.campus.exchange.model.enums.RequestStatus;
import com.campus.exchange.model.enums.ResourceStatus;
import com.campus.exchange.model.enums.TransactionStatus;
import com.campus.exchange.util.DbConnection;
import com.campus.exchange.util.DbErrors;
import com.campus.exchange.util.ValidationUtil;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.logging.Logger;

/**
 * Request workflow. Acceptance and request creation use explicit database
 * transactions and row locks so a resource cannot be reserved twice.
 */
public class RequestService {

    private static final Logger LOG = Logger.getLogger(RequestService.class.getName());

    private final ResourceDAO resourceDAO;
    private final ResourceRequestDAO requestDAO;
    private final TransactionDAO transactionDAO;
    private final UserDAO userDAO;

    public RequestService() {
        this(new ResourceDAO(), new ResourceRequestDAO(), new TransactionDAO(), new UserDAO());
    }

    public RequestService(ResourceDAO resourceDAO, ResourceRequestDAO requestDAO,
                          TransactionDAO transactionDAO, UserDAO userDAO) {
        this.resourceDAO = resourceDAO;
        this.requestDAO = requestDAO;
        this.transactionDAO = transactionDAO;
        this.userDAO = userDAO;
    }

    public int createRequest(int resourceId, int requesterId, String message) {
        String cleanMessage = ValidationUtil.optional(message, 1000, "Message");

        User requester;
        try {
            requester = userDAO.findById(requesterId);
        } catch (SQLException e) {
            throw DbErrors.wrap(LOG, "Loading requester", e);
        }
        if (requester == null) {
            throw new NotFoundException("Requester not found.");
        }
        if (!(requester instanceof Student student) || !student.canSubmitRequests()) {
            throw new AccessDeniedException(
                    "Only verified, active students can submit requests.");
        }

        try (Connection conn = DbConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                Resource resource = resourceDAO.findByIdForUpdate(conn, resourceId);
                if (resource == null) {
                    throw new NotFoundException("Resource not found.");
                }
                if (resource.getOwnerId() == requesterId) {
                    throw new ValidationException("You cannot request your own resource.");
                }
                if (resource.getStatus() != ResourceStatus.AVAILABLE) {
                    throw new ConflictException("This resource is no longer available.");
                }
                if (requestDAO.hasActiveRequest(conn, resourceId, requesterId)) {
                    throw new ConflictException(
                            "You already have an active request for this resource.");
                }

                ResourceRequest request = new ResourceRequest();
                request.setResourceId(resourceId);
                request.setRequesterId(requesterId);
                request.setMessage(cleanMessage);
                request.setStatus(RequestStatus.PENDING);
                int id = requestDAO.insert(conn, request);
                conn.commit();
                return id;
            } catch (AppException e) {
                rollbackQuietly(conn);
                throw e;
            } catch (SQLException e) {
                rollbackQuietly(conn);
                throw DbErrors.wrap(LOG, "Creating request", e);
            }
        } catch (SQLException e) {
            throw DbErrors.wrap(LOG, "Opening request transaction", e);
        }
    }

    public int acceptRequest(int requestId, int ownerId) {
        try (Connection conn = DbConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                int transactionId = acceptRequest(conn, requestId, ownerId);
                conn.commit();
                return transactionId;
            } catch (AppException e) {
                rollbackQuietly(conn);
                throw e;
            } catch (SQLException e) {
                rollbackQuietly(conn);
                throw DbErrors.wrap(LOG, "Accepting request", e);
            }
        } catch (SQLException e) {
            throw DbErrors.wrap(LOG, "Opening acceptance transaction", e);
        }
    }

    /**
     * Package-private transactional core so the locking workflow can be unit
     * tested with a mocked Connection and DAOs.
     */
    int acceptRequest(Connection conn, int requestId, int ownerId) throws SQLException {
        ResourceRequest request = requestDAO.findByIdForUpdate(conn, requestId);
        if (request == null) {
            throw new NotFoundException("Request not found.");
        }
        Resource resource = resourceDAO.findByIdForUpdate(conn, request.getResourceId());
        if (resource == null) {
            throw new NotFoundException("Resource not found.");
        }
        if (resource.getOwnerId() != ownerId) {
            throw new AccessDeniedException("Only the resource owner can accept this request.");
        }
        if (request.getStatus() != RequestStatus.PENDING) {
            throw new ConflictException("Only pending requests can be accepted.");
        }
        if (resource.getStatus() != ResourceStatus.AVAILABLE) {
            throw new ConflictException("This resource is no longer available.");
        }

        if (!resourceDAO.updateStatusIfCurrent(
                conn, resource.getResourceId(), ResourceStatus.AVAILABLE, ResourceStatus.RESERVED)) {
            throw new ConflictException("This resource was reserved by another request.");
        }
        if (!requestDAO.updateStatusIfCurrent(
                conn, requestId, RequestStatus.PENDING, RequestStatus.ACCEPTED)) {
            throw new ConflictException("This request is no longer pending.");
        }

        Transaction transaction = new Transaction();
        transaction.setRequestId(requestId);
        transaction.setOwnerId(ownerId);
        transaction.setRequesterId(request.getRequesterId());
        transaction.setExchangeType(resource.getExchangeType());
        transaction.setStatus(TransactionStatus.PENDING_HANDOVER);
        return transactionDAO.insert(conn, transaction);
    }

    public void rejectRequest(int requestId, int ownerId) {
        ResourceRequest request = getRequired(requestId);
        if (request.getResourceOwnerId() != ownerId) {
            throw new AccessDeniedException("Only the resource owner can reject this request.");
        }
        if (!request.isPending()) {
            throw new ConflictException("Only pending requests can be rejected.");
        }
        try (Connection conn = DbConnection.getConnection()) {
            if (!requestDAO.updateStatusIfCurrent(
                    conn, requestId, RequestStatus.PENDING, RequestStatus.REJECTED)) {
                throw new ConflictException("This request is no longer pending.");
            }
        } catch (SQLException e) {
            throw DbErrors.wrap(LOG, "Rejecting request", e);
        }
    }

    public void cancelRequest(int requestId, int requesterId) {
        ResourceRequest request = getRequired(requestId);
        if (request.getRequesterId() != requesterId) {
            throw new AccessDeniedException("You can only cancel your own requests.");
        }
        if (!request.isPending()) {
            throw new ConflictException("Only pending requests can be cancelled.");
        }
        try (Connection conn = DbConnection.getConnection()) {
            if (!requestDAO.updateStatusIfCurrent(
                    conn, requestId, RequestStatus.PENDING, RequestStatus.CANCELLED)) {
                throw new ConflictException("This request is no longer pending.");
            }
        } catch (SQLException e) {
            throw DbErrors.wrap(LOG, "Cancelling request", e);
        }
    }

    public List<ResourceRequest> findOutgoing(int requesterId) {
        try {
            return requestDAO.findOutgoing(requesterId);
        } catch (SQLException e) {
            throw DbErrors.wrap(LOG, "Loading outgoing requests", e);
        }
    }

    public List<ResourceRequest> findIncoming(int ownerId) {
        try {
            return requestDAO.findIncoming(ownerId);
        } catch (SQLException e) {
            throw DbErrors.wrap(LOG, "Loading incoming requests", e);
        }
    }

    public ResourceRequest getRequired(int requestId) {
        try {
            ResourceRequest request = requestDAO.findById(requestId);
            if (request == null) {
                throw new NotFoundException("Request not found.");
            }
            return request;
        } catch (SQLException e) {
            throw DbErrors.wrap(LOG, "Loading request", e);
        }
    }

    private void rollbackQuietly(Connection conn) {
        try {
            conn.rollback();
        } catch (SQLException ignored) {
            // The original failure is more useful than a rollback failure.
        }
    }
}
