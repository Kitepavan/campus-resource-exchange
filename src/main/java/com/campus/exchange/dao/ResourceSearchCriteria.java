package com.campus.exchange.dao;

import com.campus.exchange.model.enums.Condition;
import com.campus.exchange.model.enums.ExchangeType;
import com.campus.exchange.model.enums.ResourceType;

import java.math.BigDecimal;

/**
 * Optional catalog filters. Every field is nullable; blank values are ignored.
 */
public class ResourceSearchCriteria {

    private String keyword;
    private String category;
    private ResourceType resourceType;
    private ExchangeType exchangeType;
    private Condition condition;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private String courseCode;
    private String pickupLocation;
    private String sort;
    private int limit;
    private int offset;

    public String getSort() {
        return sort;
    }

    public void setSort(String sort) {
        this.sort = sort;
    }

    public int getLimit() {
        return limit;
    }

    public void setLimit(int limit) {
        this.limit = limit;
    }

    public int getOffset() {
        return offset;
    }

    public void setOffset(int offset) {
        this.offset = offset;
    }

    public boolean isEmpty() {
        return isBlank(keyword) && isBlank(category) && resourceType == null
                && exchangeType == null && condition == null && minPrice == null
                && maxPrice == null && isBlank(courseCode) && isBlank(pickupLocation);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public ResourceType getResourceType() {
        return resourceType;
    }

    public void setResourceType(ResourceType resourceType) {
        this.resourceType = resourceType;
    }

    public ExchangeType getExchangeType() {
        return exchangeType;
    }

    public void setExchangeType(ExchangeType exchangeType) {
        this.exchangeType = exchangeType;
    }

    public Condition getCondition() {
        return condition;
    }

    public void setCondition(Condition condition) {
        this.condition = condition;
    }

    public BigDecimal getMinPrice() {
        return minPrice;
    }

    public void setMinPrice(BigDecimal minPrice) {
        this.minPrice = minPrice;
    }

    public BigDecimal getMaxPrice() {
        return maxPrice;
    }

    public void setMaxPrice(BigDecimal maxPrice) {
        this.maxPrice = maxPrice;
    }

    public String getCourseCode() {
        return courseCode;
    }

    public void setCourseCode(String courseCode) {
        this.courseCode = courseCode;
    }

    public String getPickupLocation() {
        return pickupLocation;
    }

    public void setPickupLocation(String pickupLocation) {
        this.pickupLocation = pickupLocation;
    }
}
