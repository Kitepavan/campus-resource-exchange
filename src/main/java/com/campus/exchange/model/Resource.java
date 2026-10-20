package com.campus.exchange.model;

import com.campus.exchange.model.enums.Condition;
import com.campus.exchange.model.enums.ExchangeType;
import com.campus.exchange.model.enums.ResourceStatus;
import com.campus.exchange.model.enums.ResourceType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Locale;

/**
 * Base class for every listing. Subclasses add domain-specific presentation
 * while the exchange workflow works with Resource references.
 */
public abstract class Resource implements Searchable {

    private int resourceId;
    private int ownerId;
    private String title;
    private String description;
    private String category;
    private ResourceType resourceType;
    private Condition condition;
    private ExchangeType exchangeType;
    private BigDecimal price;
    private String lendingTerms;
    private String courseCode;
    private String pickupLocation;
    private String imagePath;
    private ResourceStatus status = ResourceStatus.AVAILABLE;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * Human-readable summary rendered by the catalogue and detail pages.
     */
    public abstract String displayDetails();

    @Override
    public boolean matches(String query) {
        if (query == null || query.isBlank()) {
            return true;
        }
        String needle = query.trim().toLowerCase(Locale.ROOT);
        return contains(title, needle)
                || contains(description, needle)
                || contains(category, needle)
                || contains(courseCode, needle);
    }

    private boolean contains(String value, String needle) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(needle);
    }

    public boolean isAvailable() {
        return status == ResourceStatus.AVAILABLE;
    }

    public boolean isReserved() {
        return status == ResourceStatus.RESERVED;
    }

    public boolean isLent() {
        return status == ResourceStatus.LENT;
    }

    public boolean isInactive() {
        return status == ResourceStatus.INACTIVE;
    }

    public boolean isUnavailable() {
        return status == ResourceStatus.UNAVAILABLE;
    }

    public String getPriceLabel() {
        if (exchangeType == ExchangeType.DONATE || price == null || price.signum() == 0) {
            return "Free";
        }
        return price.toPlainString();
    }

    public int getResourceId() {
        return resourceId;
    }

    public void setResourceId(int resourceId) {
        this.resourceId = resourceId;
    }

    public int getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(int ownerId) {
        this.ownerId = ownerId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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

    public Condition getCondition() {
        return condition;
    }

    public void setCondition(Condition condition) {
        this.condition = condition;
    }

    public ExchangeType getExchangeType() {
        return exchangeType;
    }

    public void setExchangeType(ExchangeType exchangeType) {
        this.exchangeType = exchangeType;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public String getLendingTerms() {
        return lendingTerms;
    }

    public void setLendingTerms(String lendingTerms) {
        this.lendingTerms = lendingTerms;
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

    public String getImagePath() {
        return imagePath;
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }

    public ResourceStatus getStatus() {
        return status;
    }

    public void setStatus(ResourceStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
