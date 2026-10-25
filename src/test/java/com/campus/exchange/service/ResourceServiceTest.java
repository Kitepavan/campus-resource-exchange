package com.campus.exchange.service;

import com.campus.exchange.dao.ResourceDAO;
import com.campus.exchange.exception.AccessDeniedException;
import com.campus.exchange.exception.ValidationException;
import com.campus.exchange.model.Book;
import com.campus.exchange.model.LabEquipment;
import com.campus.exchange.model.Resource;
import com.campus.exchange.model.Student;
import com.campus.exchange.model.enums.Condition;
import com.campus.exchange.model.enums.ExchangeType;
import com.campus.exchange.model.enums.ResourceStatus;
import com.campus.exchange.model.enums.ResourceType;
import com.campus.exchange.model.enums.VerificationStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ResourceServiceTest {

    private ResourceDAO resourceDAO;
    private ResourceService resourceService;

    @BeforeEach
    void setUp() {
        resourceDAO = mock(ResourceDAO.class);
        resourceService = new ResourceService(resourceDAO);
    }

    @Test
    void verifiedStudentCanCreateListing() throws Exception {
        when(resourceDAO.insert(any(Resource.class))).thenAnswer(invocation -> {
            Resource resource = invocation.getArgument(0);
            resource.setResourceId(11);
            return 11;
        });

        Resource created = resourceService.create(validBook(), verifiedStudent(5));

        assertEquals(11, created.getResourceId());
        assertEquals(5, created.getOwnerId());
        assertEquals(ResourceStatus.AVAILABLE, created.getStatus());
        verify(resourceDAO).insert(any(Resource.class));
    }

    @Test
    void unverifiedOrInactiveStudentCannotCreate() throws Exception {
        assertThrows(AccessDeniedException.class,
                () -> resourceService.create(validBook(), unverifiedStudent(5)));
        assertThrows(AccessDeniedException.class,
                () -> resourceService.create(validBook(), inactiveStudent(5)));

        verify(resourceDAO, never()).insert(any(Resource.class));
    }

    @Test
    void ownerCanEditOwnListing() throws Exception {
        when(resourceDAO.findById(11)).thenReturn(storedResource(11, 5, ResourceStatus.AVAILABLE));
        when(resourceDAO.update(any(Resource.class))).thenReturn(true);

        Book edited = validBook();
        edited.setResourceId(11);
        edited.setTitle("Edited Title");
        Resource result = resourceService.update(edited, verifiedStudent(5));

        assertEquals(5, result.getOwnerId());
        assertEquals(ResourceStatus.AVAILABLE, result.getStatus());
        verify(resourceDAO).update(any(Resource.class));
    }

    @Test
    void nonOwnerCannotEditOrDeactivate() throws Exception {
        when(resourceDAO.findById(11)).thenReturn(storedResource(11, 6, ResourceStatus.AVAILABLE));

        Book edited = validBook();
        edited.setResourceId(11);
        assertThrows(AccessDeniedException.class,
                () -> resourceService.update(edited, verifiedStudent(5)));
        assertThrows(AccessDeniedException.class,
                () -> resourceService.deactivate(11, verifiedStudent(5)));

        verify(resourceDAO, never()).update(any(Resource.class));
        verify(resourceDAO, never()).updateStatus(anyInt(), any());
    }

    @Test
    void reservedOrLentListingCannotBeEditedOrDeactivated() throws Exception {
        when(resourceDAO.findById(11)).thenReturn(storedResource(11, 5, ResourceStatus.RESERVED));
        Book edited = validBook();
        edited.setResourceId(11);
        assertThrows(ValidationException.class,
                () -> resourceService.update(edited, verifiedStudent(5)));
        assertThrows(ValidationException.class,
                () -> resourceService.deactivate(11, verifiedStudent(5)));

        when(resourceDAO.findById(12)).thenReturn(storedResource(12, 5, ResourceStatus.LENT));
        assertThrows(ValidationException.class,
                () -> resourceService.deactivate(12, verifiedStudent(5)));
    }

    @Test
    void ownerCanDeactivateAvailableListing() throws Exception {
        when(resourceDAO.findById(11)).thenReturn(storedResource(11, 5, ResourceStatus.AVAILABLE));
        when(resourceDAO.updateStatus(11, ResourceStatus.INACTIVE)).thenReturn(true);

        resourceService.deactivate(11, verifiedStudent(5));

        verify(resourceDAO).updateStatus(11, ResourceStatus.INACTIVE);
    }

    @Test
    void lendingNeedsAPositiveFee() throws Exception {
        LabEquipment lend = validLend(null);

        assertThrows(ValidationException.class,
                () -> resourceService.create(lend, verifiedStudent(5)),
                "a LEND listing without a fee must be rejected");
        assertThrows(ValidationException.class,
                () -> resourceService.create(validLend(new BigDecimal("1000001")), verifiedStudent(5)),
                "a fee above the maximum must be rejected");

        when(resourceDAO.insert(any(Resource.class))).thenReturn(11);
        Resource created = resourceService.create(validLend(new BigDecimal("200.00")),
                verifiedStudent(5));

        assertEquals(new BigDecimal("200.00"), created.getPrice());
        assertEquals("200.00 lending fee", created.getPriceLabel());
    }

    @Test
    void legacyLendWithoutFeeIsLabelledClearly() {
        assertEquals("Fee not set", validLend(null).getPriceLabel());
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

    private Student inactiveStudent(int userId) {
        Student student = verifiedStudent(userId);
        student.setActive(false);
        return student;
    }

    private Book storedResource(int resourceId, int ownerId, ResourceStatus status) {
        Book book = validBook();
        book.setResourceId(resourceId);
        book.setOwnerId(ownerId);
        book.setStatus(status);
        return book;
    }

    private Book validBook() {
        Book book = new Book();
        book.setTitle("A Valid Title");
        book.setDescription("This description is long enough to pass validation.");
        book.setCategory("General");
        book.setResourceType(ResourceType.BOOK);
        book.setCondition(Condition.NEW);
        book.setExchangeType(ExchangeType.SELL);
        book.setPrice(BigDecimal.ONE);
        book.setPickupLocation("Main Library");
        return book;
    }

    private LabEquipment validLend(BigDecimal fee) {
        LabEquipment equipment = new LabEquipment();
        equipment.setTitle("Oscilloscope");
        equipment.setDescription("Two-channel scope for the electronics lab, all probes included.");
        equipment.setCategory("Electronics");
        equipment.setResourceType(ResourceType.LAB_EQUIPMENT);
        equipment.setCondition(Condition.GOOD);
        equipment.setExchangeType(ExchangeType.LEND);
        equipment.setPrice(fee);
        equipment.setLendingTerms("7 days, return all probes");
        equipment.setPickupLocation("Electronics Lab");
        return equipment;
    }
}
