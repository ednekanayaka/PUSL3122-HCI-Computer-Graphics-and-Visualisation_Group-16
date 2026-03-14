package com.roomviz.model;

public enum FurnitureKind {
    CHAIR("🪑"),
    TABLE_RECT("▭"),
    TABLE_ROUND("●");

    public final String iconText;
    FurnitureKind(String iconText) { this.iconText = iconText; }
}
