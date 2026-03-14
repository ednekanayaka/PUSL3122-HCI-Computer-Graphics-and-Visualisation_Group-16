package com.roomviz.model;

public class FurnitureItem {
    private String id;          // unique id
    private String name;        // display name (e.g., Accent Chair)
    private String kind;        // CHAIR, TABLE_RECT, TABLE_ROUND (string for simple JSON)

    // geometry in room space (Unit: Inches)
    private int x;
    private int y;
    private int w;
    private int h;
    private int rotation;       // degrees 0..360

    // styling (extended for shading/color tools)
    private String colorHex;     // "#RRGGBB"
    private int shadingPercent;  // 0..100
    private String material;     // Matte/Satin/Gloss
    private String lighting;     // Daylight/Warm/Cool/Neutral

    public FurnitureItem() {}

    public FurnitureItem(String id, String name, String kind, int x, int y, int w, int h) {
        this.id = id;
        this.name = name;
        this.kind = kind;
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
        this.rotation = 0;

        // defaults
        this.colorHex = "#3B82F6";
        this.shadingPercent = 50;
        this.material = "Matte";
        this.lighting = "Daylight";
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getKind() { return kind; }
    public void setKind(String kind) { this.kind = kind; }

    public int getX() { return x; }
    public void setX(int x) { this.x = x; }

    public int getY() { return y; }
    public void setY(int y) { this.y = y; }

    public int getW() { return w; }
    public void setW(int w) { this.w = w; }

    public int getH() { return h; }
    public void setH(int h) { this.h = h; }

    public int getRotation() { return rotation; }
    public void setRotation(int rotation) { this.rotation = rotation; }

    public String getColorHex() { return colorHex; }
    public void setColorHex(String colorHex) { this.colorHex = colorHex; }

    public int getShadingPercent() { return shadingPercent; }
    public void setShadingPercent(int shadingPercent) { this.shadingPercent = shadingPercent; }

    public String getMaterial() { return material; }
    public void setMaterial(String material) { this.material = material; }

    public String getLighting() { return lighting; }
    public void setLighting(String lighting) { this.lighting = lighting; }
}
