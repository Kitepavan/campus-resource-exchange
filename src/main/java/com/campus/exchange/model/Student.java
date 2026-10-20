package com.campus.exchange.model;

import com.campus.exchange.model.enums.Role;
import com.campus.exchange.model.enums.VerificationStatus;

/**
 * A registered student. New accounts start unverified and are approved manually.
 */
public class Student extends User {

    public Student() {
        setRole(Role.STUDENT);
        setVerificationStatus(VerificationStatus.PENDING);
    }

    @Override
    public String getDashboardPath() {
        return "/dashboard";
    }

    @Override
    public String getRoleLabel() {
        return "Student";
    }

    public boolean canListResources() {
        return isActive() && isVerified();
    }

    public boolean canSubmitRequests() {
        return isActive() && isVerified();
    }
}
