package com.campus.exchange.backup;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Administrator-controlled, serializable snapshot of the resource catalogue.
 * It deliberately contains no users, password hashes, sessions, or credentials.
 */
public class ResourceCatalogBackup implements Serializable {

    private static final long serialVersionUID = 1L;

    private String createdAt;
    private List<ResourceBackupItem> items = new ArrayList<>();

    public ResourceCatalogBackup() {
        this.createdAt = LocalDateTime.now().toString();
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public List<ResourceBackupItem> getItems() {
        return items;
    }

    public void setItems(List<ResourceBackupItem> items) {
        this.items = items == null ? new ArrayList<>() : items;
    }
}
