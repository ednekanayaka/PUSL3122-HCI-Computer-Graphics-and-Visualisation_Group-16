package com.roomviz.ui;

import com.roomviz.model.UserSettings;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.plaf.FontUIResource;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.util.HashMap;
import java.util.Enumeration;
import java.util.Map;

public final class UiKit {
    private UiKit() {}

    // ===== Font Baseline (prevents cumulative scaling) =====
    private static final Map<Object, FontUIResource> BASELINE_FONTS = new HashMap<>();
    private static boolean baselineCaptured = false;

    // ===== Base Palette (Light) =====
    private static final Color BASE_BG = new Color(0xF6F7FB);
    private static final Color BASE_WHITE = Color.WHITE;
    private static final Color BASE_TEXT = new Color(0x111827);
    private static final Color BASE_MUTED = new Color(0x6B7280);
    private static final Color BASE_BORDER = new Color(0xE5E7EB);

    // ===== High Contrast Palette =====
    private static final Color HC_BG = Color.WHITE;
    private static final Color HC_WHITE = Color.WHITE;
    private static final Color HC_TEXT = Color.BLACK;
    private static final Color HC_MUTED = Color.BLACK;
    private static final Color HC_BORDER = Color.BLACK;

    // ===== Theme State (mutable) =====
    public static Color BG = BASE_BG;
    public static Color WHITE = BASE_WHITE;
    public static Color TEXT = BASE_TEXT;
    public static Color MUTED = BASE_MUTED;
    public static Color BORDER = BASE_BORDER;

    public static Color PRIMARY = new Color(0x4F46E5);
    public static Color PRIMARY_DARK = new Color(0x4338CA);

    public static Color CHIP_BG = new Color(0xF3F4F6);
    public static Color CHIP_TEXT = new Color(0x374151);

    public static Color DANGER = new Color(0xDC2626);

    // ===== Apply settings globally =====
    public static void applySettings(UserSettings s) {
        boolean highContrast = (s != null) && s.isHighContrast();
        String fontSize = (s == null) ? "Small" : safe(s.getFontSize(), "Small");

        // Palette
        if (highContrast) {
            BG = HC_BG;
            WHITE = HC_WHITE;
            TEXT = HC_TEXT;
            MUTED = HC_MUTED;
            BORDER = HC_BORDER;

            CHIP_BG = Color.WHITE;
            CHIP_TEXT = Color.BLACK;
        } else {
            BG = BASE_BG;
            WHITE = BASE_WHITE;
            TEXT = BASE_TEXT;
            MUTED = BASE_MUTED;
            BORDER = BASE_BORDER;

            CHIP_BG = new Color(0xF3F4F6);
            CHIP_TEXT = new Color(0x374151);
        }

        // Font scale
        float scale = 1.0f; // Small
        if ("Medium".equalsIgnoreCase(fontSize)) scale = 1.10f;
        if ("Large".equalsIgnoreCase(fontSize)) scale = 1.20f;

        applyGlobalFontScale(scale);
    }

    /** Call after applySettings() to refresh all currently visible UI */
    public static void refreshUI(Window window) {
        if (window == null) return;
        SwingUtilities.updateComponentTreeUI(window);
        window.invalidate();
        window.validate();
        window.repaint();
    }

    private static void applyGlobalFontScale(float scale) {
        UIDefaults defaults = UIManager.getLookAndFeelDefaults();

        // Capture baseline once
        if (!baselineCaptured) {
            Enumeration<Object> keys0 = defaults.keys();
            while (keys0.hasMoreElements()) {
                Object key = keys0.nextElement();
                Object value = defaults.get(key);
                if (value instanceof FontUIResource) {
                    BASELINE_FONTS.put(key, (FontUIResource) value);
                }
            }
            baselineCaptured = true;
        }

        // Apply from baseline (no accumulation)
        for (Map.Entry<Object, FontUIResource> e : BASELINE_FONTS.entrySet()) {
            FontUIResource base = e.getValue();
            float newSize = Math.max(10f, base.getSize2D() * scale);
            defaults.put(e.getKey(), new FontUIResource(base.deriveFont(newSize)));
        }
    }

    private static String safe(String s, String fallback) {
        if (s == null) return fallback;
        String t = s.trim();
        return t.isEmpty() ? fallback : t;
    }

    // ===== Typography helpers =====
    /** Multiplier-based so it scales up/down correctly with global settings. */
    public static Font scaled(JComponent c, int style, float multiplier) {
        Font base = c.getFont();
        return base.deriveFont(style, Math.max(10f, base.getSize2D() * multiplier));
    }

    /** Small label style used in forms/settings panels. */
    public static JLabel fieldLabel(String text) {
        JLabel l = new JLabel(text);
        l.setForeground(MUTED);
        l.setFont(scaled(l, Font.PLAIN, 0.92f));
        return l;
    }

    // ===== Rounded panel =====
    public static class RoundedPanel extends JPanel {
        private final int radius;
        private Color fill;
        private Color border;

        public RoundedPanel(int radius, Color fill) {
            this.radius = radius;
            this.fill = fill;
            setOpaque(false);
        }

