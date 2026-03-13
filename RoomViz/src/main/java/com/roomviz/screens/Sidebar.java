package com.roomviz.screens;

import com.roomviz.app.AppFrame;
import com.roomviz.app.Router;
import com.roomviz.app.ScreenKeys;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

public class Sidebar extends JPanel {

    private final Consumer<String> onTitleChange;

    // ---- Palette (match DashboardPage / Figma-ish) ----
    private static final Color BG = Color.WHITE;
    private static final Color TEXT = new Color(0x111827);
    private static final Color MUTED = new Color(0x6B7280);
    private static final Color BORDER = new Color(0xE5E7EB);

    private static final Color ACTIVE_BG = new Color(0xEEF2FF);      // indigo-50
    private static final Color ACTIVE_BORDER = new Color(0xC7D2FE);  // indigo-200
    private static final Color ACTIVE_TEXT = new Color(0x4338CA);    // indigo-700
    private static final Color HOVER_BG = new Color(0xF3F4F6);       // gray-100

    private final Map<String, NavButton> buttonMap = new LinkedHashMap<>();
    private NavButton activeBtn;

    public Sidebar(AppFrame frame, Router router, Consumer<String> onTitleChange) {
        this.onTitleChange = onTitleChange;

        setPreferredSize(new Dimension(240, 0)); // ✅ keep same width
        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(16, 12, 16, 12));
        setBackground(BG);

