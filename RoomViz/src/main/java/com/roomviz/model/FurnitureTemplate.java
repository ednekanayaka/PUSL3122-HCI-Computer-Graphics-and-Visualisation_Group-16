package com.roomviz.model;

public class FurnitureTemplate {
    public final String name;
    public final String displaySize;
    public final FurnitureKind kind;
    public final int defaultW;
    public final int defaultH;
    public final double widthFt;
    public final double depthFt;

    public FurnitureTemplate(String name, String displaySize, FurnitureKind kind, double widthFt, double depthFt) {
        this.name = name;
        this.displaySize = displaySize;
        this.kind = kind;
        this.widthFt = Math.max(0.1, widthFt);
        this.depthFt = Math.max(0.1, depthFt);
        // Legacy preview scale fallback (24 px ~= 1 ft) so older code paths still render reasonably.
        this.defaultW = Math.max(8, (int) Math.round(this.widthFt * 24.0));
        this.defaultH = Math.max(8, (int) Math.round(this.depthFt * 24.0));
    }

    public FurnitureTemplate(String name, String displaySize, FurnitureKind kind, int defaultW, int defaultH) {
        this(
                name,
                displaySize,
                kind,
                Math.max(0.1, defaultW / 24.0),
                Math.max(0.1, defaultH / 24.0)
        );
    }

    @Override public String toString() { return name; }
}
