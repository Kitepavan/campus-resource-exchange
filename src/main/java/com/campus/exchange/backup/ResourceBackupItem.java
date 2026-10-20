package com.campus.exchange.backup;

import com.campus.exchange.model.Resource;
import com.campus.exchange.model.enums.Condition;
import com.campus.exchange.model.enums.ExchangeType;
import com.campus.exchange.model.enums.ResourceStatus;
import com.campus.exchange.model.enums.ResourceType;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * Portable listing data stored in a catalogue backup. Enums are stored as
 * strings so a backup remains readable if enum constants are reordered.
 */
public class ResourceBackupItem implements Serializable {

    private static final long serialVersionUID = 1L;

    private int ownerId;
    private String title;
    private String description;
    private String category;
    private String resourceType;
    private String condition;
    private String exchangeType;
    private BigDecimal price;
    private String lendingTerms;
    private String courseCode;
    private String pickupLocation;
    private String imagePath;
    private String status;

    public static ResourceBackupItem from(Resource resource) {
        ResourceBackupItem item = new ResourceBackupItem();
        item.setOwnerId(resource.getOwnerId());
        item.setTitle(resource.getTitle());
        item.setDescription(resource.getDescription());
        item.setCategory(resource.getCategory());
        item.setResourceType(resource.getResourceType().name());
        item.setCondition(resource.getCondition().name());
        item.setExchangeType(resource.getExchangeType().name());
        item.setPrice(resource.getPrice());
        item.setLendingTerms(resource.getLendingTerms());
        item.setCourseCode(resource.getCourseCode());
        item.setPickupLocation(resource.getPickupLocation());
        item.setImagePath(resource.getImagePath());
        item.setStatus(resource.getStatus().name());
        return item;
    }

    public ResourceType parsedResourceType() {
        return ResourceType.valueOf(resourceType);
    }

    public Condition parsedCondition() {
        return Condition.valueOf(condition);
    }

    public ExchangeType parsedExchangeType() {
        return ExchangeType.valueOf(exchangeType);
    }

    public ResourceStatus parsedStatus() {
        return ResourceStatus.valueOf(status);
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

    public String getResourceType() {
        return resourceType;
    }

    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }

    public String getCondition() {
        return condition;
    }

    public void setCondition(String condition) {
        this.condition = condition;
    }

    public String getExchangeType() {
        return exchangeType;
    }

    public void setExchangeType(String exchangeType) {
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
