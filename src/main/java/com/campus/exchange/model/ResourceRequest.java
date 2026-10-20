package com.campus.exchange.model;

import com.campus.exchange.model.enums.ExchangeType;
import com.campus.exchange.model.enums.RequestStatus;

import java.time.LocalDateTime;

/**
 * A student's request for a resource. Named ResourceRequest to avoid confusion
 * with jakarta.servlet.http.HttpServletRequest.
 */
public class ResourceRequest {

    private int requestId;
    private int resourceId;
    private int requesterId;
    private String message;
    private RequestStatus status = RequestStatus.PENDING;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Read-only fields populated by joins for list/detail pages.
    private String resourceTitle;
    private int resourceOwnerId;
    private String ownerName;
    private String requesterName;
    private ExchangeType exchangeType;

    public boolean isPending() {
        return status == RequestStatus.PENDING;
    }

    public boolean isAccepted() {
        return status == RequestStatus.ACCEPTED;
    }

    public boolean isCompleted() {
        return status == RequestStatus.COMPLETED;
    }

    public int getRequestId() {
        return requestId;
    }

    public void setRequestId(int requestId) {
        this.requestId = requestId;
    }

    public int getResourceId() {
        return resourceId;
    }

    public void setResourceId(int resourceId) {
        this.resourceId = resourceId;
    }

    public int getRequesterId() {
        return requesterId;
    }

    public void setRequesterId(int requesterId) {
        this.requesterId = requesterId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public RequestStatus getStatus() {
        return status;
    }

    public void setStatus(RequestStatus status) {
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

    public String getResourceTitle() {
        return resourceTitle;
    }

    public void setResourceTitle(String resourceTitle) {
        this.resourceTitle = resourceTitle;
    }

    public int getResourceOwnerId() {
        return resourceOwnerId;
    }

    public void setResourceOwnerId(int resourceOwnerId) {
        this.resourceOwnerId = resourceOwnerId;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public void setOwnerName(String ownerName) {
        this.ownerName = ownerName;
    }

    public String getRequesterName() {
        return requesterName;
    }

    public void setRequesterName(String requesterName) {
        this.requesterName = requesterName;
    }

    public ExchangeType getExchangeType() {
        return exchangeType;
    }

    public void setExchangeType(ExchangeType exchangeType) {
        this.exchangeType = exchangeType;
    }
}
