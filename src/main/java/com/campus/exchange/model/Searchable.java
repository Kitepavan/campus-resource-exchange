package com.campus.exchange.model;

/**
 * Implemented by resources that can be searched with a free-text query.
 */
public interface Searchable {
    boolean matches(String query);
}
