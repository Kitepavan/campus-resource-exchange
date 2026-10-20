package com.campus.exchange.util;

import com.campus.exchange.exception.ValidationException;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Server-side validation shared by servlets and services. Never rely on the
 * browser for these checks.
 */
public final class ValidationUtil {

    private static final Pattern EMAIL =
            Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final Pattern STUDENT_ID =
            Pattern.compile("^[A-Za-z0-9-]{3,40}$");
    private static final Pattern COURSE_CODE =
            Pattern.compile("^[A-Za-z0-9-]{2,20}$");

    private ValidationUtil() {
    }

    public static String notBlank(String value, String field) {
        if (value == null || value.trim().isEmpty()) {
            throw new ValidationException(field + " is required.");
        }
        return value.trim();
    }

    public static String length(String value, String field, int min, int max) {
        String trimmed = notBlank(value, field);
        if (trimmed.length() < min || trimmed.length() > max) {
            throw new ValidationException(
                    field + " must be between " + min + " and " + max + " characters.");
        }
        return trimmed;
    }

    public static String optional(String value, int max, String field) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.length() > max) {
            throw new ValidationException(field + " must be at most " + max + " characters.");
        }
        return trimmed;
    }

    public static String email(String value) {
        String trimmed = notBlank(value, "Email").toLowerCase(Locale.ROOT);
        if (!EMAIL.matcher(trimmed).matches() || trimmed.length() > 190) {
            throw new ValidationException("Enter a valid email address.");
        }
        return trimmed;
    }

    public static String studentId(String value) {
        String trimmed = notBlank(value, "Student ID");
        if (!STUDENT_ID.matcher(trimmed).matches()) {
            throw new ValidationException(
                    "Student ID must be 3-40 letters, digits, or hyphens.");
        }
        return trimmed;
    }

    public static String courseCode(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        String trimmed = value.trim();
        if (!COURSE_CODE.matcher(trimmed).matches()) {
            throw new ValidationException(
                    "Course code must be 2-20 letters, digits, or hyphens.");
        }
        return trimmed;
    }

    public static String password(String value) {
        if (value == null || value.length() < 8) {
            throw new ValidationException("Password must be at least 8 characters.");
        }
        if (value.length() > 72) {
            throw new ValidationException("Password must be at most 72 characters.");
        }
        boolean letter = value.chars().anyMatch(Character::isLetter);
        boolean digit = value.chars().anyMatch(Character::isDigit);
        if (!letter || !digit) {
            throw new ValidationException("Password must contain at least one letter and one digit.");
        }
        return value;
    }

    public static int yearOfStudy(Integer value) {
        if (value == null || value < 1 || value > 8) {
            throw new ValidationException("Year of study must be between 1 and 8.");
        }
        return value;
    }

    public static BigDecimal nonNegative(BigDecimal value, String field) {
        if (value == null || value.signum() < 0) {
            throw new ValidationException(field + " must be zero or greater.");
        }
        return value;
    }

    public static BigDecimal positive(BigDecimal value, String field) {
        if (value == null || value.signum() <= 0) {
            throw new ValidationException(field + " must be greater than zero.");
        }
        return value;
    }

    public static int parseInt(String value, String field) {
        try {
            return Integer.parseInt(value == null ? "" : value.trim());
        } catch (NumberFormatException e) {
            throw new ValidationException("Invalid " + field + ".");
        }
    }

    public static Integer parseIntOrNull(String value, String field) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return parseInt(value, field);
    }

    public static BigDecimal parseDecimal(String value, String field) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            BigDecimal parsed = new BigDecimal(value.trim());
            if (parsed.scale() > 2) {
                throw new ValidationException(field + " may have at most two decimal places.");
            }
            return parsed;
        } catch (NumberFormatException e) {
            throw new ValidationException("Invalid " + field + ".");
        }
    }

    public static void requireTrue(boolean condition, String message) {
        if (!condition) {
            throw new ValidationException(message);
        }
    }
}
