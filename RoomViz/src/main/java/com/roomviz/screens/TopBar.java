package com.roomviz.screens;

import com.roomviz.app.AppFrame;
import com.roomviz.data.Session;
import com.roomviz.model.User;
import com.roomviz.ui.FontAwesome;
import com.roomviz.ui.UiKit;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;

/**
 * Top navigation bar with app brand and user pill.
 */
public class TopBar extends JPanel {

    private final AppFrame frame;
    private final Session session;

    // user pill
    private final JLabel nameLabel = new JLabel("User");
    private final JLabel emailLabel = new JLabel("user@email.com");
    private final BadgeLabel avatarLabel = new BadgeLabel("U");

    public TopBar(AppFrame frame, Session session) {
        this.frame = frame;
        this.session = session;

        setLayout(new BorderLayout());
        setOpaque(true);
        setBackground(UiKit.BRAND_BG);
        setBorder(new EmptyBorder(6, 12, 6, 12));

        // Left
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 10));
        left.setOpaque(false);
        left.setBorder(new EmptyBorder(2, 6, 2, 6));
        left.add(buildAppBrand());

        // Right
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 8));
        right.setOpaque(false);
        right.setBorder(new EmptyBorder(0, 10, 0, 4));

        right.add(buildUserPill());
        right.add(buildLogoutBtn());

        add(left, BorderLayout.WEST);
        add(right, BorderLayout.EAST);

        refreshUserFromSession();
    }

    private JPanel buildUserPill() {
        Color pillFill = isHighContrast()
                ? UiKit.WHITE
                : UiKit.BRAND_HOVER_BG;
        Color pillBorder = isHighContrast()
                ? UiKit.BORDER
                : UiKit.BRAND_BORDER;

        UiKit.RoundedPanel pill = new UiKit.RoundedPanel(
                14,
                pillFill
        );
        pill.setBorderPaint(pillBorder);
        pill.setBorder(new EmptyBorder(6, 10, 6, 10));
        pill.setLayout(new BoxLayout(pill, BoxLayout.X_AXIS));

        JPanel textCol = new JPanel();
        textCol.setOpaque(false);
        textCol.setLayout(new BoxLayout(textCol, BoxLayout.Y_AXIS));
        textCol.setBorder(new EmptyBorder(1, 0, 1, 0));

        JPanel nameRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        nameRow.setOpaque(false);

        nameLabel.setForeground(isHighContrast() ? UiKit.TEXT : UiKit.BRAND_TEXT);
        nameLabel.setFont(UiKit.scaled(nameLabel, Font.BOLD, 0.95f));

        JLabel statusDot = new JLabel(FontAwesome.CIRCLE);
        statusDot.setForeground(isHighContrast() ? UiKit.TEXT : new Color(0x34D399));
        statusDot.setFont(FontAwesome.solid(6f));

        emailLabel.setForeground(isHighContrast() ? UiKit.TEXT : UiKit.BRAND_MUTED);
        emailLabel.setFont(UiKit.scaled(emailLabel, Font.PLAIN, 0.89f));

        nameRow.add(nameLabel);
        nameRow.add(statusDot);
        textCol.add(nameRow);
        textCol.add(Box.createVerticalStrut(1));
        textCol.add(emailLabel);

        pill.add(avatarLabel);
        pill.add(Box.createHorizontalStrut(10));
        pill.add(textCol);

        return pill;
    }

    private JPanel buildAppBrand() {
        JPanel brand = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        brand.setOpaque(false);
        brand.setBorder(new EmptyBorder(0, 4, 0, 4));

        JLabel badge = new JLabel("RV", SwingConstants.CENTER);
        badge.setOpaque(true);
        badge.setBackground(isHighContrast() ? UiKit.WHITE : Color.WHITE);
        badge.setForeground(isHighContrast() ? UiKit.TEXT : UiKit.BRAND_BG);
        badge.setFont(UiKit.scaled(badge, Font.BOLD, 0.95f));
        badge.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(isHighContrast() ? UiKit.BORDER : Color.WHITE, 1, true),
                new EmptyBorder(3, 7, 3, 7)
        ));

        JLabel name = new JLabel("RoomViz");
        name.setForeground(isHighContrast() ? UiKit.TEXT : UiKit.BRAND_TEXT);
        name.setFont(UiKit.scaled(name, Font.BOLD, 1.05f));

        JLabel sub = new JLabel("Studio");
        sub.setForeground(isHighContrast() ? UiKit.MUTED : UiKit.BRAND_MUTED);
        sub.setFont(UiKit.scaled(sub, Font.PLAIN, 0.90f));

        JPanel stack = new JPanel();
        stack.setOpaque(false);
        stack.setLayout(new BoxLayout(stack, BoxLayout.Y_AXIS));
        stack.add(name);
        stack.add(sub);

        brand.add(badge);
        brand.add(stack);
        return brand;
    }

    private JButton buildLogoutBtn() {
        if (isHighContrast()) {
            JButton b = UiKit.ghostButton("Logout");
            b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            b.addActionListener(e -> {
                if (session != null) session.logout();
                frame.goToLogin();
            });
            return b;
        }

        UiKit.RoundButton b = new UiKit.RoundButton("Logout");
        b.setForeground(Color.WHITE);
        b.setGradient(new Color(0xEF4444), new Color(0xDC2626));
        b.setBorder(new EmptyBorder(10, 14, 10, 14));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        b.addActionListener(e -> {
            if (session != null) session.logout();
            frame.goToLogin();
        });

        b.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseEntered(java.awt.event.MouseEvent e) {
                b.setGradient(new Color(0xF87171), new Color(0xEF4444));
            }
            @Override public void mouseExited(MouseEvent e) {
                b.setGradient(new Color(0xEF4444), new Color(0xDC2626));
            }
        });

        return b;
    }

    private boolean isHighContrast() {
        return UiKit.isHighContrastMode();
    }

    public void setTitle(String t) {
        // Intentionally no-op: centered page title was removed from the top bar.
    }

    public void refreshUserFromSession() {
        User u = (session == null) ? null : session.getCurrentUser();

        String nm = (u == null) ? "User" : safe(u.getFullName(), "User");
        String em = (u == null) ? "user@email.com" : safe(u.getEmail(), "user@email.com");

        nameLabel.setText(nm);
        emailLabel.setText(em);
        avatarLabel.setText(extractInitial(nm));
    }

    private String extractInitial(String name) {
        if (name == null) return "U";
        String t = name.trim();
        if (t.isEmpty()) return "U";

        char c = t.charAt(0);
        if (Character.isLetterOrDigit(c)) return String.valueOf(Character.toUpperCase(c));

        for (int i = 0; i < t.length(); i++) {
            char cc = t.charAt(i);
            if (Character.isLetterOrDigit(cc)) return String.valueOf(Character.toUpperCase(cc));
        }
        return "U";
    }

    private String safe(String v, String fallback) {
        if (v == null) return fallback;
        String t = v.trim();
        return t.isEmpty() ? fallback : t;
    }

    /** Circular avatar used in the user pill. */
    private static class BadgeLabel extends JLabel {
        private static final int SIZE = 28;

        BadgeLabel(String text) {
            super(text, SwingConstants.CENTER);
            setOpaque(false);
            setForeground(Color.WHITE);
            setFont(UiKit.scaled(this, Font.BOLD, 0.95f));
            setPreferredSize(new Dimension(SIZE, SIZE));
            setMinimumSize(new Dimension(SIZE, SIZE));
            setMaximumSize(new Dimension(SIZE, SIZE));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();

            boolean highContrast = UiKit.isHighContrastMode();
            Color fill = highContrast ? Color.BLACK : UiKit.PRIMARY;

            g2.setColor(fill);
            g2.fill(new Ellipse2D.Double(0, 0, w, h));

            if (!highContrast) {
                g2.setColor(UiKit.BRAND_BORDER);
                g2.draw(new Ellipse2D.Double(0.5, 0.5, w - 1, h - 1));
            }

            g2.dispose();
            super.paintComponent(g);
        }
    }
}
