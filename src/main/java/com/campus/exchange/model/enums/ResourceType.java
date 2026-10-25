package com.campus.exchange.model.enums;

public enum ResourceType {
    BOOK("Book"),
    LAB_EQUIPMENT("Lab equipment"),
    STUDY_MATERIAL("Study material"),
    DAILY_ESSENTIAL("Daily essential");

    private final String label;

    ResourceType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
