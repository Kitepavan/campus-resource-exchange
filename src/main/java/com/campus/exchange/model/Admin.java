package com.campus.exchange.model;

import com.campus.exchange.model.enums.Role;
import com.campus.exchange.model.enums.VerificationStatus;

/**
 * Platform administrator. Administrators are never created through public
 * registration; they are provisioned through the documented SQL workflow.
 */
public class Admin extends User {

    public Admin() {
        setRole(Role.ADMIN);
        setVerificationStatus(VerificationStatus.VERIFIED);
    }

    @Override
    public String getDashboardPath() {
        return "/admin";
    }

    @Override
    public String getRoleLabel() {
        return "Administrator";
    }

    public boolean canModerate() {
        return isActive();
    }
}