        // ---------- Top brand ----------
        JPanel top = new JPanel();
        top.setOpaque(false);
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));

        JPanel brandRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        brandRow.setOpaque(false);

        JLabel brandIcon = new JLabel("\u25A3"); // simple "app" icon
        brandIcon.setForeground(new Color(0x4F46E5));
        brandIcon.setFont(brandIcon.getFont().deriveFont(Font.BOLD, 18f));

        JLabel brand = new JLabel("RoomViz");
        brand.setForeground(TEXT);
        brand.setFont(brand.getFont().deriveFont(Font.BOLD, 18f));

        brandRow.add(brandIcon);
        brandRow.add(brand);

        JLabel subtitle = new JLabel("Dashboard");
        subtitle.setForeground(MUTED);
        subtitle.setFont(subtitle.getFont().deriveFont(Font.PLAIN, 11.5f));
        subtitle.setBorder(new EmptyBorder(6, 2, 0, 0));

        top.add(brandRow);
        top.add(subtitle);

        // ---------- Nav ----------
        JPanel nav = new JPanel();
        nav.setOpaque(false);
        nav.setLayout(new GridLayout(0, 1, 0, 8));
        nav.setBorder(new EmptyBorder(16, 0, 0, 0));

        // ✅ Removed Projects tab completely
        Map<String, NavItem> items = new LinkedHashMap<>();
        items.put(ScreenKeys.DASHBOARD,      new NavItem("Dashboard", "\u25A6", "Dashboard"));
        items.put(ScreenKeys.DESIGN_LIBRARY, new NavItem("Design Library", "\u2630", "Design Library"));
        items.put(ScreenKeys.NEW_DESIGN,     new NavItem("New Design", "+", "New Design Wizard"));
        items.put(ScreenKeys.PLANNER_2D,     new NavItem("2D Planner", "\u25AD", "2D Planner"));
        items.put(ScreenKeys.SHADING_COLOR,  new NavItem("Shading & Colour", "\u25CF", "Shading & Colour"));
        items.put(ScreenKeys.VIEW_3D,        new NavItem("3D View", "\u25B3", "3D View"));
        items.put(ScreenKeys.SETTINGS,       new NavItem("Settings", "\u2699", "Settings"));

        for (Map.Entry<String, NavItem> e : items.entrySet()) {
            final String key = e.getKey();
            final NavItem item = e.getValue();

            NavButton btn = new NavButton(item.label, item.iconText);
            btn.addActionListener(ev -> {
                // setActive(btn);  <-- Removed, let the router callback handle it
                go(router, key, item.title);
            });
            nav.add(btn);
            buttonMap.put(key, btn);
        }

        // Listen for route changes to update active state
        router.addListener(key -> {
            if (buttonMap.containsKey(key)) {
                setActive(buttonMap.get(key));
            }
        });

        // ---------- Bottom: logout ----------
        JPanel bottom = new JPanel();
        bottom.setOpaque(false);
        bottom.setLayout(new BoxLayout(bottom, BoxLayout.Y_AXIS));
        bottom.setBorder(new EmptyBorder(12, 0, 0, 0));

        JButton logout = new JButton("Logout");
        logout.setFocusPainted(false);
        logout.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        logout.setHorizontalAlignment(SwingConstants.CENTER);
        logout.setFont(logout.getFont().deriveFont(Font.PLAIN, 12.2f));
        logout.setForeground(new Color(0xDC2626)); // red-ish
        logout.setBackground(Color.WHITE);
        logout.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER, 1, true),
                new EmptyBorder(10, 12, 10, 12)
        ));
        logout.addActionListener(e -> frame.goToLogin());

        // subtle hover for logout
        logout.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) {
                logout.setBackground(new Color(0xFEF2F2)); // red-50
            }
            @Override public void mouseExited(MouseEvent e) {
                logout.setBackground(Color.WHITE);
            }
        });

        bottom.add(Box.createVerticalGlue());
        bottom.add(logout);

        add(top, BorderLayout.NORTH);
        add(nav, BorderLayout.CENTER);
        add(bottom, BorderLayout.SOUTH);
    }

    private void go(Router router, String key, String title) {
        router.show(key);
        if (onTitleChange != null) onTitleChange.accept(title);
    }

    public void setActiveKey(String key) {
        if (buttonMap.containsKey(key)) {
            setActive(buttonMap.get(key));
        }
    }

    private void setActive(NavButton btn) {
        if (activeBtn != null) activeBtn.setActive(false);
        activeBtn = btn;
        if (activeBtn != null) activeBtn.setActive(true);
        repaint();
    }

    /* ===================== Small data model ===================== */

    private static class NavItem {
        final String label;
        final String iconText;
        final String title;
        NavItem(String label, String iconText, String title) {
            this.label = label;
            this.iconText = iconText;
            this.title = title;
        }
    }

    /* ===================== Custom styled nav button ===================== */

    private static class NavButton extends JButton {
        private boolean active = false;
        private boolean hover = false;

        private final JLabel icon;
        private final JLabel text;

        NavButton(String label, String iconText) {
            setLayout(new BorderLayout(10, 0));
            setOpaque(false);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setPreferredSize(new Dimension(0, 44));

            icon = new JLabel(iconText);
            icon.setHorizontalAlignment(SwingConstants.CENTER);
            icon.setPreferredSize(new Dimension(28, 28));
            icon.setFont(icon.getFont().deriveFont(Font.BOLD, 12.5f));
            icon.setForeground(MUTED);

            text = new JLabel(label);
            text.setFont(text.getFont().deriveFont(Font.PLAIN, 12.8f));
            text.setForeground(TEXT);

            JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
            left.setOpaque(false);
            left.add(icon);
            left.add(text);

            add(left, BorderLayout.CENTER);

            addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { hover = true; repaint(); }
                @Override public void mouseExited(MouseEvent e) { hover = false; repaint(); }
            });
        }

        void setActive(boolean v) {
            this.active = v;

            if (active) {
                text.setForeground(ACTIVE_TEXT);
                icon.setForeground(ACTIVE_TEXT);
                text.setFont(text.getFont().deriveFont(Font.BOLD, 12.8f));
            } else {
                text.setForeground(TEXT);
                icon.setForeground(MUTED);
                text.setFont(text.getFont().deriveFont(Font.PLAIN, 12.8f));
            }
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();

            // background
            if (active) {
                g2.setColor(ACTIVE_BG);
                g2.fillRoundRect(0, 0, w, h, 14, 14);

                g2.setColor(ACTIVE_BORDER);
                g2.drawRoundRect(0, 0, w - 1, h - 1, 14, 14);
            } else if (hover) {
                g2.setColor(HOVER_BG);
                g2.fillRoundRect(0, 0, w, h, 14, 14);
            }

            g2.dispose();
            super.paintComponent(g);
        }
    }
}
