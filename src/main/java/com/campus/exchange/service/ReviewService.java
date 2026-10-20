package com.campus.exchange.service;

import com.campus.exchange.dao.ReviewDAO;
import com.campus.exchange.dao.TransactionDAO;
import com.campus.exchange.exception.AccessDeniedException;
import com.campus.exchange.exception.ConflictException;
import com.campus.exchange.exception.NotFoundException;
import com.campus.exchange.exception.ValidationException;
import com.campus.exchange.model.Review;
import com.campus.exchange.model.Transaction;
import com.campus.exchange.util.DbErrors;
import com.campus.exchange.util.ValidationUtil;

import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.List;
import java.util.logging.Logger;

/**
 * Reviews are restricted to the requester of a completed transaction, once per
 * transaction, with a rating from 1 to 5.
 */
public class ReviewService {

    private static final Logger LOG = Logger.getLogger(ReviewService.class.getName());

    private final ReviewDAO reviewDAO;
    private final TransactionDAO transactionDAO;

    public ReviewService() {
        this(new ReviewDAO(), new TransactionDAO());
    }

    public ReviewService(ReviewDAO reviewDAO, TransactionDAO transactionDAO) {
        this.reviewDAO = reviewDAO;
        this.transactionDAO = transactionDAO;
    }

    public Review createReview(int transactionId, int reviewerId, int rating, String comment) {
        if (rating < 1 || rating > 5) {
            throw new ValidationException("Rating must be between 1 and 5.");
        }
        String cleanComment = ValidationUtil.optional(comment, 1000, "Comment");

        try {
            Transaction transaction = transactionDAO.findById(transactionId);
            if (transaction == null) {
                throw new NotFoundException("Transaction not found.");
            }
            if (transaction.getRequesterId() != reviewerId) {
                throw new AccessDeniedException(
                        "Only the requester can review this transaction.");
            }
            if (!transaction.isCompleted()) {
                throw new ConflictException(
                        "A review can only be submitted after the transaction is completed.");
            }
            if (reviewDAO.findByTransactionId(transactionId) != null) {
                throw new ConflictException("This transaction has already been reviewed.");
            }

            Review review = new Review();
            review.setTransactionId(transactionId);
            review.setReviewerId(reviewerId);
            review.setRating(rating);
            review.setComment(cleanComment);
            reviewDAO.insert(review);
            return review;
        } catch (SQLIntegrityConstraintViolationException e) {
            throw new ConflictException("This transaction has already been reviewed.");
        } catch (SQLException e) {
            throw DbErrors.wrap(LOG, "Saving review", e);
        }
    }

    public Review findByTransaction(int transactionId) {
        try {
            return reviewDAO.findByTransactionId(transactionId);
        } catch (SQLException e) {
            throw DbErrors.wrap(LOG, "Loading review", e);
        }
    }

    public List<Review> findForOwner(int ownerId) {
        try {
            return reviewDAO.findForOwner(ownerId);
        } catch (SQLException e) {
            throw DbErrors.wrap(LOG, "Loading owner reviews", e);
        }
    }

    public List<Review> findAll() {
        try {
            return reviewDAO.findAll();
        } catch (SQLException e) {
            throw DbErrors.wrap(LOG, "Loading reviews", e);
        }
    }
}
