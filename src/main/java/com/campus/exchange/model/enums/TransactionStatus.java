package com.campus.exchange.model.enums;

public enum TransactionStatus {
    PENDING_HANDOVER("Pending handover"),
    ACTIVE_LOAN("Active loan"),
    COMPLETED("Completed");

    private final String label;

    TransactionStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
