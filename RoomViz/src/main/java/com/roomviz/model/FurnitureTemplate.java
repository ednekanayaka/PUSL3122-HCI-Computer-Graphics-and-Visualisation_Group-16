package com.roomviz.model;

public class FurnitureTemplate {
    public final String name;
    public final String displaySize;
    public final FurnitureKind kind;
    public final int defaultW;
    public final int defaultH;

    public FurnitureTemplate(String name, String displaySize, FurnitureKind kind, int defaultW, int defaultH) {
        this.name = name;
        this.displaySize = displaySize;
        this.kind = kind;
        this.defaultW = defaultW;
        this.defaultH = defaultH;
    }

    @Override public String toString() { return name; }
}
