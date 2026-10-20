package com.campus.exchange.service;

import com.campus.exchange.dao.ResourceDAO;
import com.campus.exchange.dao.ResourceRequestDAO;
import com.campus.exchange.dao.TransactionDAO;
import com.campus.exchange.exception.AccessDeniedException;
import com.campus.exchange.exception.AppException;
import com.campus.exchange.exception.ConflictException;
import com.campus.exchange.exception.NotFoundException;
import com.campus.exchange.model.Resource;
import com.campus.exchange.model.ResourceRequest;
import com.campus.exchange.model.Transaction;
import com.campus.exchange.model.enums.RequestStatus;
import com.campus.exchange.model.enums.ResourceStatus;
import com.campus.exchange.model.enums.TransactionStatus;
import com.campus.exchange.util.DbConnection;
import com.campus.exchange.util.DbErrors;
import com.campus.exchange.util.JdbcUtil;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.logging.Logger;

/**
 * Transaction lifecycle: handover, active loans, and returns. Every transition
 * is checked on the server and guarded by conditional updates.
 */
public class TransactionService {

    private static final Logger LOG = Logger.getLogger(TransactionService.class.getName());

    private final TransactionDAO transactionDAO;
    private final ResourceDAO resourceDAO;
    private final ResourceRequestDAO requestDAO;

    public TransactionService() {
        this(new TransactionDAO(), new ResourceDAO(), new ResourceRequestDAO());
    }

    public TransactionService(TransactionDAO transactionDAO, ResourceDAO resourceDAO,
                              ResourceRequestDAO requestDAO) {
        this.transactionDAO = transactionDAO;
        this.resourceDAO = resourceDAO;
        this.requestDAO = requestDAO;
    }

    public void completeHandover(int transactionId, int userId) {
        Transaction transaction = getRequired(transactionId);
        inTransaction(conn -> completeHandover(conn, transaction, userId));
    }

    public void markLent(int transactionId, int ownerId) {
        Transaction transaction = getRequired(transactionId);
        inTransaction(conn -> markLent(conn, transaction, ownerId));
    }

    public void markReturned(int transactionId, int ownerId) {
        Transaction transaction = getRequired(transactionId);
        inTransaction(conn -> markReturned(conn, transaction, ownerId));
    }

    /**
     * Package-private cores for unit testing the state transitions without a
     * live MySQL server.
     */
    void completeHandover(Connection conn, Transaction transaction, int userId)
            throws SQLException {
        requireParticipant(transaction, userId);
        if (!transaction.isSaleOrDonation()) {
            throw new ConflictException("Lending transactions use the loan workflow.");
        }
        if (!transaction.isPendingHandover()) {
            throw new ConflictException("This transaction is not awaiting handover.");
        }

        ResourceRequest request = requestDAO.findByIdForUpdate(conn, transaction.getRequestId());
        if (request == null) {
            throw new NotFoundException("Request not found.");
        }
        Resource resource = resourceDAO.findByIdForUpdate(conn, request.getResourceId());
        if (resource == null) {
            throw new NotFoundException("Resource not found.");
        }
        if (resource.getStatus() != ResourceStatus.RESERVED) {
            throw new ConflictException("The resource is not reserved for this exchange.");
        }

        LocalDateTime now = LocalDateTime.now();
        if (!transactionDAO.updateStatus(conn, transaction.getTransactionId(),
                TransactionStatus.PENDING_HANDOVER, TransactionStatus.COMPLETED,
                JdbcUtil.toTimestamp(now), null)) {
            throw new ConflictException("This transaction has already changed.");
        }
        if (!requestDAO.updateStatusIfCurrent(conn, request.getRequestId(),
                RequestStatus.ACCEPTED, RequestStatus.COMPLETED)) {
            throw new ConflictException("The request is no longer active.");
        }
        if (!resourceDAO.updateStatusIfCurrent(conn, resource.getResourceId(),
                ResourceStatus.RESERVED, ResourceStatus.UNAVAILABLE)) {
            throw new ConflictException("The resource is no longer reserved.");
        }
    }

    void markLent(Connection conn, Transaction transaction, int ownerId) throws SQLException {
        requireOwner(transaction, ownerId);
        if (!transaction.isLend()) {
            throw new ConflictException("Only lending transactions can start a loan.");
        }
        if (!transaction.isPendingHandover()) {
            throw new ConflictException("This transaction is not awaiting handover.");
        }

        ResourceRequest request = requestDAO.findByIdForUpdate(conn, transaction.getRequestId());
        if (request == null) {
            throw new NotFoundException("Request not found.");
        }
        Resource resource = resourceDAO.findByIdForUpdate(conn, request.getResourceId());
        if (resource == null) {
            throw new NotFoundException("Resource not found.");
        }
        if (resource.getStatus() != ResourceStatus.RESERVED) {
            throw new ConflictException("The resource is not reserved for this loan.");
        }

        if (!transactionDAO.updateStatus(conn, transaction.getTransactionId(),
                TransactionStatus.PENDING_HANDOVER, TransactionStatus.ACTIVE_LOAN,
                null, null)) {
            throw new ConflictException("This transaction has already changed.");
        }
        if (!resourceDAO.updateStatusIfCurrent(conn, resource.getResourceId(),
                ResourceStatus.RESERVED, ResourceStatus.LENT)) {
            throw new ConflictException("The resource could not be marked as lent.");
        }
    }

