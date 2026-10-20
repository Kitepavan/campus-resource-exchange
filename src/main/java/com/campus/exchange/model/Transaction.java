package com.campus.exchange.model;

import com.campus.exchange.model.enums.ExchangeType;
import com.campus.exchange.model.enums.TransactionStatus;

import java.time.LocalDateTime;

/**
 * The recorded exchange created when an owner accepts a request.
 */
public class Transaction {

    private int transactionId;
    private int requestId;
    private int ownerId;
    private int requesterId;
    private ExchangeType exchangeType;
    private TransactionStatus status = TransactionStatus.PENDING_HANDOVER;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
    private LocalDateTime returnedAt;

    // Read-only fields populated by joins.
    private int resourceId;
    private String resourceTitle;
    private String ownerName;
    private String requesterName;
    private String requestMessage;

    public boolean isPendingHandover() {
        return status == TransactionStatus.PENDING_HANDOVER;
    }

    public boolean isActiveLoan() {
        return status == TransactionStatus.ACTIVE_LOAN;
    }

    public boolean isCompleted() {
        return status == TransactionStatus.COMPLETED;
    }

    public boolean isLend() {
        return exchangeType == ExchangeType.LEND;
    }

    public boolean isSaleOrDonation() {
        return exchangeType == ExchangeType.SELL || exchangeType == ExchangeType.DONATE;
    }

    public int getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(int transactionId) {
        this.transactionId = transactionId;
    }

    public int getRequestId() {
        return requestId;
    }

    public void setRequestId(int requestId) {
        this.requestId = requestId;
    }

    public int getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(int ownerId) {
        this.ownerId = ownerId;
    }

    public int getRequesterId() {
        return requesterId;
    }

    public void setRequesterId(int requesterId) {
        this.requesterId = requesterId;
    }

    public ExchangeType getExchangeType() {
        return exchangeType;
    }

    public void setExchangeType(ExchangeType exchangeType) {
        this.exchangeType = exchangeType;
    }

    public TransactionStatus getStatus() {
        return status;
    }

    public void setStatus(TransactionStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }

    public LocalDateTime getReturnedAt() {
        return returnedAt;
    }

    public void setReturnedAt(LocalDateTime returnedAt) {
        this.returnedAt = returnedAt;
    }

    public int getResourceId() {
        return resourceId;
    }

    public void setResourceId(int resourceId) {
        this.resourceId = resourceId;
    }

    public String getResourceTitle() {
        return resourceTitle;
    }

    public void setResourceTitle(String resourceTitle) {
        this.resourceTitle = resourceTitle;
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

    public String getRequestMessage() {
        return requestMessage;
    }

    public void setRequestMessage(String requestMessage) {
        this.requestMessage = requestMessage;
    }
}
