package com.roomviz.model;

/**
 * A single furniture item placed in the 2D planner.
 *
 * Step 1 only needs the structure so we can save/load designs "with data".
 * Planner2D/Shading/3D will fill these values later.
 */
public class FurnitureItem {

    private String id;
    private String kind; // e.g., Chair, Table, Sofa

    // 2D transform (planner space)
    private double x;
    private double y;
    private double w;
    private double h;
    private double rotationDeg;

    // styling (future steps)
    private String colorHex;     // "#RRGGBB"
    private String shadingPreset; // "Day", "Warm", etc.
    private int layer;           // z-order

    public FurnitureItem() {}

    public FurnitureItem(String id, String kind) {
        this.id = id;
        this.kind = kind;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getKind() { return kind; }
    public void setKind(String kind) { this.kind = kind; }

    public double getX() { return x; }
    public void setX(double x) { this.x = x; }

    public double getY() { return y; }
    public void setY(double y) { this.y = y; }

    public double getW() { return w; }
    public void setW(double w) { this.w = w; }

    public double getH() { return h; }
    public void setH(double h) { this.h = h; }

    public double getRotationDeg() { return rotationDeg; }
    public void setRotationDeg(double rotationDeg) { this.rotationDeg = rotationDeg; }

    public String getColorHex() { return colorHex; }
    public void setColorHex(String colorHex) { this.colorHex = colorHex; }

    public String getShadingPreset() { return shadingPreset; }
    public void setShadingPreset(String shadingPreset) { this.shadingPreset = shadingPreset; }

    public int getLayer() { return layer; }
    public void setLayer(int layer) { this.layer = layer; }
}
