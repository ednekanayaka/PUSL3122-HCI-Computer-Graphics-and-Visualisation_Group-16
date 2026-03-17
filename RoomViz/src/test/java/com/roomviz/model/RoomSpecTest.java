package com.roomviz.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RoomSpecTest {

    @Test
    void toSizeLabel_formatsCorrectly() {
        RoomSpec spec = new RoomSpec(12, 10, "ft", "Rectangular", "Neutral Tones");
        assertEquals("12×10 ft", spec.toSizeLabel());
    }

    @Test
    void toSizeLabel_decimalValues_trimmed() {
        RoomSpec spec = new RoomSpec(12.50, 10.10, "m", "Rectangular", "Neutral");
        String label = spec.toSizeLabel();

        // Should trim trailing zeros: 12.5 not 12.50, 10.1 not 10.10
        assertEquals("12.5×10.1 m", label);
    }

    @Test
    void toSizeLabel_wholeNumbers_noDecimal() {
        RoomSpec spec = new RoomSpec(15.0, 20.0, "cm", "Square", "Cool");
        assertEquals("15×20 cm", spec.toSizeLabel());
    }

    @Test
    void toSizeLabel_zeroDimensions_returnsDash() {
        RoomSpec spec = new RoomSpec(0, 10, "ft", "Rectangular", "Neutral");
        assertEquals("-", spec.toSizeLabel());

        RoomSpec spec2 = new RoomSpec(10, 0, "ft", "Rectangular", "Neutral");
        assertEquals("-", spec2.toSizeLabel());
    }

    @Test
    void toSizeLabel_negativeDimensions_returnsDash() {
        RoomSpec spec = new RoomSpec(-5, 10, "ft", "Rectangular", "Neutral");
        assertEquals("-", spec.toSizeLabel());
    }

    @Test
    void toSizeLabel_nullUnit_noException() {
        RoomSpec spec = new RoomSpec(10, 12, null, "Rectangular", "Neutral");
        String label = spec.toSizeLabel();
        assertNotNull(label);
        assertTrue(label.contains("10"));
        assertTrue(label.contains("12"));
    }

    @Test
    void getLCorner_defaults_toTopRight() {
        RoomSpec spec = new RoomSpec();
        // default value from field initializer
        assertEquals("TOP_RIGHT", spec.getLCorner());
    }

    @Test
    void setLCorner_null_defaultsToTopRight() {
        RoomSpec spec = new RoomSpec();
        spec.setLCorner(null);
        assertEquals("TOP_RIGHT", spec.getLCorner());
    }

    @Test
    void setLCorner_blank_defaultsToTopRight() {
        RoomSpec spec = new RoomSpec();
        spec.setLCorner("   ");
        assertEquals("TOP_RIGHT", spec.getLCorner());
    }

    @Test
    void setLCorner_validValue_preserved() {
        RoomSpec spec = new RoomSpec();
        spec.setLCorner("BOTTOM_LEFT");
        assertEquals("BOTTOM_LEFT", spec.getLCorner());
    }

    @Test
    void parameterizedConstructor_setsAllFields() {
        RoomSpec spec = new RoomSpec(15.5, 20.3, "m", "L-Shape", "Warm Tones");

        assertEquals(15.5, spec.getWidth(), 0.001);
        assertEquals(20.3, spec.getLength(), 0.001);
        assertEquals("m", spec.getUnit());
        assertEquals("L-Shape", spec.getShape());
        assertEquals("Warm Tones", spec.getColorScheme());
    }

    @Test
    void lShapeFields_setAndGet() {
        RoomSpec spec = new RoomSpec();
        spec.setLCutWidth(5.0);
        spec.setLCutLength(3.0);

        assertEquals(5.0, spec.getLCutWidth(), 0.001);
        assertEquals(3.0, spec.getLCutLength(), 0.001);
    }
}
