package com.roomviz.model;

/**
 * Stores the customer's room specification (size, shape, colour scheme).
 */
public class RoomSpec {

    private double width;
    private double length;
    private String unit;        // ft, cm, m
    private String shape;       // Rectangular, Square, L-Shape, Custom
    private String roomType;    // Bedroom, Living Room, etc.
    private String colorScheme; // Neutral Tones, Warm Tones, ...

    // L-Shape cut-out (measured in the same unit as width/length)
    // Interpretation: outer rectangle = width x length,
    // cutout rectangle = lCutWidth x lCutLength removed from TOP-RIGHT corner.
    private double lCutWidth;
    private double lCutLength;

    // future-proof: which corner the cutout is on (default TOP_RIGHT)
    private String lCorner = "TOP_RIGHT";

    public RoomSpec() {
        // Gson needs a no-arg constructor
    }

    public RoomSpec(double width, double length, String unit, String shape, String colorScheme) {
        this.width = width;
        this.length = length;
        this.unit = unit;
        this.shape = shape;
        this.colorScheme = colorScheme;
    }

    public double getWidth() {
        return width;
    }

    public void setWidth(double width) {
        this.width = width;
    }

    public double getLength() {
        return length;
    }

    public void setLength(double length) {
        this.length = length;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public String getShape() {
        return shape;
    }

    public void setShape(String shape) {
        this.shape = shape;
    }

    public String getColorScheme() {
        return colorScheme;
    }

    public void setColorScheme(String colorScheme) {
        this.colorScheme = colorScheme;
    }

    public String getRoomType() {
        return roomType;
    }

    public void setRoomType(String roomType) {
        this.roomType = roomType;
    }

    public double getLCutWidth() {
        return lCutWidth;
    }

    public void setLCutWidth(double lCutWidth) {
        this.lCutWidth = lCutWidth;
    }

    public double getLCutLength() {
        return lCutLength;
    }

    public void setLCutLength(double lCutLength) {
        this.lCutLength = lCutLength;
    }

    public String getLCorner() {
        return (lCorner == null || lCorner.isBlank()) ? "TOP_RIGHT" : lCorner;
    }

    public void setLCorner(String lCorner) {
        this.lCorner = (lCorner == null || lCorner.isBlank()) ? "TOP_RIGHT" : lCorner;
    }

    /** For Library cards (quick summary) */
    public String toSizeLabel() {
        if (width <= 0 || length <= 0) return "-";
        String u = (unit == null ? "" : unit.trim());
        return trimDouble(width) + "×" + trimDouble(length) + " " + u;
    }

    private static String trimDouble(double d) {
        long asLong = (long) d;
        if (asLong == d) return String.valueOf(asLong);
        return String.format(java.util.Locale.US, "%.2f", d).replaceAll("0+$", "").replaceAll("\\.$", "");
    }
}
