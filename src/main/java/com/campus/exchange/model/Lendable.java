package com.campus.exchange.model;

/**
 * A resource that can be lent. The period is a sensible default shown to users.
 */
public interface Lendable {
    int getLoanPeriodDays();
}
