package com.roomviz.ui;

import javax.swing.*;
import java.awt.*;
import java.io.InputStream;

/**
 * Font Awesome icon utility for Swing.
 * Loads FA Free 6.x OTF fonts from classpath and provides icon constants + helpers.
 */
public final class FontAwesome {

    // ── Loaded font bases (size 1f, derive as needed) ──
    private static Font SOLID_BASE;
    private static Font REGULAR_BASE;

    static {
        SOLID_BASE   = loadFont("/fonts/fa-solid-900.otf");
        REGULAR_BASE = loadFont("/fonts/fa-regular-400.otf");
    }

    private FontAwesome() {}

    // ── Icon constants (Solid) ──
    public static final String GAUGE           = "\uf624";  // fa-gauge (dashboard)
    public static final String USERS           = "\uf0c0";  // fa-users
    public static final String SWATCHBOOK      = "\uf5c3";  // fa-swatchbook
    public static final String PLUS            = "\u002b";   // fa-plus
    public static final String VECTOR_SQUARE   = "\uf5cb";  // fa-vector-square
    public static final String PALETTE         = "\uf53f";  // fa-palette
    public static final String CUBE            = "\uf1b2";  // fa-cube
    public static final String GEAR            = "\uf013";  // fa-gear
    public static final String CIRCLE          = "\uf111";  // fa-circle
    public static final String SQUARE          = "\uf0c8";  // fa-square (solid)
    public static final String CHEVRON_DOWN    = "\uf078";  // fa-chevron-down
    public static final String ARROW_DOWN      = "\uf063";  // fa-arrow-down
    public static final String BARS            = "\uf0c9";  // fa-bars
    public static final String TH_LARGE        = "\uf009";  // fa-th-large (grid)
    public static final String CLONE           = "\uf24d";  // fa-clone (duplicate)
    public static final String ARROW_LEFT      = "\uf060";  // fa-arrow-left
    public static final String EXPAND          = "\uf065";  // fa-expand (fullscreen)
    public static final String DOWNLOAD        = "\uf019";  // fa-download
    public static final String ROTATE_LEFT     = "\uf0e2";  // fa-rotate-left (undo)
    public static final String ROTATE_RIGHT    = "\uf01e";  // fa-rotate-right (redo)
    public static final String XMARK           = "\uf00d";  // fa-xmark (close)
    public static final String SUN             = "\uf185";  // fa-sun
    public static final String FIRE            = "\uf06d";  // fa-fire
    public static final String SNOWFLAKE       = "\uf2dc";  // fa-snowflake
    public static final String CIRCLE_INFO     = "\uf05a";  // fa-circle-info
    public static final String COUCH           = "\uf4b8";  // fa-couch
    public static final String CHECK           = "\uf00c";  // fa-check
    public static final String PAINT_BRUSH     = "\uf1fc";  // fa-paint-brush
    public static final String ARROWS_ROTATE   = "\uf021";  // fa-arrows-rotate (reset)
    public static final String CROSSHAIRS      = "\uf05b";  // fa-crosshairs (center)
    public static final String SEARCH          = "\uf002";  // fa-magnifying-glass
    public static final String HOUSE           = "\uf015";  // fa-house
    public static final String CAMERA          = "\uf030";  // fa-camera
    public static final String VIDEO           = "\uf03d";  // fa-video
    public static final String LIGHTBULB       = "\uf0eb";  // fa-lightbulb
    public static final String CHAIR           = "\uf6c0";  // fa-chair
    public static final String TABLE           = "\uf0ce";  // fa-table
    public static final String PENCIL          = "\uf303";  // fa-pencil
    public static final String USER            = "\uf007";  // fa-user
    public static final String LOCK            = "\uf023";  // fa-lock
    public static final String WRENCH          = "\uf0ad";  // fa-wrench
    public static final String EYE             = "\uf06e";  // fa-eye
    public static final String QUESTION_CIRCLE = "\uf059";  // fa-circle-question

    // ── Icon constants (Regular – outline style) ──
    public static final String SQUARE_REGULAR  = "\uf0c8";  // fa-square (regular/outline)

    // ── Font getters ──

    /** Returns the FA Solid font at the given point size. */
    public static Font solid(float size) {
        return (SOLID_BASE != null) ? SOLID_BASE.deriveFont(size) : fallback(size);
    }

    /** Returns the FA Regular font at the given point size. */
    public static Font regular(float size) {
        return (REGULAR_BASE != null) ? REGULAR_BASE.deriveFont(size) : fallback(size);
    }

    // ── Label helpers ──

    /** Creates a JLabel with a solid FA icon at the given size. */
    public static JLabel label(String iconChar, float size) {
        return label(iconChar, size, null);
    }

    /** Creates a JLabel with a solid FA icon at the given size and color. */
    public static JLabel label(String iconChar, float size, Color color) {
        JLabel l = new JLabel(iconChar);
        l.setFont(solid(size));
        if (color != null) l.setForeground(color);
        l.setHorizontalAlignment(SwingConstants.CENTER);
        return l;
    }

    /** Creates a JLabel with a regular (outline) FA icon. */
    public static JLabel labelRegular(String iconChar, float size, Color color) {
        JLabel l = new JLabel(iconChar);
        l.setFont(regular(size));
        if (color != null) l.setForeground(color);
        l.setHorizontalAlignment(SwingConstants.CENTER);
        return l;
    }

    // ── Internal ──

    private static Font loadFont(String resourcePath) {
        try (InputStream is = FontAwesome.class.getResourceAsStream(resourcePath)) {
            if (is == null) {
                System.err.println("[FontAwesome] Font resource not found: " + resourcePath);
                return null;
            }
            Font f = Font.createFont(Font.TRUETYPE_FONT, is);
            GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(f);
            return f;
        } catch (Exception e) {
            System.err.println("[FontAwesome] Failed to load font: " + resourcePath + " — " + e.getMessage());
            return null;
        }
    }

    private static Font fallback(float size) {
        return new Font(Font.SANS_SERIF, Font.PLAIN, (int) size);
    }
}
