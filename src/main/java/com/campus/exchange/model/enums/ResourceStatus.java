package com.campus.exchange.model.enums;

public enum ResourceStatus {
    AVAILABLE("Available"),
    RESERVED("Reserved"),
    LENT("Lent"),
    UNAVAILABLE("Unavailable"),
    INACTIVE("Inactive");

    private final String label;

    ResourceStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
