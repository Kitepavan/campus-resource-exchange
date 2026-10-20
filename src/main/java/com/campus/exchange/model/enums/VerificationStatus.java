package com.campus.exchange.model.enums;

public enum VerificationStatus {
    PENDING("Pending"),
    VERIFIED("Verified"),
    REJECTED("Rejected");

    private final String label;

    VerificationStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
