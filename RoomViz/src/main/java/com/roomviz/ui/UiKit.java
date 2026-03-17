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
import java.util.Locale;
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
    private static final Color BASE_PRIMARY = new Color(0x5B2BFF);
    private static final Color BASE_PRIMARY_DARK = new Color(0x8E2DE2);
    private static final Color BASE_CHIP_BG = new Color(0xF3F4F6);
    private static final Color BASE_CHIP_TEXT = new Color(0x374151);
    private static final Color BASE_DANGER = new Color(0xDC2626);
    private static final Color BASE_SUCCESS = new Color(0x10B981);

    // ===== Dark Blue Palette =====
    private static final Color DB_BG = new Color(0x0B1220);
    private static final Color DB_WHITE = new Color(0x111B2E);
    private static final Color DB_TEXT = new Color(0xE5ECFF);
    private static final Color DB_MUTED = new Color(0x9DB0D3);
    private static final Color DB_BORDER = new Color(0x25324A);
    private static final Color DB_PRIMARY = new Color(0x3B82F6);
    private static final Color DB_PRIMARY_DARK = new Color(0x2563EB);
    private static final Color DB_CHIP_BG = new Color(0x17243B);
    private static final Color DB_CHIP_TEXT = new Color(0xCFE0FF);
    private static final Color DB_DANGER = new Color(0xF87171);
    private static final Color DB_SUCCESS = new Color(0x34D399);

    // ===== High Contrast Palette =====
    private static final Color HC_BG = Color.WHITE;
    private static final Color HC_WHITE = Color.WHITE;
    private static final Color HC_TEXT = Color.BLACK;
    private static final Color HC_MUTED = Color.BLACK;
    private static final Color HC_BORDER = Color.BLACK;
    private static final Color HC_PRIMARY = Color.BLACK;
    private static final Color HC_PRIMARY_DARK = Color.BLACK;
    private static final Color HC_CHIP_BG = Color.WHITE;
    private static final Color HC_CHIP_TEXT = Color.BLACK;
    private static final Color HC_DANGER = Color.BLACK;
    private static final Color HC_SUCCESS = Color.BLACK;

    // ===== Theme State (mutable) =====
    public static Color BG = BASE_BG;
    public static Color WHITE = BASE_WHITE;
    public static Color TEXT = BASE_TEXT;
    public static Color MUTED = BASE_MUTED;
    public static Color BORDER = BASE_BORDER;

    public static Color PRIMARY = BASE_PRIMARY;
    public static Color PRIMARY_DARK = BASE_PRIMARY_DARK;

    public static Color CHIP_BG = BASE_CHIP_BG;
    public static Color CHIP_TEXT = BASE_CHIP_TEXT;

    public static Color DANGER = BASE_DANGER;
    public static Color SUCCESS = BASE_SUCCESS;

    // ===== Brand Panel Colors (TopBar / Sidebar) =====
    public static Color BRAND_BG;
    public static Color BRAND_TEXT;
    public static Color BRAND_MUTED;
    public static Color BRAND_ACTIVE_BG;
    public static Color BRAND_HOVER_BG;
    public static Color BRAND_BORDER;

    // ===== Semantic UI Colors (auto-switch light/dark) =====
    public static Color CARD_HOVER       = new Color(0xFAFAFB);
    public static Color CHIP_ACTIVE_BG   = new Color(0xEEF2FF);
    public static Color CHIP_ACTIVE_TEXT  = BASE_PRIMARY_DARK;
    public static Color PILL_SUCCESS_BG   = new Color(0xDCFCE7);
    public static Color PILL_SUCCESS_FG   = new Color(0x16A34A);
    public static Color PILL_WARN_BG      = new Color(0xFFEDD5);
    public static Color PILL_WARN_FG      = new Color(0xF59E0B);
    public static Color PILL_PURPLE_BG    = new Color(0xEDE9FE);
    public static Color PILL_PURPLE_FG    = new Color(0x7C3AED);
    public static Color META_PILL_BG      = new Color(0xF3F4F6);
    public static Color META_PILL_FG      = new Color(0x374151);
    public static Color TIP_BG            = new Color(0xF8FAFC);
    public static Color TIP_BORDER        = new Color(0xE2E8F0);
    public static Color SHADOW_ALPHA      = new Color(0, 0, 0, 18);
    public static Color STATUS_DRAFT_FG   = new Color(0x2563EB);
    public static Color STEPPER_INACTIVE  = new Color(0xF3F4F6);
    public static Color ICON_BG           = new Color(0xEEF2FF);
    public static Color TOGGLE_OFF_BG     = new Color(0xE5E7EB);
    public static Color TOGGLE_OFF_FG     = new Color(0x374151);
    public static Color TOGGLE_OFF_BORDER = new Color(0xD1D5DB);

    private static boolean highContrastMode = false;
    private static boolean darkBlueMode = false;

    // ===== Apply settings globally =====
    public static void applySettings(UserSettings s) {
        boolean highContrast = (s != null) && s.isHighContrast();
        boolean darkBlue = isDarkBlueRequested(s);
        String fontSize = (s == null) ? "Small" : safe(s.getFontSize(), "Small");

        // Palette
        if (highContrast) {
            BG = HC_BG;
            WHITE = HC_WHITE;
            TEXT = HC_TEXT;
            MUTED = HC_MUTED;
            BORDER = HC_BORDER;

            PRIMARY = HC_PRIMARY;
            PRIMARY_DARK = HC_PRIMARY_DARK;
            CHIP_BG = HC_CHIP_BG;
            CHIP_TEXT = HC_CHIP_TEXT;
            DANGER = HC_DANGER;
            SUCCESS = HC_SUCCESS;

            BRAND_BG = HC_WHITE;
            BRAND_TEXT = HC_TEXT;
            BRAND_MUTED = HC_MUTED;
            BRAND_ACTIVE_BG = HC_TEXT; // we will invert text on active
            BRAND_HOVER_BG = new Color(0, 0, 0, 10);
            BRAND_BORDER = HC_BORDER;

            highContrastMode = true;
            darkBlueMode = false;
        } else if (darkBlue) {
            BG = DB_BG;
            WHITE = DB_WHITE;
            TEXT = DB_TEXT;
            MUTED = DB_MUTED;
            BORDER = DB_BORDER;

            PRIMARY = DB_PRIMARY;
            PRIMARY_DARK = DB_PRIMARY_DARK;
            CHIP_BG = DB_CHIP_BG;
            CHIP_TEXT = DB_CHIP_TEXT;
            DANGER = DB_DANGER;
            SUCCESS = DB_SUCCESS;

            BRAND_BG = DB_BG;
            BRAND_TEXT = DB_TEXT;
            BRAND_MUTED = DB_MUTED;
            BRAND_ACTIVE_BG = new Color(0x17243B);
            BRAND_HOVER_BG = new Color(0x111B2E);
            BRAND_BORDER = DB_BORDER;

            highContrastMode = false;
            darkBlueMode = true;
        } else {
            BG = BASE_BG;
            WHITE = BASE_WHITE;
            TEXT = BASE_TEXT;
            MUTED = BASE_MUTED;
            BORDER = BASE_BORDER;

            PRIMARY = BASE_PRIMARY;
            PRIMARY_DARK = BASE_PRIMARY_DARK;
            CHIP_BG = BASE_CHIP_BG;
            CHIP_TEXT = BASE_CHIP_TEXT;
            DANGER = BASE_DANGER;
            SUCCESS = BASE_SUCCESS;

            // Light mode uses the darker blue variant for brand panels
            BRAND_BG = new Color(0x303F9F); // A slightly darker blue for nav and sidebar
            BRAND_TEXT = Color.WHITE;
            BRAND_MUTED = new Color(255, 255, 255, 180);
            BRAND_ACTIVE_BG = new Color(255, 255, 255, 40);
            BRAND_HOVER_BG = new Color(255, 255, 255, 20);
            BRAND_BORDER = new Color(255, 255, 255, 40);

            highContrastMode = false;
            darkBlueMode = false;
        }

        // Semantic colors (depend on palette selected above)
        if (darkBlue) {
            CARD_HOVER       = new Color(0x162033);
            CHIP_ACTIVE_BG   = new Color(0x1E3A8A);
            CHIP_ACTIVE_TEXT  = new Color(0xBFDBFE);
            PILL_SUCCESS_BG   = new Color(0x064E3B);
            PILL_SUCCESS_FG   = new Color(0x34D399);
            PILL_WARN_BG      = new Color(0x78350F);
            PILL_WARN_FG      = new Color(0xFBBF24);
            PILL_PURPLE_BG    = new Color(0x3B0764);
            PILL_PURPLE_FG    = new Color(0xA78BFA);
            META_PILL_BG      = new Color(0x1E293B);
            META_PILL_FG      = new Color(0xCBD5E1);
            TIP_BG            = new Color(0x0F172A);
            TIP_BORDER        = new Color(0x334155);
            SHADOW_ALPHA      = new Color(0, 0, 0, 50);
            STATUS_DRAFT_FG   = new Color(0x60A5FA);
            STEPPER_INACTIVE  = new Color(0x1E293B);
            ICON_BG           = new Color(0x1E3A8A);
            TOGGLE_OFF_BG     = new Color(0x1F2937);
            TOGGLE_OFF_FG     = new Color(0xCBD5E1);
            TOGGLE_OFF_BORDER = new Color(0x334155);
        } else if (highContrast) {
            CARD_HOVER       = HC_BG;
            CHIP_ACTIVE_BG   = HC_WHITE;
            CHIP_ACTIVE_TEXT  = HC_TEXT;
            PILL_SUCCESS_BG   = HC_WHITE;
            PILL_SUCCESS_FG   = HC_TEXT;
            PILL_WARN_BG      = HC_WHITE;
            PILL_WARN_FG      = HC_TEXT;
            PILL_PURPLE_BG    = HC_WHITE;
            PILL_PURPLE_FG    = HC_TEXT;
            META_PILL_BG      = HC_WHITE;
            META_PILL_FG      = HC_TEXT;
            TIP_BG            = HC_WHITE;
            TIP_BORDER        = HC_BORDER;
            SHADOW_ALPHA      = new Color(0, 0, 0, 0);
            STATUS_DRAFT_FG   = HC_TEXT;
            STEPPER_INACTIVE  = HC_WHITE;
            ICON_BG           = HC_WHITE;
            TOGGLE_OFF_BG     = HC_BG;
            TOGGLE_OFF_FG     = HC_TEXT;
            TOGGLE_OFF_BORDER = HC_BORDER;
        } else {
            CARD_HOVER       = new Color(0xFAFAFB);
            CHIP_ACTIVE_BG   = new Color(0xEEF2FF);
            CHIP_ACTIVE_TEXT  = BASE_PRIMARY_DARK;
            PILL_SUCCESS_BG   = new Color(0xDCFCE7);
            PILL_SUCCESS_FG   = new Color(0x16A34A);
            PILL_WARN_BG      = new Color(0xFFEDD5);
            PILL_WARN_FG      = new Color(0xF59E0B);
            PILL_PURPLE_BG    = new Color(0xEDE9FE);
            PILL_PURPLE_FG    = new Color(0x7C3AED);
            META_PILL_BG      = new Color(0xF3F4F6);
            META_PILL_FG      = new Color(0x374151);
            TIP_BG            = new Color(0xF8FAFC);
            TIP_BORDER        = new Color(0xE2E8F0);
            SHADOW_ALPHA      = new Color(0, 0, 0, 18);
            STATUS_DRAFT_FG   = new Color(0x2563EB);
            STEPPER_INACTIVE  = new Color(0xF3F4F6);
            ICON_BG           = new Color(0xEEF2FF);
            TOGGLE_OFF_BG     = new Color(0xE5E7EB);
            TOGGLE_OFF_FG     = new Color(0x374151);
            TOGGLE_OFF_BORDER = new Color(0xD1D5DB);
        }

        // Font scale
        float scale = 1.0f; // Small
        if ("Medium".equalsIgnoreCase(fontSize)) scale = 1.10f;
        if ("Large".equalsIgnoreCase(fontSize)) scale = 1.20f;

        applyGlobalFontScale(scale);
    }

    public static boolean isHighContrastMode() {
        return highContrastMode;
    }

    public static boolean isDarkBlueMode() {
        return darkBlueMode;
    }

    private static boolean isDarkBlueRequested(UserSettings s) {
        if (s == null) return false;
        String mode = safe(s.getThemeMode(), "light")
                .toLowerCase(Locale.ENGLISH)
                .replace('-', '_')
                .replace(' ', '_');
        return "dark_blue".equals(mode) || "dark".equals(mode);
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
                if (fill.equals(UiKit.WHITE) && !UiKit.isHighContrastMode() && !UiKit.isDarkBlueMode()) {
                    g2.setColor(new Color(255, 255, 255, 170)); // Glass effect
                } else {
                    g2.setColor(fill);
                }
                g2.fillRoundRect(0, 0, w, h, radius, radius);
            }

            if (border != null) {
                if (border.equals(UiKit.BORDER) && !UiKit.isHighContrastMode() && !UiKit.isDarkBlueMode()) {
                    g2.setColor(new Color(255, 255, 255, 200)); // Lighter border
                } else {
                    g2.setColor(border);
                }
                g2.drawRoundRect(0, 0, w - 1, h - 1, radius, radius);
            }
            g2.dispose();
            super.paintComponent(g);
        }
    }

    // ===== Buttons =====
    public static class RoundButton extends JButton {
        private Color color1, color2;
        private int radius = 16;
        private boolean hovered = false;

        public RoundButton(String text) {
            super(text);
            setOpaque(false);
            setContentAreaFilled(false);
            setFocusPainted(false);
            setBorder(new EmptyBorder(10, 18, 10, 18));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

            addMouseListener(new java.awt.event.MouseAdapter() {
                public void mouseEntered(java.awt.event.MouseEvent e) { hovered = true; repaint(); }
                public void mouseExited(java.awt.event.MouseEvent e) { hovered = false; repaint(); }
            });
        }

        public void setGradient(Color c1, Color c2) {
            this.color1 = c1;
            this.color2 = c2;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();

            if (color1 != null && color2 != null) {
                Color c1 = hovered && isEnabled() ? color1.brighter() : color1;
                Color c2 = hovered && isEnabled() ? color2.brighter() : color2;
                GradientPaint gp = new GradientPaint(0, 0, c1, w, h, c2);
                g2.setPaint(gp);
            } else {
                Color bg = getBackground();
                if (hovered && isEnabled()) {
                    if (bg.equals(UiKit.WHITE)) {
                        bg = new Color(243, 244, 246);
                    } else if (bg.equals(UiKit.BG)) {
                        bg = bg.darker();
                    } else {
                        bg = bg.brighter();
                    }
                }
                
                // Glass effect for ghost buttons in light mode
                if (getBackground().equals(UiKit.WHITE) && !UiKit.isHighContrastMode() && !UiKit.isDarkBlueMode()) {
                    bg = hovered ? new Color(255, 255, 255, 220) : new Color(255, 255, 255, 140);
                }
                g2.setColor(bg);
            }

            g2.fillRoundRect(0, 0, w, h, radius, radius);

            // Subtle border
            if (color1 == null) {
                if (!UiKit.isHighContrastMode() && !UiKit.isDarkBlueMode() && getBackground().equals(UiKit.WHITE)) {
                    g2.setColor(new Color(255, 255, 255, 200));
                } else {
                    g2.setColor(UiKit.BORDER);
                }
                g2.drawRoundRect(0, 0, w - 1, h - 1, radius, radius);
            }

            g2.dispose();
            super.paintComponent(g);
        }
    }

    public static JButton primaryButton(String text) {
        RoundButton b = new RoundButton(text);
        if (highContrastEnabled()) {
            b.setBackground(TEXT);
            b.setGradient(null, null);
        } else {
            b.setBackground(PRIMARY);
            b.setGradient(PRIMARY, PRIMARY_DARK);
        }
        b.setForeground(Color.WHITE);
        return b;
    }

    public static JButton ghostButton(String text) {
        RoundButton b = new RoundButton(text);
        b.setBackground(WHITE);
        b.setForeground(TEXT);
        b.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(BORDER, 1, true),
                new EmptyBorder(9, 12, 9, 12)
        ));
        return b;
    }

    public static JButton iconButton(String iconText) {
        RoundButton b = new RoundButton(iconText);
        b.setFont(FontAwesome.solid(13f));
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
        JLabel l = new JLabel(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getBackground());
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        l.setOpaque(false);
        l.setBackground(CHIP_BG);
        l.setForeground(CHIP_TEXT);
        l.setFont(scaled(l, Font.PLAIN, 0.92f));
        l.setBorder(new EmptyBorder(6, 10, 6, 10));
        return l;
    }

    public static JComponent chipPrimary(String text) {
        JLabel l = new JLabel(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getBackground());
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        l.setOpaque(false);
        if (highContrastEnabled()) {
            l.setBackground(WHITE);
            l.setForeground(TEXT);
        } else if (isDarkBlueMode()) {
            l.setBackground(new Color(0x1D4ED8));
            l.setForeground(new Color(0xEAF2FF));
        } else {
            l.setBackground(new Color(0xEEF2FF));
            l.setForeground(PRIMARY_DARK);
        }
        l.setFont(scaled(l, Font.BOLD, 0.92f));
        l.setBorder(new EmptyBorder(6, 10, 6, 10));
        return l;
    }

    private static boolean highContrastEnabled() {
        return highContrastMode;
    }

    private static Color placeholderColor() {
        if (highContrastEnabled()) return TEXT;
        if (isDarkBlueMode()) return new Color(0x8FA5CC);
        return new Color(0x9CA3AF);
    }

    // ===== Textfield with placeholder =====
    public static JTextField searchField(String placeholder) {
        return searchFieldWithIcon(placeholder).field;
    }

    public static class SearchResult {
        public JPanel panel;
        public JTextField field;
    }

    public static SearchResult searchFieldWithIcon(String placeholder) {
        JPanel wrapper = new JPanel(new BorderLayout(8, 0));
        wrapper.setOpaque(true);
        wrapper.setBackground(WHITE);
        wrapper.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(BORDER, 1, true),
                new EmptyBorder(8, 12, 8, 12)
        ));

        JLabel icon = new JLabel(FontAwesome.SEARCH);
        icon.setForeground(placeholderColor());
        icon.setFont(FontAwesome.solid(14f));

        JTextField tf = new JTextField();
        tf.setText(placeholder);
        tf.setBorder(null);
        tf.setOpaque(false);
        tf.setForeground(placeholderColor());
        tf.setFont(scaled(tf, Font.PLAIN, 0.95f));

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
                    tf.setForeground(placeholderColor());
                }
            }
        });

        wrapper.add(icon, BorderLayout.WEST);
        wrapper.add(tf, BorderLayout.CENTER);

        SearchResult res = new SearchResult();
        res.panel = wrapper;
        res.field = tf;
        return res;
    }

    // =====  Input field styling  =====
    /**
     * Creates a styled input field matching RoomViz UI.
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

    // ===== Dropdown styling (FIX for "Cannot resolve method styleDropdown") =====
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
                    Color selectionBg = highContrastEnabled()
                            ? Color.BLACK
                            : (isDarkBlueMode() ? new Color(0x1E3A8A) : new Color(0xEEF2FF));
                    Color selectionFg = highContrastEnabled() ? Color.WHITE : TEXT;
                    list.setSelectionBackground(selectionBg);
                    list.setSelectionForeground(selectionFg);
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
