package com.campus.exchange.model;

import java.math.BigDecimal;

public class StudyMaterial extends Resource implements Sellable, Lendable {

    @Override
    public String displayDetails() {
        return "Study material: " + getTitle();
    }

    @Override
    public BigDecimal getSalePrice() {
        return getPrice();
    }

    @Override
    public int getLoanPeriodDays() {
        return 14;
    }
}
