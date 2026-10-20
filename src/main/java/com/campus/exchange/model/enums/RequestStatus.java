package com.campus.exchange.model.enums;

public enum RequestStatus {
    PENDING("Pending"),
    ACCEPTED("Accepted"),
    REJECTED("Rejected"),
    CANCELLED("Cancelled"),
    COMPLETED("Completed");

    private final String label;

    RequestStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
