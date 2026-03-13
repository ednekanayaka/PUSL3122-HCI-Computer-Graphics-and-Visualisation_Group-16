package com.roomviz.model;

/**
 * Stores the customer's room specification (size, shape, colour scheme).
 */
public class RoomSpec {

    private double width;
    private double length;
    private String unit;        // ft, cm, m
    private String shape;       // Rectangular, Square, L-Shape, Custom
    private String colorScheme; // Neutral Tones, Warm Tones, ...

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
