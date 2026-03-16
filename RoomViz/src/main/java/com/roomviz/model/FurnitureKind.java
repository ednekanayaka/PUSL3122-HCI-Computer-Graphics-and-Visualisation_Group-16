package com.roomviz.model;

import com.roomviz.ui.FontAwesome;

public enum FurnitureKind {
    CHAIR(FontAwesome.CHAIR),
    TABLE_RECT(FontAwesome.TABLE),
    TABLE_ROUND(FontAwesome.CIRCLE);

    public final String iconText;
    FurnitureKind(String iconText) { this.iconText = iconText; }
}
