package com.roomviz.screens;

import com.roomviz.app.Router;
import com.roomviz.app.ScreenKeys;
import com.roomviz.data.Session;
import com.roomviz.model.User;
import com.roomviz.ui.FontAwesome;
import com.roomviz.ui.UiKit;

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
    private final Map<String, NavButton> buttonMap = new LinkedHashMap<>();
    private NavButton activeBtn;

    // Role context (null-safe)
    private final Session session;

    /**
     * Backward-compatible constructor (if older code calls Sidebar(router, onTitleChange)).
     * If session is not provided, we assume ADMIN to avoid accidentally hiding tools.
     */
    public Sidebar(Router router, Consumer<String> onTitleChange) {
        this(router, onTitleChange, null);
    }

    /**
     * - CUSTOMER: Design Library, 2D View (read-only), 3D View (read-only), Settings
     * - ADMIN: full access (incl. Customers management)
     */
    public Sidebar(Router router, Consumer<String> onTitleChange, Session session) {
        this.onTitleChange = onTitleChange;
        this.session = session;

        setPreferredSize(new Dimension(248, 0));
        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(14, 12, 14, 12));
        setOpaque(false);

        // ---------- Top spacer ----------
        JPanel top = new JPanel();
        top.setOpaque(false);
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
        top.setBorder(new EmptyBorder(2, 0, 8, 0));

        // ---------- Nav ----------
        JPanel nav = new JPanel();
        nav.setOpaque(false);
        nav.setLayout(new BoxLayout(nav, BoxLayout.Y_AXIS));

        boolean customer = isCustomer();

        Map<String, NavItem> items = new LinkedHashMap<>();

        if (!customer) {
            // Admin-only items
            items.put(ScreenKeys.DASHBOARD, new NavItem("Dashboard", FontAwesome.GAUGE, "Dashboard"));

            // Customers management (Admin-only)
            // NOTE: Ensure ScreenKeys.CUSTOMERS exists: public static final String CUSTOMERS = "customers";
            items.put(ScreenKeys.CUSTOMERS, new NavItem("Customers", FontAwesome.USERS, "Customers"));
        }

        // Shared items (Admin + Customer)
        items.put(ScreenKeys.DESIGN_LIBRARY, new NavItem("Design Library", FontAwesome.SWATCHBOOK, "Design Library"));

        if (!customer) {
            // Admin-only items
            items.put(ScreenKeys.NEW_DESIGN, new NavItem("New Design", FontAwesome.PLUS, "New Design Wizard"));
        }

        // Customer sees "2D View" label (read-only), Admin sees "2D Planner"
        items.put(
                ScreenKeys.PLANNER_2D,
                customer
                        ? new NavItem("2D View", FontAwesome.VECTOR_SQUARE, "2D View")
                        : new NavItem("2D Planner", FontAwesome.VECTOR_SQUARE, "2D Planner")
        );

        if (!customer) {
            // Admin-only items
            items.put(ScreenKeys.SHADING_COLOR, new NavItem("Shading & Colour", FontAwesome.PALETTE, "Shading & Colour"));
        }

        // Shared items
        items.put(ScreenKeys.VIEW_3D, new NavItem("3D View", FontAwesome.CUBE, "3D View"));
        items.put(ScreenKeys.SETTINGS, new NavItem("Settings", FontAwesome.GEAR, "Settings"));

        int i = 0;
        int size = items.size();
        for (Map.Entry<String, NavItem> e : items.entrySet()) {
            final String key = e.getKey();
            final NavItem item = e.getValue();

            NavButton btn = new NavButton(item.label, item.iconText);
            btn.addActionListener(ev -> go(router, key, item.title));
            nav.add(btn);
            if (i < size - 1) nav.add(Box.createVerticalStrut(6));
            buttonMap.put(key, btn);
            i++;
        }

        // Listen for route changes to update active state
        if (router != null) {
            router.addListener(key -> {
                if (buttonMap.containsKey(key)) {
                    setActive(buttonMap.get(key));
                }
            });
        }

        // ---------- Bottom ----------
        JPanel bottom = new JPanel();
        bottom.setOpaque(false);
        bottom.setLayout(new BoxLayout(bottom, BoxLayout.Y_AXIS));
        bottom.setBorder(new EmptyBorder(10, 2, 0, 2));

        JPanel tipCard = new JPanel();
        tipCard.setOpaque(true);
        tipCard.setBackground(isHighContrast() ? UiKit.WHITE : new Color(255, 255, 255, 25)); // Translucent white for blue background
        tipCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(isHighContrast() ? UiKit.BORDER : new Color(255, 255, 255, 40), 1, true),
                new EmptyBorder(9, 10, 9, 10)
        ));
        tipCard.setLayout(new BoxLayout(tipCard, BoxLayout.Y_AXIS));

        JLabel tipTitle = new JLabel("Quick Tip");
        tipTitle.setForeground(isHighContrast() ? UiKit.TEXT : Color.WHITE);
        tipTitle.setFont(UiKit.scaled(tipTitle, Font.BOLD, 0.84f));

        JLabel tipText = new JLabel("<html>Pick a design from <b>Design Library</b> before opening tools.</html>");
        tipText.setForeground(isHighContrast() ? UiKit.MUTED : new Color(255, 255, 255, 200));
        tipText.setFont(UiKit.scaled(tipText, Font.PLAIN, 0.82f));

        tipCard.add(tipTitle);
        tipCard.add(Box.createVerticalStrut(3));
        tipCard.add(tipText);
        bottom.add(tipCard);

        add(top, BorderLayout.NORTH);
        add(nav, BorderLayout.CENTER);
        add(bottom, BorderLayout.SOUTH);
    }

    private boolean isCustomer() {
        // Null-safe:
        // - If session/user missing -> treat as ADMIN to avoid accidental lockout
        if (session == null) return false;
        User u = session.getCurrentUser();
        if (u == null) return false;
        return u.isCustomer();
    }

    private static boolean isHighContrast() {
        return UiKit.isHighContrastMode();
    }

    private void go(Router router, String key, String title) {
        if (router != null) router.show(key);
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

    // --- Model ---

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

    // --- UI ---

    private static class NavButton extends JButton {
        private boolean active = false;
        private boolean hover = false;

        private final JLabel icon;
        private final JLabel text;
        private Color iconBg;
        private Color iconBorder;

        NavButton(String label, String iconText) {
            setLayout(new BorderLayout(10, 0));
            setOpaque(false);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setPreferredSize(new Dimension(0, 44));
            setBorder(new EmptyBorder(0, 0, 0, 0));

            icon = new JLabel(iconText);
            icon.setHorizontalAlignment(SwingConstants.CENTER);
            icon.setPreferredSize(new Dimension(24, 24));
            icon.setFont(FontAwesome.solid(13f));
            icon.setForeground(isHighContrast() ? UiKit.MUTED : UiKit.BRAND_MUTED);
            icon.setOpaque(false);

            text = new JLabel(label);
            text.setFont(UiKit.scaled(text, Font.PLAIN, 0.96f));
            text.setForeground(isHighContrast() ? UiKit.TEXT : UiKit.BRAND_TEXT);

            JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
            left.setOpaque(false);
            left.add(icon);
            left.add(text);

            add(left, BorderLayout.CENTER);

            addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { hover = true; applyVisualState(); repaint(); }
                @Override public void mouseExited(MouseEvent e) { hover = false; applyVisualState(); repaint(); }
            });

            applyVisualState();
        }

        void setActive(boolean v) {
            this.active = v;
            applyVisualState();
            repaint();
        }

        private void applyVisualState() {
            if (active) {
                text.setForeground(activeText());
                text.setFont(UiKit.scaled(text, Font.BOLD, 0.96f));
                icon.setForeground(activeText());
                iconBg = isHighContrast() ? UiKit.WHITE
                        : UiKit.BRAND_ACTIVE_BG;
                iconBorder = isHighContrast() ? UiKit.BORDER
                        : UiKit.BRAND_BORDER;
            } else if (hover) {
                text.setForeground(isHighContrast() ? UiKit.TEXT : UiKit.BRAND_TEXT);
                text.setFont(UiKit.scaled(text, Font.PLAIN, 0.96f));
                icon.setForeground(isHighContrast() ? UiKit.TEXT : UiKit.BRAND_TEXT);
                iconBg = isHighContrast() ? UiKit.WHITE
                        : UiKit.BRAND_HOVER_BG;
                iconBorder = isHighContrast() ? UiKit.BORDER
                        : UiKit.BRAND_BORDER;
            } else {
                text.setForeground(isHighContrast() ? UiKit.TEXT : UiKit.BRAND_TEXT);
                text.setFont(UiKit.scaled(text, Font.PLAIN, 0.96f));
                icon.setForeground(isHighContrast() ? UiKit.MUTED : UiKit.BRAND_MUTED);
                iconBg = isHighContrast() ? UiKit.WHITE
                        : new Color(0, 0, 0, 0); // Transparent for inactive
                iconBorder = isHighContrast() ? UiKit.BORDER
                        : new Color(0, 0, 0, 0); // Transparent for inactive
            }
        }

        private Color activeText() {
            return isHighContrast() ? UiKit.TEXT
                    : Color.WHITE;
        }

        private Color activeBg() {
            return isHighContrast() ? UiKit.WHITE
                    : UiKit.BRAND_ACTIVE_BG;
        }

        private Color activeBorder() {
            return isHighContrast() ? UiKit.BORDER
                    : UiKit.BRAND_BORDER;
        }

        private Color hoverBg() {
            return isHighContrast() ? UiKit.WHITE
                    : UiKit.BRAND_HOVER_BG;
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();

            if (active) {
                g2.setColor(activeBg());
                g2.fillRoundRect(0, 0, w, h, 14, 14);

                g2.setColor(activeBorder());
                g2.drawRoundRect(0, 0, w - 1, h - 1, 14, 14);

                g2.setColor(activeText());
                g2.fillRoundRect(1, 8, 3, h - 16, 3, 3);
            } else if (hover) {
                g2.setColor(hoverBg());
                g2.fillRoundRect(0, 0, w, h, 14, 14);
                g2.setColor(isHighContrast() ? UiKit.BORDER
                        : UiKit.BRAND_BORDER);
                g2.drawRoundRect(0, 0, w - 1, h - 1, 14, 14);
            }

            Rectangle r = icon.getBounds();
            if (r.width > 0 && r.height > 0) {
                g2.setColor(iconBg);
                g2.fillRoundRect(r.x, r.y, r.width, r.height, 8, 8);
                g2.setColor(iconBorder);
                g2.drawRoundRect(r.x, r.y, r.width - 1, r.height - 1, 8, 8);
            }

            g2.dispose();
            super.paintComponent(g);
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();

        // Sidebar background
        g2.setColor(UiKit.BRAND_BG);
        g2.fillRect(0, 0, w, h);

        // Right divider line (subtle over the blue)
        g2.setColor(isHighContrast() ? UiKit.BORDER : UiKit.BRAND_BORDER);
        g2.drawLine(w - 1, 0, w - 1, h);

        g2.dispose();
        super.paintComponent(g);
    }
}