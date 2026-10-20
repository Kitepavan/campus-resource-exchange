package com.campus.exchange.model;

import java.math.BigDecimal;

public class Book extends Resource implements Sellable, Lendable {

    @Override
    public String displayDetails() {
        String course = getCourseCode() == null || getCourseCode().isBlank()
                ? ""
                : " (" + getCourseCode() + ")";
        return "Book: " + getTitle() + course;
    }

    @Override
    public BigDecimal getSalePrice() {
        return getPrice();
    }

    @Override
    public int getLoanPeriodDays() {
        return 30;
    }
}
