package com.campus.exchange.service;

import com.campus.exchange.backup.ResourceBackupItem;
import com.campus.exchange.backup.ResourceCatalogBackup;
import com.campus.exchange.dao.ResourceDAO;
import com.campus.exchange.dao.UserDAO;
import com.campus.exchange.exception.ValidationException;
import com.campus.exchange.model.Book;
import com.campus.exchange.model.Resource;
import com.campus.exchange.model.Student;
import com.campus.exchange.model.enums.Condition;
import com.campus.exchange.model.enums.ExchangeType;
import com.campus.exchange.model.enums.ResourceStatus;
import com.campus.exchange.model.enums.ResourceType;
import com.campus.exchange.model.enums.VerificationStatus;
import com.campus.exchange.util.DbConnection;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class BackupServiceTest {

    private static final String BACKUP_NAME = "catalog-20260101-120000-001.ser";
    private static final int OWNER_ID = 5;

    @TempDir
    Path backupDirectory;

    private ResourceDAO resourceDAO;
    private UserDAO userDAO;
    private BackupService backupService;

    @BeforeEach
    void setUp() {
        System.setProperty("BACKUP_DIR", backupDirectory.toString());
        resourceDAO = mock(ResourceDAO.class);
        userDAO = mock(UserDAO.class);
        backupService = new BackupService(resourceDAO, userDAO);
    }

    @AfterEach
    void tearDown() {
        System.clearProperty("BACKUP_DIR");
    }

    @Test
    void serializationRoundTripKeepsEveryField() throws Exception {
        ResourceCatalogBackup backup = new ResourceCatalogBackup();
        backup.getItems().add(ResourceBackupItem.from(book(11, OWNER_ID, "Clean Code")));
        Path path = backupDirectory.resolve(BACKUP_NAME);

        backupService.writeBackup(path, backup);
        ResourceCatalogBackup restored = backupService.readBackup(path);

        assertEquals(1, restored.getItems().size());
        ResourceBackupItem item = restored.getItems().get(0);
        assertEquals(OWNER_ID, item.getOwnerId());
        assertEquals("Clean Code", item.getTitle());
        assertEquals("Computer Science", item.getCategory());
        assertEquals(ResourceType.BOOK, item.parsedResourceType());
        assertEquals(Condition.GOOD, item.parsedCondition());
        assertEquals(ExchangeType.SELL, item.parsedExchangeType());
        assertEquals(new BigDecimal("45.00"), item.getPrice());
        assertEquals("CS301", item.getCourseCode());
        assertEquals("Central Library", item.getPickupLocation());
        assertEquals(ResourceStatus.AVAILABLE, item.parsedStatus());
    }

    @Test
    void backupStoresNoCredentialsOrUserDetails() {
        for (Class<?> type : new Class<?>[]{ResourceCatalogBackup.class, ResourceBackupItem.class}) {
            for (Field field : type.getDeclaredFields()) {
                String name = field.getName().toLowerCase();
                assertFalse(name.contains("password"), type.getSimpleName() + "." + field.getName());
                assertFalse(name.contains("hash"), type.getSimpleName() + "." + field.getName());
                assertNotEquals(com.campus.exchange.model.User.class, field.getType());
            }
        }
    }

    @Test
    void corruptedFileIsRejected() throws IOException {
        Path path = backupDirectory.resolve(BACKUP_NAME);
        Files.write(path, "this is not a serialized backup".getBytes());

        assertThrows(ValidationException.class, () -> backupService.readBackup(path));
        assertThrows(ValidationException.class, () -> backupService.restore(BACKUP_NAME));
    }

    @Test
    void disallowedClassIsRejectedByObjectInputFilter() throws Exception {
        Path path = backupDirectory.resolve(BACKUP_NAME);
        Map<String, String> payload = new HashMap<>();
        payload.put("evil", "payload");
        try (ObjectOutputStream output = new ObjectOutputStream(
                Files.newOutputStream(path, StandardOpenOption.CREATE, StandardOpenOption.WRITE))) {
            output.writeObject(payload);
        }

        ValidationException error = assertThrows(ValidationException.class,
                () -> backupService.readBackup(path));
        assertTrue(error.getMessage().contains("disallowed"));
    }

    @Test
    void invalidItemsAreRejectedBeforeRestore() throws Exception {
        ResourceBackupItem badPrice = ResourceBackupItem.from(book(11, OWNER_ID, "Zero Price"));
        badPrice.setPrice(BigDecimal.ZERO);
        assertRejected("Zero Price", badPrice);

        ResourceBackupItem badEnum = ResourceBackupItem.from(book(12, OWNER_ID, "Bad Enum"));
        badEnum.setExchangeType("BORROW");
        assertRejected("Bad Enum", badEnum);

        when(userDAO.findById(OWNER_ID)).thenReturn(null);
        ResourceBackupItem missingOwner = ResourceBackupItem.from(book(13, OWNER_ID, "Orphan"));
        ValidationException error = assertThrows(ValidationException.class,
                () -> restore(missingOwner));
        assertTrue(error.getMessage().contains("missing owner"));
    }

    @Test
    void restoreSkipsDuplicatesAndReportsInsertedCount() throws Exception {
        when(userDAO.findById(OWNER_ID)).thenReturn(verifiedStudent(OWNER_ID));

        ResourceCatalogBackup backup = new ResourceCatalogBackup();
        backup.getItems().add(ResourceBackupItem.from(book(11, OWNER_ID, "Duplicate Book")));
        backup.getItems().add(ResourceBackupItem.from(book(12, OWNER_ID, "Fresh Book")));
        Path path = backupDirectory.resolve(BACKUP_NAME);
        backupService.writeBackup(path, backup);

        try (MockedStatic<DbConnection> db = mockStatic(DbConnection.class)) {
            Connection conn = mock(Connection.class);
            db.when(DbConnection::getConnection).thenReturn(conn);
            when(resourceDAO.existsByOwnerAndTitle(conn, OWNER_ID, "Duplicate Book")).thenReturn(true);
            when(resourceDAO.existsByOwnerAndTitle(conn, OWNER_ID, "Fresh Book")).thenReturn(false);

            assertEquals(1, backupService.restore(BACKUP_NAME));

            verify(resourceDAO, times(1)).insert(eq(conn), any(Resource.class));
            verify(conn).commit();
        }
    }

    @Test
    void backupsAreListedNewestFirstAndUnknownNamesIgnored() throws Exception {
        backupService.writeBackup(backupDirectory.resolve("catalog-20260101-120000-001.ser"),
                new ResourceCatalogBackup());
        backupService.writeBackup(backupDirectory.resolve("catalog-20260202-120000-002.ser"),
                new ResourceCatalogBackup());
        Files.write(backupDirectory.resolve("evil.ser"), "x".getBytes());

        assertEquals(java.util.List.of("catalog-20260202-120000-002.ser", "catalog-20260101-120000-001.ser"),
                backupService.listBackups());
        assertThrows(ValidationException.class, () -> backupService.restore("evil.ser"));
        assertThrows(ValidationException.class, () -> backupService.restore("../evil.ser"));
    }

    private void assertRejected(String title, ResourceBackupItem item) {
        ValidationException error = assertThrows(ValidationException.class, () -> restore(item));
        assertTrue(error.getMessage().startsWith("Backup item 1"),
                "expected an item-level error for " + title + " but was: " + error.getMessage());
    }

    private int restore(ResourceBackupItem item) {
        ResourceCatalogBackup backup = new ResourceCatalogBackup();
        backup.getItems().add(item);
        Path path = backupDirectory.resolve(BACKUP_NAME);
        try {
            backupService.writeBackup(path, backup);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        return backupService.restore(BACKUP_NAME);
    }

    private Student verifiedStudent(int userId) {
        Student student = new Student();
        student.setUserId(userId);
        student.setActive(true);
        student.setVerificationStatus(VerificationStatus.VERIFIED);
        return student;
    }

    private Book book(int resourceId, int ownerId, String title) {
        Book book = new Book();
        book.setResourceId(resourceId);
        book.setOwnerId(ownerId);
        book.setTitle(title);
        book.setDescription("This description is long enough to pass validation.");
        book.setCategory("Computer Science");
        book.setResourceType(ResourceType.BOOK);
        book.setCondition(Condition.GOOD);
        book.setExchangeType(ExchangeType.SELL);
        book.setPrice(new BigDecimal("45.00"));
        book.setCourseCode("CS301");
        book.setPickupLocation("Central Library");
        book.setStatus(ResourceStatus.AVAILABLE);
        return book;
    }
}
