package com.campus.exchange.model;

import com.campus.exchange.model.enums.Role;
import com.campus.exchange.model.enums.VerificationStatus;

import java.time.LocalDateTime;

/**
 * Common account state for students and administrators.
 */
public abstract class User {

    private int userId;
    private String fullName;
    private String email;
    private String passwordHash;
    private String studentId;
    private String department;
    private Integer yearOfStudy;
    private Role role = Role.STUDENT;
    private VerificationStatus verificationStatus = VerificationStatus.PENDING;
    private boolean active = true;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public abstract String getDashboardPath();

    public abstract String getRoleLabel();

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getStudentId() {
        return studentId;
    }

    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public Integer getYearOfStudy() {
        return yearOfStudy;
    }

    public void setYearOfStudy(Integer yearOfStudy) {
        this.yearOfStudy = yearOfStudy;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public VerificationStatus getVerificationStatus() {
        return verificationStatus;
    }

    public void setVerificationStatus(VerificationStatus verificationStatus) {
        this.verificationStatus = verificationStatus;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public boolean isStudent() {
        return role == Role.STUDENT;
    }

    public boolean isAdmin() {
        return role == Role.ADMIN;
    }

    public boolean isVerified() {
        return verificationStatus == VerificationStatus.VERIFIED;
    }
}
