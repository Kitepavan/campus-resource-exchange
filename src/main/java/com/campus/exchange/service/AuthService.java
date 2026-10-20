package com.campus.exchange.service;

import com.campus.exchange.dao.UserDAO;
import com.campus.exchange.exception.AppException;
import com.campus.exchange.exception.NotFoundException;
import com.campus.exchange.exception.ValidationException;
import com.campus.exchange.model.Student;
import com.campus.exchange.model.User;
import com.campus.exchange.util.DbErrors;
import com.campus.exchange.util.PasswordUtil;
import com.campus.exchange.util.ValidationUtil;

import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.logging.Logger;

/**
 * Registration, login, and profile rules.
 */
public class AuthService {

    private static final Logger LOG = Logger.getLogger(AuthService.class.getName());

    private final UserDAO userDAO;

    public AuthService() {
        this(new UserDAO());
    }

    public AuthService(UserDAO userDAO) {
        this.userDAO = userDAO;
    }

    public Student register(String fullName, String email, String studentId,
                            String rawPassword, String department, Integer yearOfStudy) {
        String name = ValidationUtil.length(fullName, "Full name", 2, 120);
        String normalizedEmail = ValidationUtil.email(email);
        String normalizedStudentId = ValidationUtil.studentId(studentId);
        String password = ValidationUtil.password(rawPassword);
        String normalizedDepartment = ValidationUtil.length(
                department, "Department", 2, 100);
        int year = ValidationUtil.yearOfStudy(yearOfStudy);

        try {
            if (userDAO.emailExists(normalizedEmail)) {
                throw new ValidationException("An account with this email already exists.");
            }
            if (userDAO.studentIdExists(normalizedStudentId)) {
                throw new ValidationException("This student ID is already registered.");
            }

            Student student = new Student();
            student.setFullName(name);
            student.setEmail(normalizedEmail);
            student.setStudentId(normalizedStudentId);
            student.setDepartment(normalizedDepartment);
            student.setYearOfStudy(year);
            student.setPasswordHash(PasswordUtil.hash(password));
            userDAO.insert(student);
            return student;
        } catch (SQLIntegrityConstraintViolationException e) {
            throw new ValidationException("Email or student ID is already registered.");
        } catch (SQLException e) {
            throw DbErrors.wrap(LOG, "Registering student", e);
        }
    }

    public User login(String email, String rawPassword) {
        String normalizedEmail = ValidationUtil.email(email);
        String password = ValidationUtil.notBlank(rawPassword, "Password");
        try {
            User user = userDAO.findByEmail(normalizedEmail);
            if (user == null || !PasswordUtil.matches(password, user.getPasswordHash())) {
                throw new ValidationException("Invalid email or password.");
            }
            if (!user.isActive()) {
                throw new ValidationException("This account has been suspended.");
            }
            return user;
        } catch (SQLException e) {
            throw DbErrors.wrap(LOG, "Logging in", e);
        }
    }

    public User findById(int userId) {
        try {
            User user = userDAO.findById(userId);
            if (user == null) {
                throw new NotFoundException("User not found.");
            }
            return user;
        } catch (SQLException e) {
            throw DbErrors.wrap(LOG, "Loading user", e);
        }
    }

    public void updateProfile(int userId, String fullName, String department,
                              Integer yearOfStudy) {
        String name = ValidationUtil.length(fullName, "Full name", 2, 120);
        try {
            User user = userDAO.findById(userId);
            if (user == null) {
                throw new NotFoundException("User not found.");
            }
            String normalizedDepartment;
            Integer normalizedYear;
            if (user.isStudent()) {
                normalizedDepartment = ValidationUtil.length(
                        department, "Department", 2, 100);
                normalizedYear = ValidationUtil.yearOfStudy(yearOfStudy);
            } else {
                normalizedDepartment = ValidationUtil.optional(department, 100, "Department");
                normalizedYear = yearOfStudy;
            }
            if (!userDAO.updateProfile(userId, name, normalizedDepartment, normalizedYear)) {
                throw new NotFoundException("User not found.");
            }
        } catch (SQLException e) {
            throw DbErrors.wrap(LOG, "Updating profile", e);
        }
    }
}
