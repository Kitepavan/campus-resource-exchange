package com.campus.exchange.service;

import com.campus.exchange.dao.UserDAO;
import com.campus.exchange.exception.ValidationException;
import com.campus.exchange.model.Student;
import com.campus.exchange.model.User;
import com.campus.exchange.util.PasswordUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AuthServiceTest {

    private UserDAO userDAO;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        userDAO = mock(UserDAO.class);
        authService = new AuthService(userDAO);
    }

    @Test
    void registerCreatesPendingStudentWithBcryptHash() throws Exception {
        when(userDAO.emailExists("new@campus.test")).thenReturn(false);
        when(userDAO.studentIdExists("STU-001")).thenReturn(false);
        when(userDAO.insert(any(Student.class))).thenAnswer(invocation -> {
            Student student = invocation.getArgument(0);
            student.setUserId(42);
            return 42;
        });

        Student result = authService.register("New Student", "new@campus.test",
                "STU-001", "secret123", "Computer Science", 2);

        assertEquals(42, result.getUserId());
        assertTrue(result.isStudent());
        assertFalse(result.isVerified());
        assertTrue(result.getPasswordHash().startsWith("$2a$12$"));
        assertTrue(PasswordUtil.matches("secret123", result.getPasswordHash()));

        verify(userDAO).insert(any(Student.class));
    }

    @Test
    void registerRejectsDuplicateEmailAndStudentId() throws Exception {
        when(userDAO.emailExists("dup@campus.test")).thenReturn(true);
        assertThrows(ValidationException.class, () -> authService.register(
                "Dup", "dup@campus.test", "STU-001", "secret123", "CS", 1));

        when(userDAO.emailExists("fresh@campus.test")).thenReturn(false);
        when(userDAO.studentIdExists("STU-002")).thenReturn(true);
        assertThrows(ValidationException.class, () -> authService.register(
                "Fresh", "fresh@campus.test", "STU-002", "secret123", "CS", 1));
    }

    @Test
    void registerRejectsWeakInput() {
        assertThrows(ValidationException.class, () -> authService.register(
                "ab", "bad-email", "STU-003", "secret123", "CS", 1));
        assertThrows(ValidationException.class, () -> authService.register(
                "Good Name", "good@campus.test", "STU-003", "short", "CS", 1));
        assertThrows(ValidationException.class, () -> authService.register(
                "Good Name", "good@campus.test", "STU-003", "secret123", "CS", 9));
    }

    @Test
    void loginSucceedsWithCorrectPassword() throws Exception {
        Student student = new Student();
        student.setUserId(7);
        student.setEmail("alice@campus.test");
        student.setPasswordHash(PasswordUtil.hash("secret123"));
        student.setActive(true);
        when(userDAO.findByEmail("alice@campus.test")).thenReturn(student);

        User result = authService.login("alice@campus.test", "secret123");

        assertEquals(7, result.getUserId());
        assertTrue(result.isActive());
    }

    @Test
    void loginRejectsWrongPasswordAndUnknownEmail() throws Exception {
        Student student = new Student();
        student.setPasswordHash(PasswordUtil.hash("secret123"));
        student.setActive(true);
        when(userDAO.findByEmail("alice@campus.test")).thenReturn(student);
        assertThrows(ValidationException.class, () -> authService.login("alice@campus.test", "wrong123"));

        when(userDAO.findByEmail("nobody@campus.test")).thenReturn(null);
        assertThrows(ValidationException.class, () -> authService.login("nobody@campus.test", "secret123"));
    }

    @Test
    void inactiveUserCannotLogIn() throws Exception {
        Student student = new Student();
        student.setPasswordHash(PasswordUtil.hash("secret123"));
        student.setActive(false);
        when(userDAO.findByEmail("inactive@campus.test")).thenReturn(student);

        ValidationException error = assertThrows(ValidationException.class,
                () -> authService.login("inactive@campus.test", "secret123"));
        assertTrue(error.getMessage().contains("suspended"));
    }
}
