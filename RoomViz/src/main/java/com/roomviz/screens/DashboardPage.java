package com.roomviz.screens;

import com.roomviz.app.AppFrame;
import com.roomviz.app.Router;
import com.roomviz.app.ScreenKeys;
import com.roomviz.data.AppState;
import com.roomviz.model.Design;
import com.roomviz.model.RoomSpec;
import com.roomviz.model.DesignStatus;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Dashboard page styled to match the Figma layout (cards, CTA, filters, list rows)
 * - NOW wired to real repository data (stats + recent list)
 * - Delete / Duplicate / Open fully functional
 */
public class DashboardPage extends JPanel {

    // ----- Palette (Figma-like) -----
    private static final Color BG = new Color(0xF6F7FB);
    private static final Color TEXT = new Color(0x111827);
    private static final Color MUTED = new Color(0x6B7280);
    private static final Color BORDER = new Color(0xE5E7EB);
    private static final Color WHITE = Color.WHITE;
    private static final Color PRIMARY = new Color(0x4F46E5);
    private static final Color PRIMARY_DARK = new Color(0x6D28D9);
    private static final Color SUCCESS = new Color(0x16A34A);
    private static final Color WARN = new Color(0xF59E0B);

    private final Router router;
    private final AppState appState;

    // dynamic sections
    private JPanel statsRowRef;
    private JPanel recentListRef;

    public DashboardPage(AppFrame frame, Router router, AppState appState) {
        this.router = router;
        this.appState = appState;

        setLayout(new BorderLayout());
        setOpaque(false);

        JPanel content = new JPanel();
        content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBorder(new EmptyBorder(22, 24, 22, 24));

        // Header
        content.add(welcomeHeader());
        content.add(vSpace(14));

        // Stats cards row (dynamic)
        statsRowRef = new JPanel(new GridLayout(1, 3, 14, 0));
        statsRowRef.setOpaque(false);
        content.add(statsRowRef);
        content.add(vSpace(16));

        // CTA
        content.add(ctaCard());
        content.add(vSpace(18));

        // Search + Filters
        content.add(searchAndFilters());
        content.add(vSpace(14));

        // Recent Designs
        content.add(recentHeader());
        content.add(vSpace(10));

        recentListRef = new JPanel();
        recentListRef.setOpaque(false);
        recentListRef.setLayout(new BoxLayout(recentListRef, BoxLayout.Y_AXIS));
        content.add(recentListRef);

        content.add(vSpace(10));

        JLabel loadMore = new JLabel("Load More Designs \u2193");
        loadMore.setForeground(PRIMARY);
        loadMore.setFont(loadMore.getFont().deriveFont(Font.PLAIN, 12.5f));
        loadMore.setBorder(new EmptyBorder(6, 6, 6, 6));
        content.add(loadMore);

        JScrollPane scroller = new JScrollPane(content);
        scroller.setBorder(BorderFactory.createEmptyBorder());
        scroller.getViewport().setOpaque(true);
        scroller.getViewport().setBackground(BG);
        scroller.setOpaque(false);
        scroller.getVerticalScrollBar().setUnitIncrement(18);

        add(scroller, BorderLayout.CENTER);

        // refresh when this screen is shown again
        router.addListener(key -> {
            if (ScreenKeys.DASHBOARD.equals(key)) {
                refreshDynamicSections();
            }
        });

        // first paint
        refreshDynamicSections();
    }

    /* ===================== Dynamic refresh ===================== */

    private void refreshDynamicSections() {
        refreshStats();
        refreshRecentList();
        revalidate();
        repaint();
    }

    private void refreshStats() {
        if (statsRowRef == null) return;

        statsRowRef.removeAll();

        List<Design> all = appState.getRepo().getAllSortedByLastUpdatedDesc();

        int savedCount = all.size();

        int inProgressCount = 0;
        for (Design d : all) {
            if (d != null && d.getStatus() == DesignStatus.IN_PROGRESS) {
                inProgressCount++;
            }
        }

        long startOfToday = startOfTodayEpochMs();
        int editedToday = 0;
        for (Design d : all) {
            if (d != null && d.getLastUpdatedEpochMs() >= startOfToday) {
                editedToday++;
            }
        }

        statsRowRef.add(statCard(String.valueOf(savedCount), "Saved Designs",
                "All time", SUCCESS, new Color(0xEEFDF3), new Color(0xDCFCE7)));

        statsRowRef.add(statCard(String.valueOf(inProgressCount), "In Progress",
                "With items", WARN, new Color(0xFFF7ED), new Color(0xFFEDD5)));

        statsRowRef.add(statCard(String.valueOf(editedToday), "Recently Edited",
                "Today", new Color(0x7C3AED), new Color(0xF5F3FF), new Color(0xEDE9FE)));
    }

