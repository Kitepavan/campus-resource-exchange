package com.campus.exchange.model;

import java.math.BigDecimal;

/**
 * A resource that can be offered for sale.
 */
public interface Sellable {
    BigDecimal getSalePrice();
}
