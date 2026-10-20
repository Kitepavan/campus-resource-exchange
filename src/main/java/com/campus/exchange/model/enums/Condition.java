package com.campus.exchange.model.enums;

public enum Condition {
    NEW("New"),
    LIKE_NEW("Like new"),
    GOOD("Good"),
    FAIR("Fair");

    private final String label;

    Condition(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