        public void setFill(Color c) { this.fill = c; }
        public void setBorderPaint(Color c) { this.border = c; }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth();
            int h = getHeight();

            if (fill != null) {
                g2.setColor(fill);
                g2.fillRoundRect(0, 0, w, h, radius, radius);
            }

            if (border != null) {
                g2.setColor(border);
                g2.drawRoundRect(0, 0, w - 1, h - 1, radius, radius);
            }
            g2.dispose();
            super.paintComponent(g);
        }
    }

    // ===== Buttons =====
    public static JButton primaryButton(String text) {
        JButton b = new JButton(text);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setFocusPainted(false);
        b.setBackground(PRIMARY);
        b.setForeground(Color.WHITE);
        b.setBorder(new EmptyBorder(10, 14, 10, 14));
        return b;
    }

    public static JButton ghostButton(String text) {
        JButton b = new JButton(text);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setFocusPainted(false);
        b.setBackground(WHITE);
        b.setForeground(TEXT);
        b.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(BORDER, 1, true),
                new EmptyBorder(9, 12, 9, 12)
        ));
        return b;
    }

    public static JButton iconButton(String iconText) {
        JButton b = new JButton(iconText);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setFocusPainted(false);
        b.setBackground(WHITE);
        b.setForeground(TEXT);
        b.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(BORDER, 1, true),
                new EmptyBorder(7, 9, 7, 9)
        ));
        return b;
    }

    // ===== Chips =====
    public static JComponent chip(String text) {
        JLabel l = new JLabel(text);
        l.setOpaque(true);
        l.setBackground(CHIP_BG);
        l.setForeground(CHIP_TEXT);
        l.setFont(scaled(l, Font.PLAIN, 0.92f));
        l.setBorder(new EmptyBorder(6, 10, 6, 10));
        return l;
    }

    public static JComponent chipPrimary(String text) {
        JLabel l = new JLabel(text);
        l.setOpaque(true);
        l.setBackground(highContrastEnabled() ? WHITE : new Color(0xEEF2FF));
        l.setForeground(highContrastEnabled() ? TEXT : PRIMARY_DARK);
        l.setFont(scaled(l, Font.BOLD, 0.92f));
        l.setBorder(new EmptyBorder(6, 10, 6, 10));
        return l;
    }

    private static boolean highContrastEnabled() {
        return TEXT.equals(HC_TEXT) && BORDER.equals(HC_BORDER) && BG.equals(HC_BG);
    }

    // ===== Textfield with placeholder =====
    public static JTextField searchField(String placeholder) {
        JTextField tf = new JTextField();
        tf.setText(placeholder);
        tf.setForeground(new Color(0x9CA3AF));
        tf.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(BORDER, 1, true),
                new EmptyBorder(10, 12, 10, 12)
        ));

        tf.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                if (tf.getText().equals(placeholder)) {
                    tf.setText("");
                    tf.setForeground(TEXT);
                }
            }

            @Override
            public void focusLost(FocusEvent e) {
                if (tf.getText().trim().isEmpty()) {
                    tf.setText(placeholder);
                    tf.setForeground(new Color(0x9CA3AF));
                }
            }
        });

        return tf;
    }

    // ===== ✅ Input field styling (FIX for "Cannot resolve method inputField") =====
    /**
     * Creates a styled input field matching RoomViz UI.
     * Label parameter is kept for your existing calls (even if you don't use it visually here).
     */
    public static JTextField inputField(String label, String value) {
        JTextField tf = new JTextField(value == null ? "" : value);
        tf.setOpaque(true);
        tf.setBackground(WHITE);
        tf.setForeground(TEXT);
        tf.setFont(scaled(tf, Font.PLAIN, 0.95f));
        tf.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(BORDER, 1, true),
                new EmptyBorder(10, 12, 10, 12)
        ));
        return tf;
    }

    /** Convenience overload used in some pages. */
    public static JTextField inputField(String value) {
        return inputField("", value);
    }

    // ===== ✅ Dropdown styling (FIX for "Cannot resolve method styleDropdown") =====
    public static <T> JComboBox<T> styleDropdown(JComboBox<T> combo) {
        if (combo == null) return null;

        combo.setOpaque(true);
        combo.setBackground(WHITE);
        combo.setForeground(TEXT);
        combo.setFocusable(false);

        combo.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(BORDER, 1, true),
                new EmptyBorder(8, 10, 8, 10)
        ));

        combo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(
                    JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {

                JLabel l = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

                if (list != null) {
                    list.setSelectionBackground(highContrastEnabled() ? Color.BLACK : new Color(0xEEF2FF));
                    list.setSelectionForeground(highContrastEnabled() ? Color.WHITE : TEXT);
                    list.setBackground(WHITE);
                    list.setForeground(TEXT);
                    list.setBorder(new EmptyBorder(6, 6, 6, 6));
                }

                l.setBorder(new EmptyBorder(6, 10, 6, 10));
                l.setFont(scaled(l, Font.PLAIN, 0.95f));
                return l;
            }
        });

        return combo;
    }
}
