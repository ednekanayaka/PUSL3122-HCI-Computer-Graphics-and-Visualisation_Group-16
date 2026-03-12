package com.roomviz.model;

import java.util.ArrayList;
import java.util.List;

/**
 * A saved design in the designer's portfolio.
 */
public class Design {

    private String id;
    private String designName;
    private String customerName;
    private String notes;

    private RoomSpec roomSpec;

    private long createdAtEpochMs;
    private long lastUpdatedEpochMs;

    private List<FurnitureItem> items = new ArrayList<>();

    public Design() {
        // Gson needs a no-arg constructor
    }

    public Design(String id, String designName, String customerName, String notes, RoomSpec roomSpec) {
        this.id = id;
        this.designName = designName;
        this.customerName = customerName;
        this.notes = notes;
        this.roomSpec = roomSpec;

        long now = System.currentTimeMillis();
        this.createdAtEpochMs = now;
        this.lastUpdatedEpochMs = now;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getDesignName() { return designName; }
    public void setDesignName(String designName) { this.designName = designName; }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public RoomSpec getRoomSpec() { return roomSpec; }
    public void setRoomSpec(RoomSpec roomSpec) { this.roomSpec = roomSpec; }

    public long getCreatedAtEpochMs() { return createdAtEpochMs; }
    public void setCreatedAtEpochMs(long createdAtEpochMs) { this.createdAtEpochMs = createdAtEpochMs; }

    public long getLastUpdatedEpochMs() { return lastUpdatedEpochMs; }
    public void setLastUpdatedEpochMs(long lastUpdatedEpochMs) { this.lastUpdatedEpochMs = lastUpdatedEpochMs; }

    public List<FurnitureItem> getItems() { return items; }
    public void setItems(List<FurnitureItem> items) { this.items = items; }
}
