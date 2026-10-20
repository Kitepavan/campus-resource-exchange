package com.campus.exchange.model;

import java.math.BigDecimal;

public class LabEquipment extends Resource implements Sellable, Lendable {

    @Override
    public String displayDetails() {
        return "Lab equipment: " + getTitle();
    }

    @Override
    public BigDecimal getSalePrice() {
        return getPrice();
    }

    @Override
    public int getLoanPeriodDays() {
        return 7;
    }
}