    private void refreshRecentList() {
        if (recentListRef == null) return;

        recentListRef.removeAll();

        List<Design> all = appState.getRepo().getAllSortedByLastUpdatedDesc();
        if (all.isEmpty()) {
            recentListRef.add(emptyRecentCard());
            return;
        }

        int limit = Math.min(5, all.size());
        for (int i = 0; i < limit; i++) {
            Design d = all.get(i);
            recentListRef.add(designRow(d));
            if (i < limit - 1) recentListRef.add(vSpace(10));
        }
    }

    private JComponent emptyRecentCard() {
        RoundedPanel card = new RoundedPanel(14, WHITE);
        card.setOpaque(false);
        card.setBorderPaint(BORDER);
        card.setLayout(new BorderLayout());
        card.setBorder(new EmptyBorder(14, 14, 14, 14));

        JLabel t = new JLabel("No designs yet");
        t.setForeground(TEXT);
        t.setFont(t.getFont().deriveFont(Font.BOLD, 13.2f));

        JLabel sub = new JLabel("Click “Create New Design” to start your first room visualization.");
        sub.setForeground(MUTED);
        sub.setFont(sub.getFont().deriveFont(Font.PLAIN, 12.2f));
        sub.setBorder(new EmptyBorder(6, 0, 0, 0));

        JPanel box = new JPanel();
        box.setOpaque(false);
        box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));
        box.add(t);
        box.add(sub);

        card.add(box, BorderLayout.CENTER);
        return card;
    }

    /* ===================== Sections ===================== */

    private JComponent welcomeHeader() {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));

        JLabel h1 = new JLabel("Welcome back!");
        h1.setForeground(TEXT);
        h1.setFont(h1.getFont().deriveFont(Font.BOLD, 26f));

        JLabel sub = new JLabel("Ready to create stunning furniture visualizations for your clients?");
        sub.setForeground(MUTED);
        sub.setFont(sub.getFont().deriveFont(Font.PLAIN, 12.5f));
        sub.setBorder(new EmptyBorder(6, 0, 0, 0));

        p.add(h1);
        p.add(sub);
        return p;
    }

    private JComponent statCard(String value, String label, String pillText, Color pillFg, Color pillBg, Color iconBg) {
        RoundedPanel card = new RoundedPanel(14, WHITE);
        card.setLayout(new BorderLayout());
        card.setBorder(new EmptyBorder(14, 14, 14, 14));
        card.setOpaque(false);
        card.setBorderPaint(BORDER);

        // Top row: icon + pill
        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);

        RoundedPanel icon = new RoundedPanel(10, iconBg);
        icon.setPreferredSize(new Dimension(32, 32));
        icon.setOpaque(false);
        icon.setLayout(new GridBagLayout());
        JLabel dot = new JLabel("\u25A0");
        dot.setForeground(pillFg);
        dot.setFont(dot.getFont().deriveFont(Font.BOLD, 10f));
        icon.add(dot);

        JLabel pill = pill(pillText, pillFg, pillBg);

        top.add(icon, BorderLayout.WEST);
        top.add(pill, BorderLayout.EAST);

        // Value + label
        JPanel mid = new JPanel();
        mid.setOpaque(false);
        mid.setLayout(new BoxLayout(mid, BoxLayout.Y_AXIS));
        mid.setBorder(new EmptyBorder(12, 2, 0, 0));

        JLabel v = new JLabel(value);
        v.setForeground(TEXT);
        v.setFont(v.getFont().deriveFont(Font.BOLD, 22f));

        JLabel l = new JLabel(label);
        l.setForeground(MUTED);
        l.setFont(l.getFont().deriveFont(Font.PLAIN, 12.2f));
        l.setBorder(new EmptyBorder(2, 0, 0, 0));

        mid.add(v);
        mid.add(l);

        card.add(top, BorderLayout.NORTH);
        card.add(mid, BorderLayout.CENTER);
        return card;
    }

    private JComponent ctaCard() {
        GradientPanel g = new GradientPanel(PRIMARY, PRIMARY_DARK, 18);
        g.setLayout(new BoxLayout(g, BoxLayout.Y_AXIS));
        g.setBorder(new EmptyBorder(22, 22, 22, 22));

        JLabel title = new JLabel("Start a New Visualization");
        title.setForeground(Color.WHITE);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 18f));
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel sub = new JLabel("Create immersive room designs for your live consultations");
        sub.setForeground(new Color(255, 255, 255, 210));
        sub.setFont(sub.getFont().deriveFont(Font.PLAIN, 12.5f));
        sub.setBorder(new EmptyBorder(6, 0, 0, 0));
        sub.setAlignmentX(Component.CENTER_ALIGNMENT);

        JButton btn = primaryButton("+  Create New Design");
        btn.setAlignmentX(Component.CENTER_ALIGNMENT);
        btn.addActionListener(e -> router.show(ScreenKeys.NEW_DESIGN));

        g.add(title);
        g.add(sub);
        g.add(vSpace(14));
        g.add(btn);
        return g;
    }

    private JComponent searchAndFilters() {
        RoundedPanel card = new RoundedPanel(14, WHITE);
        card.setOpaque(false);
        card.setBorderPaint(BORDER);
        card.setLayout(new BorderLayout());
        card.setBorder(new EmptyBorder(12, 12, 12, 12));

        // Search row
        JPanel top = new JPanel(new BorderLayout(10, 0));
        top.setOpaque(false);

        JTextField search = new JTextField();
        search.setText("Search designs by name, room type, or client...");
        search.setForeground(new Color(0x9CA3AF));
        search.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(BORDER, 1, true),
                new EmptyBorder(10, 12, 10, 12)
        ));
        search.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override public void focusGained(java.awt.event.FocusEvent e) {
                if ("Search designs by name, room type, or client...".equals(search.getText())) {
                    search.setText("");
                    search.setForeground(TEXT);
                }
            }
            @Override public void focusLost(java.awt.event.FocusEvent e) {
                if (search.getText().trim().isEmpty()) {
                    search.setText("Search designs by name, room type, or client...");
                    search.setForeground(new Color(0x9CA3AF));
                }
            }
        });

        JButton allFilters = ghostButton("All Filters");
        allFilters.setPreferredSize(new Dimension(110, allFilters.getPreferredSize().height));

        top.add(search, BorderLayout.CENTER);
        top.add(allFilters, BorderLayout.EAST);

        // Chips row (UI-only for now)
        JPanel chips = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 10));
        chips.setOpaque(false);
        chips.add(chip("Last 7 Days \u25BC"));
        chips.add(chip("Date Range \u25BC"));
        chips.add(chip("Room Size \u25BC"));
        chips.add(chip("Tags \u25BC"));
        chips.add(chipLink("+ Add Filter"));

        card.add(top, BorderLayout.NORTH);
        card.add(chips, BorderLayout.SOUTH);
        return card;
    }

    private JComponent recentHeader() {
        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(false);

        JLabel title = new JLabel("Recent Designs");
        title.setForeground(TEXT);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 16f));

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        right.setOpaque(false);

        JLabel sort = new JLabel("Sort by: Latest");
        sort.setForeground(MUTED);
        sort.setFont(sort.getFont().deriveFont(Font.PLAIN, 12.2f));

        right.add(sort);
        right.add(iconSquare("\u2630"));
        right.add(iconSquare("\u25A6"));

        row.add(title, BorderLayout.WEST);
        row.add(right, BorderLayout.EAST);
        return row;
    }

    private JComponent designRow(Design d) {
        String title = safe(d.getDesignName(), "Untitled Design");
        String client = safe(d.getCustomerName(), "Client");
        RoomSpec rs = d.getRoomSpec();

        String size = (rs == null) ? "-" : rs.toSizeLabel();
        String scheme = (rs == null) ? "-" : safe(rs.getColorScheme(), "-");
        String shape = (rs == null) ? "-" : safe(rs.getShape(), "-");

        String edited = "Last edited: " + timeAgoLabel(d.getLastUpdatedEpochMs());
        String status = (d.getItems() != null && !d.getItems().isEmpty()) ? "In Progress" : "Draft";
        Color statusColor = (d.getItems() != null && !d.getItems().isEmpty()) ? WARN : new Color(0x2563EB);

        RoundedPanel card = new RoundedPanel(14, WHITE);
        card.setOpaque(false);
        card.setBorderPaint(BORDER);
        card.setLayout(new BorderLayout());
        card.setBorder(new EmptyBorder(12, 12, 12, 12));

        // Left: thumbnail
        RoundedPanel thumb = new RoundedPanel(12, new Color(0xE5E7EB));
        thumb.setPreferredSize(new Dimension(72, 52));
        thumb.setOpaque(false);
        thumb.setLayout(new GridBagLayout());
        JLabel pic = new JLabel("\u25A1");
        pic.setForeground(new Color(0x9CA3AF));
        thumb.add(pic);

        // Middle: text
        JPanel mid = new JPanel();
        mid.setOpaque(false);
        mid.setLayout(new BoxLayout(mid, BoxLayout.Y_AXIS));

        JLabel t = new JLabel(title + " - " + client);
        t.setForeground(TEXT);
        t.setFont(t.getFont().deriveFont(Font.BOLD, 13.2f));

        JLabel meta = new JLabel(edited);
        meta.setForeground(MUTED);
        meta.setFont(meta.getFont().deriveFont(Font.PLAIN, 11.5f));
        meta.setBorder(new EmptyBorder(2, 0, 0, 0));

        JPanel pills = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 6));
        pills.setOpaque(false);
        pills.add(metaPill(size));
        pills.add(metaPill(shape));
        pills.add(metaPill(scheme));

        mid.add(t);
        mid.add(meta);
        mid.add(pills);

        // Right: status + actions
        JPanel right = new JPanel();
        right.setOpaque(false);
        right.setLayout(new BoxLayout(right, BoxLayout.Y_AXIS));

        JLabel st = new JLabel(status);
        st.setForeground(statusColor);
        st.setFont(st.getFont().deriveFont(Font.BOLD, 11.5f));
        st.setAlignmentX(Component.RIGHT_ALIGNMENT);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 10));
        actions.setOpaque(false);

        JButton open = primarySmall("Open");
        JButton dup = ghostSmall("Duplicate");
        JButton del = dangerSmall("Delete");

        open.addActionListener(e -> {
            appState.setCurrentDesignId(d.getId());
            router.show(ScreenKeys.PLANNER_2D);
        });

        dup.addActionListener(e -> {
            appState.getRepo().duplicate(d.getId());
            refreshDynamicSections();
        });

        del.addActionListener(e -> {
            int ok = JOptionPane.showConfirmDialog(
                    this,
                    "Delete \"" + title + "\"?\nThis cannot be undone.",
                    "Confirm delete",
                    JOptionPane.YES_NO_OPTION
            );
            if (ok == JOptionPane.YES_OPTION) {
                appState.getRepo().delete(d.getId());
                if (d.getId() != null && d.getId().equals(appState.getCurrentDesignId())) {
                    appState.setCurrentDesignId(null);
                }
                refreshDynamicSections();
            }
        });

        actions.add(open);
        actions.add(dup);
        actions.add(del);

        right.add(st);
        right.add(actions);

        JPanel left = new JPanel(new BorderLayout(12, 0));
        left.setOpaque(false);
        left.add(thumb, BorderLayout.WEST);
        left.add(mid, BorderLayout.CENTER);

        card.add(left, BorderLayout.CENTER);
        card.add(right, BorderLayout.EAST);

        // Subtle hover
        card.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { card.setFill(new Color(0xFAFAFB)); card.repaint(); }
            @Override public void mouseExited(MouseEvent e) { card.setFill(WHITE); card.repaint(); }
            @Override public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    appState.setCurrentDesignId(d.getId());
                    router.show(ScreenKeys.PLANNER_2D);
                }
            }
        });

        return card;
    }

    /* ===================== Helpers ===================== */

    private static long startOfTodayEpochMs() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
    }

    private static String safe(String s, String fallback) {
        if (s == null) return fallback;
        s = s.trim();
        return s.isEmpty() ? fallback : s;
    }

    private static String timeAgoLabel(long epochMs) {
        if (epochMs <= 0) return "-";
        long diff = System.currentTimeMillis() - epochMs;

        if (diff < 60_000) return "Just now";

        long mins = diff / 60_000;
        if (mins < 60) return mins + " min ago";

        long hrs = mins / 60;
        if (hrs < 24) return hrs + " hr ago";

        long days = hrs / 24;
        if (days < 14) return days + " days ago";

        try {
            SimpleDateFormat sdf = new SimpleDateFormat("d MMM yyyy", Locale.ENGLISH);
            return sdf.format(new Date(epochMs));
        } catch (Exception e) {
            return "Earlier";
        }
    }

    /* ===================== Small UI bits ===================== */

    private static Component vSpace(int px) {
        return Box.createRigidArea(new Dimension(0, px));
    }

    private static JLabel pill(String text, Color fg, Color bg) {
        JLabel l = new JLabel(text);
        l.setOpaque(true);
        l.setBackground(bg);
        l.setForeground(fg);
        l.setFont(l.getFont().deriveFont(Font.BOLD, 11f));
        l.setBorder(new EmptyBorder(4, 10, 4, 10));
        return l;
    }

    private static JLabel metaPill(String text) {
        JLabel l = new JLabel(text);
        l.setOpaque(true);
        l.setBackground(new Color(0xF3F4F6));
        l.setForeground(new Color(0x374151));
        l.setFont(l.getFont().deriveFont(Font.PLAIN, 11.2f));
        l.setBorder(new EmptyBorder(3, 8, 3, 8));
        return l;
    }

    private static JComponent chip(String text) {
        JLabel l = new JLabel(text);
        l.setOpaque(true);
        l.setBackground(new Color(0xF3F4F6));
        l.setForeground(new Color(0x374151));
        l.setFont(l.getFont().deriveFont(Font.PLAIN, 11.5f));
        l.setBorder(new EmptyBorder(6, 10, 6, 10));
        return l;
    }

    private static JComponent chipLink(String text) {
        JLabel l = new JLabel(text);
        l.setOpaque(true);
        l.setBackground(new Color(0xEEF2FF));
        l.setForeground(PRIMARY);
        l.setFont(l.getFont().deriveFont(Font.BOLD, 11.5f));
        l.setBorder(new EmptyBorder(6, 10, 6, 10));
        return l;
    }

    private static JButton primaryButton(String text) {
        JButton b = new JButton(text);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setFocusPainted(false);
        b.setForeground(TEXT);
        b.setBackground(Color.WHITE);
        b.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(255, 255, 255, 140), 1, true),
                new EmptyBorder(10, 16, 10, 16)
        ));
        return b;
    }

    private static JButton ghostButton(String text) {
        JButton b = new JButton(text);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setFocusPainted(false);
        b.setBackground(Color.WHITE);
        b.setForeground(new Color(0x111827));
        b.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(BORDER, 1, true),
                new EmptyBorder(9, 12, 9, 12)
        ));
        return b;
    }

    private static JButton primarySmall(String text) {
        JButton b = new JButton(text);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setFocusPainted(false);
        b.setBackground(PRIMARY);
        b.setForeground(Color.WHITE);
        b.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
        return b;
    }

    private static JButton ghostSmall(String text) {
        JButton b = new JButton(text);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setFocusPainted(false);
        b.setBackground(new Color(0xF3F4F6));
        b.setForeground(new Color(0x111827));
        b.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
        return b;
    }

    private static JButton dangerSmall(String text) {
        JButton b = new JButton(text);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setFocusPainted(false);
        b.setBackground(Color.WHITE);
        b.setForeground(new Color(0xDC2626));
        b.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(0xFCA5A5), 1, true),
                new EmptyBorder(8, 12, 8, 12)
        ));
        return b;
    }

    private static JComponent iconSquare(String text) {
        RoundedPanel p = new RoundedPanel(10, WHITE);
        p.setOpaque(false);
        p.setBorderPaint(BORDER);
        p.setPreferredSize(new Dimension(28, 28));
        p.setLayout(new GridBagLayout());
        JLabel l = new JLabel(text);
        l.setForeground(new Color(0x6B7280));
        l.setFont(l.getFont().deriveFont(Font.PLAIN, 12f));
        p.add(l);
        return p;
    }

    /* ===================== Custom panels ===================== */

    /** Rounded rectangle panel with optional border paint. */
    private static class RoundedPanel extends JPanel {
        private final int radius;
        private Color fill;
        private Color border;

        RoundedPanel(int radius, Color fill) {
            this.radius = radius;
            this.fill = fill;
            setOpaque(false);
        }

        void setFill(Color c) { this.fill = c; }
        void setBorderPaint(Color c) { this.border = c; }

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

    /* ===================== Custom gradient panel (same style you used) ===================== */

    private static class GradientPanel extends JPanel {
        private final Color c1, c2;
        private final int radius;

        GradientPanel(Color c1, Color c2, int radius) {
            this.c1 = c1;
            this.c2 = c2;
            this.radius = radius;
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth();
            int h = getHeight();
            GradientPaint gp = new GradientPaint(0, 0, c1, w, h, c2);
            g2.setPaint(gp);
            g2.fillRoundRect(0, 0, w, h, radius, radius);
            g2.dispose();
            super.paintComponent(g);
        }
    }
}
