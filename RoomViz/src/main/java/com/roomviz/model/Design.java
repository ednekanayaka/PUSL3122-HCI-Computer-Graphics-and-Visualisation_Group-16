package com.roomviz.model;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * A saved design in the designer's portfolio.
 */
public class Design {

    private String id;
    private String designName;
    private String customerName;
    private String notes;

    private RoomSpec roomSpec;

    private DesignStatus status = DesignStatus.DRAFT;

    private long createdAtEpochMs;
    private long lastUpdatedEpochMs;

    // 2D layout bounds (for 3D sync)
    private Integer layoutX;
    private Integer layoutY;
    private Integer layoutWidth;
    private Integer layoutHeight;

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

        this.status = DesignStatus.DRAFT;

        long now = System.currentTimeMillis();
        this.createdAtEpochMs = now;
        this.lastUpdatedEpochMs = now;
    }

    /* ========================= Compatibility helpers (NEW) ========================= */

    public String getName() {
        return getDesignName();
    }

    public void setName(String name) {
        setDesignName(name);
    }

    /**
     * ✅ FIXED:
     * Make sure a new Design ALWAYS has a default RoomSpec,
     * otherwise the room border/highlight logic becomes inconsistent.
     */
    public static Design createNew(String designName) {
        Design d = new Design();
        d.id = UUID.randomUUID().toString();
        d.designName = (designName == null || designName.isBlank()) ? "Untitled Design" : designName;

        // ✅ default RoomSpec so room borders can always render
        // (Pick values that match your UI defaults)
        d.roomSpec = new RoomSpec(
                12,               // width (units)
                10,               // height/depth (units)
                "ft",             // unit
                "Rectangular",    // shape
                "Neutral Tones"   // color scheme
        );

        long now = System.currentTimeMillis();
        d.createdAtEpochMs = now;
        d.lastUpdatedEpochMs = now;
        d.items = new ArrayList<>();
        d.status = DesignStatus.DRAFT;
        return d;
    }

    public void touchUpdatedAtNow() {
        this.lastUpdatedEpochMs = System.currentTimeMillis();
    }

    /* ========================= Your existing getters/setters ========================= */

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
    public void setItems(List<FurnitureItem> items) {
        this.items = (items == null) ? new ArrayList<>() : items;
    }

    public DesignStatus getStatus() {
        return (status == null) ? DesignStatus.DRAFT : status;
    }

    public void setStatus(DesignStatus status) {
        this.status = (status == null) ? DesignStatus.DRAFT : status;
    }

    public Integer getLayoutX() { return layoutX; }
    public void setLayoutX(Integer layoutX) { this.layoutX = layoutX; }

    public Integer getLayoutY() { return layoutY; }
    public void setLayoutY(Integer layoutY) { this.layoutY = layoutY; }

    public Integer getLayoutWidth() { return layoutWidth; }
    public void setLayoutWidth(Integer layoutWidth) { this.layoutWidth = layoutWidth; }

    public Integer getLayoutHeight() { return layoutHeight; }
    public void setLayoutHeight(Integer layoutHeight) { this.layoutHeight = layoutHeight; }
}
