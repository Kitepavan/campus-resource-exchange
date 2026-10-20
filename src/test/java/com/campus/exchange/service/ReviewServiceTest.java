package com.campus.exchange.service;

import com.campus.exchange.dao.ReviewDAO;
import com.campus.exchange.dao.TransactionDAO;
import com.campus.exchange.exception.AccessDeniedException;
import com.campus.exchange.exception.ConflictException;
import com.campus.exchange.exception.ValidationException;
import com.campus.exchange.model.Review;
import com.campus.exchange.model.Transaction;
import com.campus.exchange.model.enums.ExchangeType;
import com.campus.exchange.model.enums.TransactionStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.SQLIntegrityConstraintViolationException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ReviewServiceTest {

    private static final int TRANSACTION_ID = 3;
    private static final int OWNER_ID = 5;
    private static final int REQUESTER_ID = 6;

    private ReviewDAO reviewDAO;
    private TransactionDAO transactionDAO;
    private ReviewService reviewService;

    @BeforeEach
    void setUp() {
        reviewDAO = mock(ReviewDAO.class);
        transactionDAO = mock(TransactionDAO.class);
        reviewService = new ReviewService(reviewDAO, transactionDAO);
    }

    @Test
    void validReviewAfterCompletionIsSaved() throws Exception {
        when(transactionDAO.findById(TRANSACTION_ID)).thenReturn(completedTransaction());
        when(reviewDAO.findByTransactionId(TRANSACTION_ID)).thenReturn(null);

        Review review = reviewService.createReview(TRANSACTION_ID, REQUESTER_ID, 5, "Great condition!");

        assertEquals(5, review.getRating());
        assertEquals(REQUESTER_ID, review.getReviewerId());
        assertEquals("Great condition!", review.getComment());
        verify(reviewDAO).insert(review);
    }

    @Test
    void reviewBeforeCompletionIsRejected() throws Exception {
        when(transactionDAO.findById(TRANSACTION_ID))
                .thenReturn(transaction(TransactionStatus.PENDING_HANDOVER));

        assertThrows(ConflictException.class,
                () -> reviewService.createReview(TRANSACTION_ID, REQUESTER_ID, 4, "Too early"));

        verify(reviewDAO, never()).insert(any());
    }

    @Test
    void onlyTheRequesterCanReview() throws Exception {
        when(transactionDAO.findById(TRANSACTION_ID)).thenReturn(completedTransaction());

        assertThrows(AccessDeniedException.class,
                () -> reviewService.createReview(TRANSACTION_ID, OWNER_ID, 4, "Not mine to review"));
    }

    @Test
    void ratingOutsideOneToFiveIsRejected() {
        assertThrows(ValidationException.class,
                () -> reviewService.createReview(TRANSACTION_ID, REQUESTER_ID, 0, "Bad"));
        assertThrows(ValidationException.class,
                () -> reviewService.createReview(TRANSACTION_ID, REQUESTER_ID, 6, "Bad"));

        verifyNoInteractions(transactionDAO);
    }

    @Test
    void duplicateReviewIsRejectedByServiceCheck() throws Exception {
        when(transactionDAO.findById(TRANSACTION_ID)).thenReturn(completedTransaction());
        when(reviewDAO.findByTransactionId(TRANSACTION_ID)).thenReturn(new Review());

        assertThrows(ConflictException.class,
                () -> reviewService.createReview(TRANSACTION_ID, REQUESTER_ID, 5, "Again"));

        verify(reviewDAO, never()).insert(any());
    }

    @Test
    void duplicateReviewIsRejectedByUniqueKey() throws Exception {
        when(transactionDAO.findById(TRANSACTION_ID)).thenReturn(completedTransaction());
        when(reviewDAO.findByTransactionId(TRANSACTION_ID)).thenReturn(null);
        when(reviewDAO.insert(any(Review.class)))
                .thenThrow(new SQLIntegrityConstraintViolationException("duplicate key"));

        assertThrows(ConflictException.class,
                () -> reviewService.createReview(TRANSACTION_ID, REQUESTER_ID, 5, "Race"));
    }

    private Transaction completedTransaction() {
        return transaction(TransactionStatus.COMPLETED);
    }

    private Transaction transaction(TransactionStatus status) {
        Transaction transaction = new Transaction();
        transaction.setTransactionId(TRANSACTION_ID);
        transaction.setOwnerId(OWNER_ID);
        transaction.setRequesterId(REQUESTER_ID);
        transaction.setExchangeType(ExchangeType.SELL);
        transaction.setStatus(status);
        return transaction;
    }
}
