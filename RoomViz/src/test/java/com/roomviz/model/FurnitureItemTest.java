package com.roomviz.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FurnitureItemTest {

    @Test
    void fullConstructor_setsGeometryAndDefaults() {
        FurnitureItem item = new FurnitureItem("f1", "Accent Chair", "CHAIR", 100, 200, 60, 60);

        assertEquals("f1", item.getId());
        assertEquals("Accent Chair", item.getName());
        assertEquals("CHAIR", item.getKind());
        assertEquals(100, item.getX());
        assertEquals(200, item.getY());
        assertEquals(60, item.getW());
        assertEquals(60, item.getH());
        assertEquals(0, item.getRotation());

        // Verify defaults
        assertEquals("#3B82F6", item.getColorHex());
        assertEquals(50, item.getShadingPercent());
        assertEquals("Matte", item.getMaterial());
        assertEquals("Daylight", item.getLighting());
    }

    @Test
    void noArgConstructor_doesNotCrash() {
        FurnitureItem item = new FurnitureItem();
        assertNotNull(item);
        assertNull(item.getId());
        assertNull(item.getName());
    }

    @Test
    void settersAndGetters_work() {
        FurnitureItem item = new FurnitureItem();
        item.setId("id-99");
        item.setName("Round Table");
        item.setKind("TABLE_ROUND");
        item.setX(50);
        item.setY(75);
        item.setW(120);
        item.setH(120);
        item.setRotation(90);
        item.setColorHex("#FF0000");
        item.setShadingPercent(80);
        item.setMaterial("Gloss");
        item.setLighting("Warm");

        assertEquals("id-99", item.getId());
        assertEquals("Round Table", item.getName());
        assertEquals("TABLE_ROUND", item.getKind());
        assertEquals(50, item.getX());
        assertEquals(75, item.getY());
        assertEquals(120, item.getW());
        assertEquals(120, item.getH());
        assertEquals(90, item.getRotation());
        assertEquals("#FF0000", item.getColorHex());
        assertEquals(80, item.getShadingPercent());
        assertEquals("Gloss", item.getMaterial());
        assertEquals("Warm", item.getLighting());
    }
}
