package com.campus.exchange.service;

import com.campus.exchange.dao.ResourceDAO;
import com.campus.exchange.dao.ResourceRequestDAO;
import com.campus.exchange.dao.TransactionDAO;
import com.campus.exchange.exception.AccessDeniedException;
import com.campus.exchange.exception.ConflictException;
import com.campus.exchange.model.Book;
import com.campus.exchange.model.Resource;
import com.campus.exchange.model.ResourceRequest;
import com.campus.exchange.model.Transaction;
import com.campus.exchange.model.enums.ExchangeType;
import com.campus.exchange.model.enums.RequestStatus;
import com.campus.exchange.model.enums.ResourceStatus;
import com.campus.exchange.model.enums.TransactionStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class TransactionServiceTest {

    private static final int OWNER_ID = 5;
    private static final int REQUESTER_ID = 6;
    private static final int OTHER_ID = 99;
    private static final int TRANSACTION_ID = 3;
    private static final int REQUEST_ID = 9;
    private static final int RESOURCE_ID = 11;

    private TransactionDAO transactionDAO;
    private ResourceDAO resourceDAO;
    private ResourceRequestDAO requestDAO;
    private TransactionService transactionService;
    private Connection conn;

    @BeforeEach
    void setUp() {
        transactionDAO = mock(TransactionDAO.class);
        resourceDAO = mock(ResourceDAO.class);
        requestDAO = mock(ResourceRequestDAO.class);
        transactionService = new TransactionService(transactionDAO, resourceDAO, requestDAO);
        conn = mock(Connection.class);
    }

    @Test
    void saleHandoverCompletesTransactionRequestAndResource() throws Exception {
        stubContext(ResourceStatus.RESERVED);
        when(transactionDAO.updateStatus(eq(conn), eq(TRANSACTION_ID),
                eq(TransactionStatus.PENDING_HANDOVER), eq(TransactionStatus.COMPLETED), any(), isNull()))
                .thenReturn(true);
        when(requestDAO.updateStatusIfCurrent(conn, REQUEST_ID, RequestStatus.ACCEPTED, RequestStatus.COMPLETED))
                .thenReturn(true);
        when(resourceDAO.updateStatusIfCurrent(conn, RESOURCE_ID, ResourceStatus.RESERVED, ResourceStatus.UNAVAILABLE))
                .thenReturn(true);

        transactionService.completeHandover(conn, saleTransaction(), OWNER_ID);

        verify(transactionDAO).updateStatus(any(), anyInt(), any(), any(), any(), any());
        verify(resourceDAO).updateStatusIfCurrent(conn, RESOURCE_ID,
                ResourceStatus.RESERVED, ResourceStatus.UNAVAILABLE);
    }

    @Test
    void requesterCanAlsoCompleteHandover() throws Exception {
        stubContext(ResourceStatus.RESERVED);
        when(transactionDAO.updateStatus(any(), anyInt(), any(), any(), any(), any())).thenReturn(true);
        when(requestDAO.updateStatusIfCurrent(any(), anyInt(), any(), any())).thenReturn(true);
        when(resourceDAO.updateStatusIfCurrent(any(), anyInt(), any(), any())).thenReturn(true);

        transactionService.completeHandover(conn, saleTransaction(), REQUESTER_ID);

        verify(resourceDAO).updateStatusIfCurrent(any(), anyInt(),
                eq(ResourceStatus.RESERVED), eq(ResourceStatus.UNAVAILABLE));
    }

    @Test
    void lendGoesThroughActiveLoanAndBackToAvailable() throws Exception {
        stubContext(ResourceStatus.RESERVED);
        when(transactionDAO.updateStatus(any(), anyInt(), any(), any(), any(), any())).thenReturn(true);
        when(resourceDAO.updateStatusIfCurrent(any(), anyInt(), any(), any())).thenReturn(true);
        when(requestDAO.updateStatusIfCurrent(any(), anyInt(), any(), any())).thenReturn(true);

        transactionService.markLent(conn, lendTransaction(TransactionStatus.PENDING_HANDOVER), OWNER_ID);

        verify(resourceDAO).updateStatusIfCurrent(conn, RESOURCE_ID, ResourceStatus.RESERVED, ResourceStatus.LENT);

        stubContext(ResourceStatus.LENT);
        transactionService.markReturned(conn, lendTransaction(TransactionStatus.ACTIVE_LOAN), OWNER_ID);

        verify(resourceDAO).updateStatusIfCurrent(conn, RESOURCE_ID, ResourceStatus.LENT, ResourceStatus.AVAILABLE);
        verify(requestDAO).updateStatusIfCurrent(conn, REQUEST_ID, RequestStatus.ACCEPTED, RequestStatus.COMPLETED);
    }

    @Test
    void returnKeepsListingInactiveWhenAnAdminDeactivatedItDuringTheLoan() throws Exception {
        stubContext(ResourceStatus.INACTIVE);
        when(transactionDAO.updateStatus(any(), anyInt(), any(), any(), any(), any())).thenReturn(true);
        when(requestDAO.updateStatusIfCurrent(any(), anyInt(), any(), any())).thenReturn(true);

        transactionService.markReturned(conn, lendTransaction(TransactionStatus.ACTIVE_LOAN), OWNER_ID);

        verify(resourceDAO, never()).updateStatusIfCurrent(any(), anyInt(), any(), eq(ResourceStatus.AVAILABLE));
        verify(transactionDAO).updateStatus(any(), eq(TRANSACTION_ID), eq(TransactionStatus.ACTIVE_LOAN),
                eq(TransactionStatus.COMPLETED), any(), any());
    }

    @Test
    void invalidTransitionsAreRejected() throws Exception {
        assertThrows(ConflictException.class,
                () -> transactionService.completeHandover(conn, lendTransaction(TransactionStatus.PENDING_HANDOVER), OWNER_ID));
        assertThrows(ConflictException.class,
                () -> transactionService.markLent(conn, saleTransaction(), OWNER_ID));
        assertThrows(ConflictException.class,
                () -> transactionService.markReturned(conn, lendTransaction(TransactionStatus.PENDING_HANDOVER), OWNER_ID));
        assertThrows(ConflictException.class,
                () -> transactionService.completeHandover(conn, saleTransaction(TransactionStatus.COMPLETED), OWNER_ID));

        verifyNoInteractions(transactionDAO);
    }

    @Test
    void nonOwnerCannotChangeTheTransaction() throws Exception {
        assertThrows(AccessDeniedException.class,
                () -> transactionService.markLent(conn, lendTransaction(TransactionStatus.PENDING_HANDOVER), OTHER_ID));
        assertThrows(AccessDeniedException.class,
                () -> transactionService.markReturned(conn, lendTransaction(TransactionStatus.ACTIVE_LOAN), OTHER_ID));
    }

    @Test
    void nonParticipantCannotViewTransaction() throws Exception {
        when(transactionDAO.findById(TRANSACTION_ID)).thenReturn(saleTransaction());

        assertEquals(TRANSACTION_ID, transactionService.getForUser(TRANSACTION_ID, REQUESTER_ID).getTransactionId());
        assertThrows(AccessDeniedException.class, () -> transactionService.getForUser(TRANSACTION_ID, OTHER_ID));
    }

    private void stubContext(ResourceStatus resourceStatus) throws Exception {
        when(requestDAO.findByIdForUpdate(conn, REQUEST_ID)).thenReturn(acceptedRequest());
        when(resourceDAO.findByIdForUpdate(conn, RESOURCE_ID)).thenReturn(resource(resourceStatus));
    }

    private ResourceRequest acceptedRequest() {
        ResourceRequest request = new ResourceRequest();
        request.setRequestId(REQUEST_ID);
        request.setResourceId(RESOURCE_ID);
        request.setRequesterId(REQUESTER_ID);
        request.setResourceOwnerId(OWNER_ID);
        request.setStatus(RequestStatus.ACCEPTED);
        return request;
    }

    private Resource resource(ResourceStatus status) {
        Book book = new Book();
        book.setResourceId(RESOURCE_ID);
        book.setOwnerId(OWNER_ID);
        book.setTitle("A Valid Title");
        book.setStatus(status);
        return book;
    }

    private Transaction saleTransaction() {
        return saleTransaction(TransactionStatus.PENDING_HANDOVER);
    }

    private Transaction saleTransaction(TransactionStatus status) {
        return transaction(ExchangeType.SELL, status);
    }

    private Transaction lendTransaction(TransactionStatus status) {
        return transaction(ExchangeType.LEND, status);
    }

    private Transaction transaction(ExchangeType exchangeType, TransactionStatus status) {
        Transaction transaction = new Transaction();
        transaction.setTransactionId(TRANSACTION_ID);
        transaction.setRequestId(REQUEST_ID);
        transaction.setOwnerId(OWNER_ID);
        transaction.setRequesterId(REQUESTER_ID);
        transaction.setExchangeType(exchangeType);
        transaction.setStatus(status);
        return transaction;
    }
}
