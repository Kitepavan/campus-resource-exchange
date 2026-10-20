package com.campus.exchange.model.enums;

public enum ExchangeType {
    SELL("Sell"),
    LEND("Lend"),
    DONATE("Donate");

    private final String label;

    ExchangeType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
