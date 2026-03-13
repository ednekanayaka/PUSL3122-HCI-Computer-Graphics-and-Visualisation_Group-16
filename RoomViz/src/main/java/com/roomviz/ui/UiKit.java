package com.roomviz.ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;

public final class UiKit {
    private UiKit() {}

    // ===== Palette =====
    public static final Color BG = new Color(0xF6F7FB);
    public static final Color WHITE = Color.WHITE;
    public static final Color TEXT = new Color(0x111827);
    public static final Color MUTED = new Color(0x6B7280);
    public static final Color BORDER = new Color(0xE5E7EB);

    public static final Color PRIMARY = new Color(0x4F46E5);
    public static final Color PRIMARY_DARK = new Color(0x4338CA);

    public static final Color CHIP_BG = new Color(0xF3F4F6);
    public static final Color CHIP_TEXT = new Color(0x374151);

    public static final Color DANGER = new Color(0xDC2626);

    // ===== Typography helpers =====
    public static Font semibold(Font base, float size) {
        return base.deriveFont(Font.BOLD, size);
    }
    public static Font regular(Font base, float size) {
        return base.deriveFont(Font.PLAIN, size);
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

            g2.setColor(fill);
            g2.fillRoundRect(0, 0, w, h, radius, radius);

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
        b.setBackground(Color.WHITE);
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
        b.setBackground(Color.WHITE);
        b.setForeground(new Color(0x4B5563));
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
        l.setFont(l.getFont().deriveFont(Font.PLAIN, 11.5f));
        l.setBorder(new EmptyBorder(6, 10, 6, 10));
        return l;
    }

    public static JComponent chipPrimary(String text) {
        JLabel l = new JLabel(text);
        l.setOpaque(true);
        l.setBackground(new Color(0xEEF2FF));
        l.setForeground(PRIMARY_DARK);
        l.setFont(l.getFont().deriveFont(Font.BOLD, 11.5f));
        l.setBorder(new EmptyBorder(6, 10, 6, 10));
        return l;
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
            @Override public void focusGained(FocusEvent e) {
                if (placeholder.equals(tf.getText())) {
                    tf.setText("");
                    tf.setForeground(TEXT);
                }
            }

            @Override public void focusLost(FocusEvent e) {
                if (tf.getText().trim().isEmpty()) {
                    tf.setText(placeholder);
                    tf.setForeground(new Color(0x9CA3AF));
                }
            }
        });

        return tf;
    }

    public static void styleDropdown(JComboBox<?> cb) {
        cb.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(BORDER, 1, true),
                new EmptyBorder(6, 10, 6, 10)
        ));
        cb.setBackground(Color.WHITE);
    }
}
