package com.campus.exchange.service;

import com.campus.exchange.dao.ResourceDAO;
import com.campus.exchange.dao.ResourceRequestDAO;
import com.campus.exchange.dao.TransactionDAO;
import com.campus.exchange.dao.UserDAO;
import com.campus.exchange.exception.AccessDeniedException;
import com.campus.exchange.exception.ConflictException;
import com.campus.exchange.exception.ValidationException;
import com.campus.exchange.model.Book;
import com.campus.exchange.model.Resource;
import com.campus.exchange.model.ResourceRequest;
import com.campus.exchange.model.Student;
import com.campus.exchange.model.Transaction;
import com.campus.exchange.model.enums.Condition;
import com.campus.exchange.model.enums.ExchangeType;
import com.campus.exchange.model.enums.RequestStatus;
import com.campus.exchange.model.enums.ResourceStatus;
import com.campus.exchange.model.enums.ResourceType;
import com.campus.exchange.model.enums.TransactionStatus;
import com.campus.exchange.model.enums.VerificationStatus;
import com.campus.exchange.util.DbConnection;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.math.BigDecimal;
import java.sql.Connection;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class RequestServiceTest {

    private static final int OWNER_ID = 5;
    private static final int REQUESTER_ID = 6;

    private ResourceDAO resourceDAO;
    private ResourceRequestDAO requestDAO;
    private TransactionDAO transactionDAO;
    private UserDAO userDAO;
    private RequestService requestService;

    private MockedStatic<DbConnection> dbConnection;
    private Connection conn;

    @BeforeEach
    void setUp() {
        resourceDAO = mock(ResourceDAO.class);
        requestDAO = mock(ResourceRequestDAO.class);
        transactionDAO = mock(TransactionDAO.class);
        userDAO = mock(UserDAO.class);
        requestService = new RequestService(resourceDAO, requestDAO, transactionDAO, userDAO);

        conn = mock(Connection.class);
        dbConnection = mockStatic(DbConnection.class);
        dbConnection.when(DbConnection::getConnection).thenReturn(conn);
    }

    @AfterEach
    void tearDown() {
        dbConnection.close();
    }

    @Test
    void unverifiedRequesterIsRejectedBeforeAnyDatabaseWork() throws Exception {
        when(userDAO.findById(REQUESTER_ID)).thenReturn(unverifiedStudent(REQUESTER_ID));

        assertThrows(AccessDeniedException.class,
                () -> requestService.createRequest(11, REQUESTER_ID, "Please?"));

        verify(resourceDAO, never()).findByIdForUpdate(any(), anyInt());
    }

    @Test
    void cannotRequestOwnResource() throws Exception {
        when(userDAO.findById(OWNER_ID)).thenReturn(verifiedStudent(OWNER_ID));
        when(resourceDAO.findByIdForUpdate(conn, 11)).thenReturn(resource(11, OWNER_ID, ResourceStatus.AVAILABLE));

        assertThrows(ValidationException.class,
                () -> requestService.createRequest(11, OWNER_ID, "Mine"));

        verify(conn).rollback();
        verify(requestDAO, never()).insert(any(), any());
    }

    @Test
    void cannotRequestUnavailableResource() throws Exception {
        when(userDAO.findById(REQUESTER_ID)).thenReturn(verifiedStudent(REQUESTER_ID));
        when(resourceDAO.findByIdForUpdate(conn, 11)).thenReturn(resource(11, OWNER_ID, ResourceStatus.RESERVED));

        assertThrows(ConflictException.class,
                () -> requestService.createRequest(11, REQUESTER_ID, "Mine"));

        verify(conn).rollback();
    }

    @Test
    void duplicateActiveRequestIsRejected() throws Exception {
        when(userDAO.findById(REQUESTER_ID)).thenReturn(verifiedStudent(REQUESTER_ID));
        when(resourceDAO.findByIdForUpdate(conn, 11)).thenReturn(resource(11, OWNER_ID, ResourceStatus.AVAILABLE));
        when(requestDAO.hasActiveRequest(conn, 11, REQUESTER_ID)).thenReturn(true);

        assertThrows(ConflictException.class,
                () -> requestService.createRequest(11, REQUESTER_ID, "Again"));

        verify(conn).rollback();
    }

    @Test
    void validRequestIsInsertedAndCommitted() throws Exception {
        when(userDAO.findById(REQUESTER_ID)).thenReturn(verifiedStudent(REQUESTER_ID));
        when(resourceDAO.findByIdForUpdate(conn, 11)).thenReturn(resource(11, OWNER_ID, ResourceStatus.AVAILABLE));
        when(requestDAO.insert(eq(conn), any(ResourceRequest.class))).thenReturn(55);

        assertEquals(55, requestService.createRequest(11, REQUESTER_ID, "Can I borrow it?"));

        verify(conn).commit();
        verify(requestDAO).insert(eq(conn), argThat(request ->
                request.getStatus() == RequestStatus.PENDING
                        && request.getRequesterId() == REQUESTER_ID));
    }

    @Test
    void onlyOwnerCanAcceptAndOnlyRequesterCanCancel() throws Exception {
        when(requestDAO.findByIdForUpdate(conn, 9)).thenReturn(pendingRequest(9, 11));
        when(resourceDAO.findByIdForUpdate(conn, 11)).thenReturn(resource(11, OWNER_ID, ResourceStatus.AVAILABLE));

        assertThrows(AccessDeniedException.class, () -> requestService.acceptRequest(conn, 9, 1234));

        when(requestDAO.findById(9)).thenReturn(pendingRequest(9, 11));
        assertThrows(AccessDeniedException.class, () -> requestService.cancelRequest(9, OWNER_ID));
    }

    @Test
    void onlyPendingRequestsCanBeCancelledOrRejected() throws Exception {
        ResourceRequest accepted = pendingRequest(9, 11);
        accepted.setStatus(RequestStatus.ACCEPTED);
        when(requestDAO.findById(9)).thenReturn(accepted);

        assertThrows(ConflictException.class, () -> requestService.cancelRequest(9, REQUESTER_ID));
        assertThrows(ConflictException.class, () -> requestService.rejectRequest(9, OWNER_ID));
    }

    @Test
    void acceptCreatesExactlyOneTransaction() throws Exception {
        when(requestDAO.findByIdForUpdate(conn, 9)).thenReturn(pendingRequest(9, 11));
        when(resourceDAO.findByIdForUpdate(conn, 11)).thenReturn(resource(11, OWNER_ID, ResourceStatus.AVAILABLE));
        when(resourceDAO.updateStatusIfCurrent(conn, 11, ResourceStatus.AVAILABLE, ResourceStatus.RESERVED))
                .thenReturn(true);
        when(requestDAO.updateStatusIfCurrent(conn, 9, RequestStatus.PENDING, RequestStatus.ACCEPTED))
                .thenReturn(true);
        when(transactionDAO.insert(eq(conn), any(Transaction.class))).thenReturn(77);

        assertEquals(77, requestService.acceptRequest(conn, 9, OWNER_ID));

        verify(transactionDAO).insert(eq(conn), argThat(transaction ->
                transaction.getStatus() == TransactionStatus.PENDING_HANDOVER
                        && transaction.getExchangeType() == ExchangeType.SELL
                        && transaction.getOwnerId() == OWNER_ID
                        && transaction.getRequesterId() == REQUESTER_ID));
        verify(transactionDAO, times(1)).insert(any(), any());
    }

    @Test
    void secondAcceptFailsOnceResourceIsReserved() throws Exception {
        ResourceRequest second = pendingRequest(10, 11);
        when(requestDAO.findByIdForUpdate(conn, 10)).thenReturn(second);
        when(resourceDAO.findByIdForUpdate(conn, 11)).thenReturn(resource(11, OWNER_ID, ResourceStatus.RESERVED));

        assertThrows(ConflictException.class, () -> requestService.acceptRequest(conn, 10, OWNER_ID));

        verify(transactionDAO, never()).insert(any(), any());
        verify(resourceDAO, never()).updateStatusIfCurrent(any(), anyInt(), any(), any());
    }

    @Test
    void acceptRollsBackWhenConditionalResourceUpdateAffectsNoRows() throws Exception {
        when(requestDAO.findByIdForUpdate(conn, 9)).thenReturn(pendingRequest(9, 11));
        when(resourceDAO.findByIdForUpdate(conn, 11)).thenReturn(resource(11, OWNER_ID, ResourceStatus.AVAILABLE));
        when(resourceDAO.updateStatusIfCurrent(conn, 11, ResourceStatus.AVAILABLE, ResourceStatus.RESERVED))
                .thenReturn(false);

        assertThrows(ConflictException.class, () -> requestService.acceptRequest(9, OWNER_ID));

        verify(conn).rollback();
        verify(conn, never()).commit();
        verify(transactionDAO, never()).insert(any(), any());
    }

    private ResourceRequest pendingRequest(int requestId, int resourceId) {
        ResourceRequest request = new ResourceRequest();
        request.setRequestId(requestId);
        request.setResourceId(resourceId);
        request.setRequesterId(REQUESTER_ID);
        request.setResourceOwnerId(OWNER_ID);
        request.setStatus(RequestStatus.PENDING);
        return request;
    }

    private Resource resource(int resourceId, int ownerId, ResourceStatus status) {
        Book book = new Book();
        book.setResourceId(resourceId);
        book.setOwnerId(ownerId);
        book.setTitle("A Valid Title");
        book.setDescription("This description is long enough to pass validation.");
        book.setCategory("General");
        book.setResourceType(ResourceType.BOOK);
        book.setCondition(Condition.NEW);
        book.setExchangeType(ExchangeType.SELL);
        book.setPrice(BigDecimal.ONE);
        book.setPickupLocation("Main Library");
        book.setStatus(status);
        return book;
    }

    private Student verifiedStudent(int userId) {
        Student student = new Student();
        student.setUserId(userId);
        student.setActive(true);
        student.setVerificationStatus(VerificationStatus.VERIFIED);
        return student;
    }

    private Student unverifiedStudent(int userId) {
        Student student = verifiedStudent(userId);
        student.setVerificationStatus(VerificationStatus.PENDING);
        return student;
    }
}
