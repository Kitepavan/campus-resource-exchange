package com.campus.exchange.model;

import java.math.BigDecimal;

public class DailyEssential extends Resource implements Sellable, Lendable {

    @Override
    public String displayDetails() {
        return "Daily essential: " + getTitle();
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
