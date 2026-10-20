package com.campus.exchange.model.enums;

public enum Role {
    STUDENT("Student"),
    ADMIN("Administrator");

    private final String label;

    Role(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