    void markReturned(Connection conn, Transaction transaction, int ownerId) throws SQLException {
        requireOwner(transaction, ownerId);
        if (!transaction.isLend()) {
            throw new ConflictException("Only lending transactions can be returned.");
        }
        if (!transaction.isActiveLoan()) {
            throw new ConflictException("This transaction is not an active loan.");
        }

        ResourceRequest request = requestDAO.findByIdForUpdate(conn, transaction.getRequestId());
        if (request == null) {
            throw new NotFoundException("Request not found.");
        }
        Resource resource = resourceDAO.findByIdForUpdate(conn, request.getResourceId());
        if (resource == null) {
            throw new NotFoundException("Resource not found.");
        }
        if (resource.getStatus() != ResourceStatus.LENT
                && resource.getStatus() != ResourceStatus.INACTIVE) {
            throw new ConflictException("The resource is not currently lent.");
        }

        LocalDateTime now = LocalDateTime.now();
        if (!transactionDAO.updateStatus(conn, transaction.getTransactionId(),
                TransactionStatus.ACTIVE_LOAN, TransactionStatus.COMPLETED,
                JdbcUtil.toTimestamp(now), JdbcUtil.toTimestamp(now))) {
            throw new ConflictException("This transaction has already changed.");
        }
        if (!requestDAO.updateStatusIfCurrent(conn, request.getRequestId(),
                RequestStatus.ACCEPTED, RequestStatus.COMPLETED)) {
            throw new ConflictException("The request is no longer active.");
        }
        if (resource.getStatus() == ResourceStatus.LENT) {
            if (!resourceDAO.updateStatusIfCurrent(conn, resource.getResourceId(),
                    ResourceStatus.LENT, ResourceStatus.AVAILABLE)) {
                throw new ConflictException("The resource could not be made available.");
            }
        }
        // If an administrator deactivated the listing during the loan, it stays INACTIVE.
    }

    public Transaction getForUser(int transactionId, int userId) {
        Transaction transaction = getRequired(transactionId);
        requireParticipant(transaction, userId);
        return transaction;
    }

    public List<Transaction> listForUser(int userId) {
        try {
            return transactionDAO.findByParticipant(userId);
        } catch (SQLException e) {
            throw DbErrors.wrap(LOG, "Loading transactions", e);
        }
    }

    public List<Transaction> listAll() {
        try {
            return transactionDAO.findAll();
        } catch (SQLException e) {
            throw DbErrors.wrap(LOG, "Loading all transactions", e);
        }
    }

    public Transaction findByRequestId(int requestId) {
        try {
            return transactionDAO.findByRequestId(requestId);
        } catch (SQLException e) {
            throw DbErrors.wrap(LOG, "Loading transaction for request", e);
        }
    }

    public Transaction getRequired(int transactionId) {
        try {
            Transaction transaction = transactionDAO.findById(transactionId);
            if (transaction == null) {
                throw new NotFoundException("Transaction not found.");
            }
            return transaction;
        } catch (SQLException e) {
            throw DbErrors.wrap(LOG, "Loading transaction", e);
        }
    }

    private void requireParticipant(Transaction transaction, int userId) {
        if (transaction.getOwnerId() != userId && transaction.getRequesterId() != userId) {
            throw new AccessDeniedException(
                    "Only transaction participants can view or update this exchange.");
        }
    }

    private void requireOwner(Transaction transaction, int ownerId) {
        if (transaction.getOwnerId() != ownerId) {
            throw new AccessDeniedException(
                    "Only the resource owner can update this transaction.");
        }
    }

    private void inTransaction(TransactionWork work) {
        try (Connection conn = DbConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                work.run(conn);
                conn.commit();
            } catch (AppException e) {
                rollbackQuietly(conn);
                throw e;
            } catch (SQLException e) {
                rollbackQuietly(conn);
                throw DbErrors.wrap(LOG, "Updating transaction", e);
            }
        } catch (SQLException e) {
            throw DbErrors.wrap(LOG, "Opening transaction", e);
        }
    }

    private void rollbackQuietly(Connection conn) {
        try {
            conn.rollback();
        } catch (SQLException ignored) {
            // Preserve the original error.
        }
    }

    @FunctionalInterface
    private interface TransactionWork {
        void run(Connection conn) throws SQLException;
    }
}
