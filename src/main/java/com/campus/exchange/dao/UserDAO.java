package com.campus.exchange.dao;

import com.campus.exchange.model.Admin;
import com.campus.exchange.model.Student;
import com.campus.exchange.model.User;
import com.campus.exchange.model.enums.Role;
import com.campus.exchange.model.enums.VerificationStatus;
import com.campus.exchange.util.DbConnection;
import com.campus.exchange.util.JdbcUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * JDBC access for users. All parameters are bound; no SQL is built from
 * untrusted values.
 */
public class UserDAO {

    public User findById(int userId) throws SQLException {
        String sql = "SELECT * FROM users WHERE user_id = ?";
        try (Connection conn = DbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    public User findByEmail(String email) throws SQLException {
        String sql = "SELECT * FROM users WHERE email = ?";
        try (Connection conn = DbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    public boolean emailExists(String email) throws SQLException {
        return exists("SELECT 1 FROM users WHERE email = ?", email);
    }

    public boolean studentIdExists(String studentId) throws SQLException {
        return exists("SELECT 1 FROM users WHERE student_id = ?", studentId);
    }

    private boolean exists(String sql, String value) throws SQLException {
        try (Connection conn = DbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, value);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public int insert(User user) throws SQLException {
        String sql = "INSERT INTO users (full_name, email, password_hash, student_id, "
                + "department, year_of_study, role, verification_status, is_active) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, user.getFullName());
            ps.setString(2, user.getEmail());
            ps.setString(3, user.getPasswordHash());
            if (user.getStudentId() == null) {
                ps.setNull(4, java.sql.Types.VARCHAR);
            } else {
                ps.setString(4, user.getStudentId());
            }
            if (user.getDepartment() == null) {
                ps.setNull(5, java.sql.Types.VARCHAR);
            } else {
                ps.setString(5, user.getDepartment());
            }
            if (user.getYearOfStudy() == null) {
                ps.setNull(6, java.sql.Types.TINYINT);
            } else {
                ps.setInt(6, user.getYearOfStudy());
            }
            ps.setString(7, user.getRole().name());
            ps.setString(8, user.getVerificationStatus().name());
            ps.setBoolean(9, user.isActive());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    user.setUserId(keys.getInt(1));
                }
            }
            return user.getUserId();
        }
    }

    public boolean updateProfile(int userId, String fullName, String department,
                                 Integer yearOfStudy) throws SQLException {
        String sql = "UPDATE users SET full_name = ?, department = ?, year_of_study = ? "
                + "WHERE user_id = ?";
        try (Connection conn = DbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, fullName);
            if (department == null) {
                ps.setNull(2, java.sql.Types.VARCHAR);
            } else {
                ps.setString(2, department);
            }
            if (yearOfStudy == null) {
                ps.setNull(3, java.sql.Types.TINYINT);
            } else {
                ps.setInt(3, yearOfStudy);
            }
            ps.setInt(4, userId);
            return ps.executeUpdate() == 1;
        }
    }

    public boolean updateVerificationStatus(int userId, VerificationStatus status)
            throws SQLException {
        String sql = "UPDATE users SET verification_status = ? "
                + "WHERE user_id = ? AND role = 'STUDENT'";
        try (Connection conn = DbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status.name());
            ps.setInt(2, userId);
            return ps.executeUpdate() == 1;
        }
    }

    public boolean updateActive(int userId, boolean active) throws SQLException {
        String sql = "UPDATE users SET is_active = ? WHERE user_id = ?";
        try (Connection conn = DbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBoolean(1, active);
            ps.setInt(2, userId);
            return ps.executeUpdate() == 1;
        }
    }

    public List<User> findAll(String roleFilter, String verificationFilter) throws SQLException {
        StringBuilder sql = new StringBuilder("SELECT * FROM users WHERE 1 = 1");
        List<String> params = new ArrayList<>();
        if (roleFilter != null && !roleFilter.isBlank()) {
            sql.append(" AND role = ?");
            params.add(roleFilter);
        }
        if (verificationFilter != null && !verificationFilter.isBlank()) {
            sql.append(" AND verification_status = ?");
            params.add(verificationFilter);
        }
        sql.append(" ORDER BY created_at DESC");

        List<User> users = new ArrayList<>();
        try (Connection conn = DbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setString(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    users.add(map(rs));
                }
            }
        }
        return users;
    }

    public int countAll() throws SQLException {
        return count("SELECT COUNT(*) FROM users", null);
    }

    public int countByRole(Role role) throws SQLException {
        return count("SELECT COUNT(*) FROM users WHERE role = ?", role.name());
    }

    public int countByVerification(VerificationStatus status) throws SQLException {
        return count("SELECT COUNT(*) FROM users WHERE verification_status = ?", status.name());
    }

    private int count(String sql, String parameter) throws SQLException {
        try (Connection conn = DbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (parameter != null) {
                ps.setString(1, parameter);
            }
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    private User map(ResultSet rs) throws SQLException {
        Role role = Role.valueOf(rs.getString("role"));
        User user = role == Role.ADMIN ? new Admin() : new Student();
        user.setUserId(rs.getInt("user_id"));
        user.setFullName(rs.getString("full_name"));
        user.setEmail(rs.getString("email"));
        user.setPasswordHash(rs.getString("password_hash"));
        user.setStudentId(rs.getString("student_id"));
        user.setDepartment(rs.getString("department"));
        user.setYearOfStudy(JdbcUtil.toInteger(rs, "year_of_study"));
        user.setRole(role);
        user.setVerificationStatus(VerificationStatus.valueOf(
                rs.getString("verification_status")));
        user.setActive(rs.getBoolean("is_active"));
        user.setCreatedAt(JdbcUtil.toLocalDateTime(rs.getTimestamp("created_at")));
        user.setUpdatedAt(JdbcUtil.toLocalDateTime(rs.getTimestamp("updated_at")));
        return user;
    }
}
