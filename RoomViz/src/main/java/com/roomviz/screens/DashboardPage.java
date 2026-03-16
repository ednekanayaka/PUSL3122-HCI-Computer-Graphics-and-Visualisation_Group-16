package com.roomviz.screens;

import com.roomviz.app.AppFrame;
import com.roomviz.app.Router;
import com.roomviz.app.ScreenKeys;
import com.roomviz.data.AppState;
import com.roomviz.model.Design;
import com.roomviz.model.DesignStatus;
import com.roomviz.model.RoomSpec;
import com.roomviz.ui.Mini2DPreviewPanel;
import com.roomviz.ui.FontAwesome;
import com.roomviz.ui.UiKit;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import java.util.Locale;

/**
 * Dashboard page
 * - repository data (stats + recent list)
 * - Search updates in real-time
 * - Filters: Last 7 Days, Room Size, Room Shape
 * - Load More paginates
 */
public class DashboardPage extends JPanel {

    private static final String SEARCH_PLACEHOLDER = "Search designs by name, room type, or client...";

    private final Router router;
    private final AppState appState;

    // dynamic sections
    private JPanel statsRowRef;
    private JPanel recentListRef;

    // dashboard filters (wired)
    private JTextField searchField;
    private JLabel loadMoreLabel;

    // pagination
    private int recentLimit = 5;

    // chip filters
    private boolean filterLast7Days = false;
    private String filterRoomSize = null;   // "Small" | "Medium" | "Large" | null
    private String filterRoomShape = null;  // "Square" | "Rectangular" | "L-Shape" | "Custom" | null

    // chip refs so we can update active style
    private JComponent chipLast7;
    private JComponent chipRoomSize;
    private JComponent chipRoomShape;

    public DashboardPage(AppFrame frame, Router router, AppState appState) {
        this.router = router;
        this.appState = appState;

        setLayout(new BorderLayout());
        setOpaque(false);

        ScrollablePanel content = new ScrollablePanel();
        content.setOpaque(false);
        content.setLayout(new GridBagLayout()); // Use GridBagLayout for flexible centering
        content.setBorder(new EmptyBorder(22, 24, 22, 24));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.NORTH;
        gbc.insets = new Insets(0, 0, 0, 0);

        // Inner column to cap width
        JPanel column = new JPanel();
        column.setOpaque(false);
        column.setLayout(new BoxLayout(column, BoxLayout.Y_AXIS));
        column.setMaximumSize(new Dimension(860, 99999));

        // Header
        column.add(welcomeHeader());
        column.add(vSpace(28));

        // Stats cards row (dynamic)
        statsRowRef = new JPanel(new GridLayout(1, 3, 14, 0));
        statsRowRef.setOpaque(false);
        column.add(statsRowRef);
        column.add(vSpace(32));

        // CTA
        column.add(ctaCard());
        column.add(vSpace(32));

        // Search + Filters (wired)
        column.add(searchAndFilters());
        column.add(vSpace(32));

        // Recent Designs
        column.add(recentHeader());
        column.add(vSpace(14));

        recentListRef = new JPanel();
        recentListRef.setOpaque(false);
        recentListRef.setLayout(new BoxLayout(recentListRef, BoxLayout.Y_AXIS));
        column.add(recentListRef);

        column.add(vSpace(20));

        JPanel loadMoreBox = new JPanel(new FlowLayout(FlowLayout.CENTER));
        loadMoreBox.setOpaque(false);
        loadMoreLabel = new JLabel("Load More Designs " + FontAwesome.ARROW_DOWN);
        loadMoreLabel.setForeground(UiKit.PRIMARY);
        loadMoreLabel.setFont(UiKit.scaled(loadMoreLabel, Font.BOLD, 1.0f));
        loadMoreLabel.setBorder(new EmptyBorder(6, 6, 6, 6));
        loadMoreLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        loadMoreLabel.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                recentLimit += 5;
                refreshRecentList();
                revalidate();
                repaint();
            }
        });
        loadMoreBox.add(loadMoreLabel);
        column.add(loadMoreBox);
        column.add(vSpace(40));

        // wrapper to limit max width while staying centered
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);
        wrapper.setMaximumSize(new Dimension(860, 99999));
        wrapper.add(column, BorderLayout.CENTER);

        content.add(wrapper, gbc);

        JScrollPane scroller = new JScrollPane(content);
        scroller.setBorder(BorderFactory.createEmptyBorder());
        scroller.getViewport().setOpaque(true);
        scroller.getViewport().setBackground(UiKit.BG);
        scroller.setOpaque(false);
        scroller.getVerticalScrollBar().setUnitIncrement(18);

        add(scroller, BorderLayout.CENTER);

        // refresh when this screen is shown again
        if (router != null) {
            router.addListener(key -> {
                if (ScreenKeys.DASHBOARD.equals(key)) {
                    refreshDynamicSections();
                }
            });
        }

        // first paint
        refreshDynamicSections();
    }

    /* ===================== Dynamic refresh ===================== */

    private void refreshDynamicSections() {
        refreshStats();

        // reset pagination baseline each time user returns to dashboard
        recentLimit = 5;

        refreshRecentList();
        revalidate();
        repaint();
    }

    private void refreshStats() {
        if (statsRowRef == null) return;

        statsRowRef.removeAll();

        if (appState == null || appState.getRepo() == null) {
            statsRowRef.add(statCard("0", "Saved Designs", "All time",
                    UiKit.PILL_SUCCESS_FG, UiKit.PILL_SUCCESS_BG, UiKit.PILL_SUCCESS_BG));
            statsRowRef.add(statCard("0", "In Progress", "With items",
                    UiKit.PILL_WARN_FG, UiKit.PILL_WARN_BG, UiKit.PILL_WARN_BG));
            statsRowRef.add(statCard("0", "Recently Edited", "Today",
                    UiKit.PILL_PURPLE_FG, UiKit.PILL_PURPLE_BG, UiKit.PILL_PURPLE_BG));
            statsRowRef.revalidate();
            statsRowRef.repaint();
            return;
        }

        List<Design> all = appState.getRepo().getAllSortedByLastUpdatedDesc();
        int savedCount = (all == null) ? 0 : all.size();

        int inProgressCount = 0;
        if (all != null) {
            for (Design d : all) {
                if (d != null && d.getStatus() == DesignStatus.IN_PROGRESS) {
                    inProgressCount++;
                }
            }
        }

        long startOfToday = startOfTodayEpochMs();
        int editedToday = 0;
        if (all != null) {
            for (Design d : all) {
                if (d != null && d.getLastUpdatedEpochMs() >= startOfToday) {
                    editedToday++;
                }
            }
        }

        statsRowRef.add(statCard(String.valueOf(savedCount), "Saved Designs",
                "All time", UiKit.PILL_SUCCESS_FG, UiKit.PILL_SUCCESS_BG, UiKit.PILL_SUCCESS_BG));

        statsRowRef.add(statCard(String.valueOf(inProgressCount), "In Progress",
                "With items", UiKit.PILL_WARN_FG, UiKit.PILL_WARN_BG, UiKit.PILL_WARN_BG));

        statsRowRef.add(statCard(String.valueOf(editedToday), "Recently Edited",
                "Today", UiKit.PILL_PURPLE_FG, UiKit.PILL_PURPLE_BG, UiKit.PILL_PURPLE_BG));

        statsRowRef.revalidate();
        statsRowRef.repaint();
    }

    private void refreshRecentList() {
        if (recentListRef == null) return;

        recentListRef.removeAll();

        if (appState == null || appState.getRepo() == null) {
            recentListRef.add(emptyRecentCard());
            updateLoadMoreVisibility(0, 0);
            recentListRef.revalidate();
            recentListRef.repaint();
            return;
        }

        List<Design> all = appState.getRepo().getAllSortedByLastUpdatedDesc();
        if (all == null || all.isEmpty()) {
            recentListRef.add(emptyRecentCard());
            updateLoadMoreVisibility(0, 0);
            recentListRef.revalidate();
            recentListRef.repaint();
            return;
        }

        // apply filters
        List<Design> filtered = applyRecentFilters(all);

        if (filtered.isEmpty()) {
            recentListRef.add(emptyFilteredCard());
            updateLoadMoreVisibility(0, 0);
            recentListRef.revalidate();
            recentListRef.repaint();
            return;
        }

        int limit = Math.min(recentLimit, filtered.size());
        for (int i = 0; i < limit; i++) {
            Design d = filtered.get(i);
            if (d == null) continue;
            recentListRef.add(designRow(d));
            if (i < limit - 1) recentListRef.add(vSpace(10));
        }

        updateLoadMoreVisibility(limit, filtered.size());

        // FORCE Swing to layout + paint immediately
        recentListRef.revalidate();
        recentListRef.repaint();
    }

    private void updateLoadMoreVisibility(int shown, int total) {
        if (loadMoreLabel == null) return;

        boolean canLoadMore = total > shown;
        loadMoreLabel.setVisible(canLoadMore);
        if (canLoadMore) {
            loadMoreLabel.setText("Load More Designs " + FontAwesome.ARROW_DOWN);
            loadMoreLabel.setForeground(UiKit.PRIMARY);
        }
    }

    private List<Design> applyRecentFilters(List<Design> all) {
        String q = (searchField == null) ? "" : searchField.getText();

        // when placeholder text is showing, treat it as "no search"
        if (SEARCH_PLACEHOLDER.equals(q)) q = "";

        q = safe(q, "").toLowerCase(Locale.ENGLISH);

        long now = System.currentTimeMillis();
        long last7Cutoff = now - (7L * 24 * 60 * 60 * 1000);

        List<Design> out = new ArrayList<>();
        for (Design d : all) {
            if (d == null) continue;

            if (filterLast7Days) {
                if (d.getLastUpdatedEpochMs() < last7Cutoff) continue;
            }

            if (filterRoomSize != null) {
                RoomSpec rs = d.getRoomSpec();
                if (!matchesRoomSize(rs, filterRoomSize)) continue;
            }

            if (filterRoomShape != null) {
                RoomSpec rs = d.getRoomSpec();
                String shape = (rs == null) ? "" : safe(rs.getShape(), "");
                if (!shape.equalsIgnoreCase(filterRoomShape)) continue;
            }

            if (!q.isEmpty()) {
                String hay = buildHaystack(d);
                if (!hay.contains(q)) continue;
            }

            out.add(d);
        }
        return out;
    }

    /**
     * Room size thresholds (in m²):
     * Small < 12
     * Medium 12–24.99
     * Large >= 25
     */
    private boolean matchesRoomSize(RoomSpec rs, String size) {
        if (rs == null) return false;

        double wM = toMeters(rs.getWidth(), rs.getUnit());
        double lM = toMeters(rs.getLength(), rs.getUnit());
        double area = Math.max(0, wM) * Math.max(0, lM);

        if ("Small".equalsIgnoreCase(size)) return area < 12.0;
        if ("Medium".equalsIgnoreCase(size)) return area >= 12.0 && area < 25.0;
        if ("Large".equalsIgnoreCase(size)) return area >= 25.0;

        return true;
    }

    private static double toMeters(double value, String unit) {
        if (value <= 0) return 0;
        String u = (unit == null) ? "" : unit.trim().toLowerCase(Locale.ENGLISH);

        switch (u) {
            case "m":
            case "meter":
            case "meters":
                return value;

            case "cm":
            case "centimeter":
            case "centimeters":
                return value / 100.0;

            case "ft":
            case "feet":
            case "foot":
                return value * 0.3048;

            default:
                // If unknown, assume meters (safe default)
                return value;
        }
    }

    private String buildHaystack(Design d) {
        StringBuilder sb = new StringBuilder();
        sb.append(safe(d.getDesignName(), "")).append(" ");
        sb.append(safe(d.getCustomerName(), "")).append(" ");

        RoomSpec rs = d.getRoomSpec();
        if (rs != null) {
            sb.append(safe(rs.getRoomType(), "")).append(" ");
            sb.append(safe(rs.getShape(), "")).append(" ");
            sb.append(safe(rs.getColorScheme(), "")).append(" ");
            sb.append(safe(rs.toSizeLabel(), "")).append(" ");
        }

        return sb.toString().toLowerCase(Locale.ENGLISH);
    }

    private JComponent emptyRecentCard() {
        UiKit.RoundedPanel card = new UiKit.RoundedPanel(14, UiKit.WHITE);
        card.setOpaque(false);
        card.setBorderPaint(UiKit.BORDER);
        card.setLayout(new GridBagLayout()); // Center box
        card.setBorder(new EmptyBorder(40, 20, 40, 20));

        JPanel box = new JPanel();
        box.setOpaque(false);
        box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));

        JLabel t = new JLabel("No designs yet", SwingConstants.CENTER);
        t.setForeground(UiKit.TEXT);
        t.setFont(UiKit.scaled(t, Font.BOLD, 1.25f));
        t.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel sub = new JLabel("Click \u201CCreate New Design\u201D to start your first room visualization.", SwingConstants.CENTER);
        sub.setForeground(UiKit.MUTED);
        sub.setFont(UiKit.scaled(sub, Font.PLAIN, 1.05f));
        sub.setBorder(new EmptyBorder(8, 0, 0, 0));
        sub.setAlignmentX(Component.CENTER_ALIGNMENT);

        box.add(t);
        box.add(sub);

        card.add(box);
        return card;
    }

    private JComponent emptyFilteredCard() {
        UiKit.RoundedPanel card = new UiKit.RoundedPanel(14, UiKit.WHITE);
        card.setOpaque(false);
        card.setBorderPaint(UiKit.BORDER);
        card.setLayout(new GridBagLayout()); // Center box
        card.setBorder(new EmptyBorder(40, 20, 40, 20));

        JPanel box = new JPanel();
        box.setOpaque(false);
        box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));

        JLabel t = new JLabel("No matching designs", SwingConstants.CENTER);
        t.setForeground(UiKit.TEXT);
        t.setFont(UiKit.scaled(t, Font.BOLD, 1.25f));
        t.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel sub = new JLabel("Try clearing filters or searching with a different keyword.", SwingConstants.CENTER);
        sub.setForeground(UiKit.MUTED);
        sub.setFont(UiKit.scaled(sub, Font.PLAIN, 1.05f));
        sub.setBorder(new EmptyBorder(8, 0, 0, 0));
        sub.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel clear = new JLabel("Clear filters", SwingConstants.CENTER);
        clear.setForeground(UiKit.PRIMARY);
        clear.setFont(UiKit.scaled(clear, Font.BOLD, 1.05f));
        clear.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        clear.setBorder(new EmptyBorder(14, 0, 0, 0));
        clear.setAlignmentX(Component.CENTER_ALIGNMENT);
        clear.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                clearDashboardFilters();
            }
        });

        box.add(t);
        box.add(sub);
        box.add(clear);

        card.add(box);
        return card;
    }

    private void clearDashboardFilters() {
        // reset search safely (don’t leave placeholder treated as “search text”)
        if (searchField != null) {
            searchField.setText("");
            // If your UiKit placeholder logic depends on focus, do a tiny nudge:
            searchField.postActionEvent();
        }

        filterLast7Days = false;
        filterRoomSize = null;
        filterRoomShape = null;

        // reset chip labels
        if (chipRoomSize instanceof JLabel) ((JLabel) chipRoomSize).setText("Room Size \u25BC");
        if (chipRoomShape instanceof JLabel) ((JLabel) chipRoomShape).setText("Room Shape \u25BC");

        recentLimit = 5;
        updateChipStates();
        refreshRecentList();
        revalidate();
        repaint();
    }

    private void updateChipStates() {
        setChipActive(chipLast7, filterLast7Days);
        setChipActive(chipRoomSize, filterRoomSize != null);
        setChipActive(chipRoomShape, filterRoomShape != null);
    }

    private void setChipActive(JComponent chip, boolean active) {
        if (!(chip instanceof JLabel)) return;
        JLabel l = (JLabel) chip;
        if (active) {
            l.setBackground(UiKit.CHIP_ACTIVE_BG);
            l.setForeground(UiKit.CHIP_ACTIVE_TEXT);
            l.setFont(UiKit.scaled(l, Font.BOLD, 0.95f));
        } else {
            l.setBackground(UiKit.META_PILL_BG);
            l.setForeground(UiKit.META_PILL_FG);
            l.setFont(UiKit.scaled(l, Font.PLAIN, 0.95f));
        }
        l.repaint();
    }

    /* ===================== Sections ===================== */

    private JComponent welcomeHeader() {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));

        JLabel h1 = new JLabel("Welcome back!", SwingConstants.CENTER);
        h1.setForeground(UiKit.TEXT);
        h1.setFont(UiKit.scaled(h1, Font.BOLD, 1.8f));
        h1.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel sub = new JLabel("Ready to create stunning furniture visualizations for your clients?", SwingConstants.CENTER);
        sub.setForeground(UiKit.MUTED);
        sub.setFont(UiKit.scaled(sub, Font.PLAIN, 1.1f));
        sub.setBorder(new EmptyBorder(8, 0, 0, 0));
        sub.setAlignmentX(Component.CENTER_ALIGNMENT);

        p.add(h1);
        p.add(sub);
        return p;
    }

    private JComponent statCard(String value, String label, String pillText, Color pillFg, Color pillBg, Color iconBg) {
        UiKit.RoundedPanel card = new UiKit.RoundedPanel(14, UiKit.WHITE);
        card.setLayout(new BorderLayout());
        card.setBorder(new EmptyBorder(16, 16, 16, 16));
        card.setOpaque(false);
        card.setBorderPaint(UiKit.BORDER);

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);

        UiKit.RoundedPanel icon = new UiKit.RoundedPanel(10, iconBg);
        icon.setPreferredSize(new Dimension(34, 34));
        icon.setOpaque(false);
        icon.setLayout(new GridBagLayout());
        JLabel dot = new JLabel(FontAwesome.SQUARE);
        dot.setForeground(pillFg);
        dot.setFont(FontAwesome.solid(11f));
        icon.add(dot);

        JLabel pill = pill(pillText, pillFg, pillBg);

        top.add(icon, BorderLayout.WEST);
        top.add(pill, BorderLayout.EAST);

        JPanel mid = new JPanel();
        mid.setOpaque(false);
        mid.setLayout(new BoxLayout(mid, BoxLayout.Y_AXIS));
        mid.setBorder(new EmptyBorder(14, 2, 0, 0));

        JLabel v = new JLabel(value);
        v.setForeground(UiKit.TEXT);
        v.setFont(UiKit.scaled(v, Font.BOLD, 1.5f));

        JLabel l = new JLabel(label);
        l.setForeground(UiKit.MUTED);
        l.setFont(UiKit.scaled(l, Font.PLAIN, 0.95f));
        l.setBorder(new EmptyBorder(4, 0, 0, 0));

        mid.add(v);
        mid.add(l);

        card.add(top, BorderLayout.NORTH);
        card.add(mid, BorderLayout.CENTER);
        return card;
    }

    private JComponent ctaCard() {
        UiKit.RoundedPanel g = new UiKit.RoundedPanel(18, UiKit.PRIMARY);
        g.setLayout(new BoxLayout(g, BoxLayout.Y_AXIS));
        g.setBorder(new EmptyBorder(24, 24, 24, 24));
        g.setBorderPaint(UiKit.PRIMARY_DARK);

        JLabel title = new JLabel("Start a New Visualization");
        title.setForeground(Color.WHITE);
        title.setFont(UiKit.scaled(title, Font.BOLD, 1.3f));
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel sub = new JLabel("Create immersive room designs for your live consultations");
        sub.setForeground(new Color(255, 255, 255, 210));
        sub.setFont(UiKit.scaled(sub, Font.PLAIN, 0.95f));
        sub.setBorder(new EmptyBorder(8, 0, 0, 0));
        sub.setAlignmentX(Component.CENTER_ALIGNMENT);

        JButton btn = UiKit.primaryButton("+  Create New Design");
        if (btn instanceof UiKit.RoundButton rb) {
            rb.setGradient(null, null);
        }
        btn.setBackground(Color.WHITE);
        btn.setForeground(UiKit.PRIMARY);
        btn.setFont(UiKit.scaled(btn, Font.BOLD, 1.0f));
        btn.setAlignmentX(Component.CENTER_ALIGNMENT);
        btn.addActionListener(e -> {
            if (router != null) router.show(ScreenKeys.NEW_DESIGN);
        });

        g.add(title);
        g.add(sub);
        g.add(vSpace(18));
        g.add(btn);
        return g;
    }

    private JComponent searchAndFilters() {
        UiKit.RoundedPanel card = new UiKit.RoundedPanel(16, UiKit.WHITE);
        card.setOpaque(false);
        card.setBorderPaint(UiKit.BORDER);
        card.setLayout(new BorderLayout(14, 14));
        card.setBorder(new EmptyBorder(16, 16, 16, 16));

        // Top Row: Search
        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);

        UiKit.SearchResult sr = UiKit.searchFieldWithIcon(SEARCH_PLACEHOLDER);
        searchField = sr.field;
        sr.panel.setPreferredSize(new Dimension(200, 38));
        top.add(sr.panel, BorderLayout.CENTER);

        // Real-time search wiring (DocumentListener)
        if (searchField != null) {
            searchField.getDocument().addDocumentListener(new DocumentListener() {
                @Override public void insertUpdate(DocumentEvent e) { onSearchChanged(); }
                @Override public void removeUpdate(DocumentEvent e) { onSearchChanged(); }
                @Override public void changedUpdate(DocumentEvent e) { onSearchChanged(); }
                private void onSearchChanged() {
                    recentLimit = 5;
                    refreshRecentList();
                }
            });

            // Keep placeholder from acting as a "real query"
            searchField.addFocusListener(new FocusAdapter() {
                @Override public void focusGained(FocusEvent e) {
                    if (SEARCH_PLACEHOLDER.equals(searchField.getText())) {
                        searchField.setText("");
                    }
                }
                @Override public void focusLost(FocusEvent e) {
                    // If user leaves it blank, let UiKit/placeholder behaviour show (safe if UiKit does it)
                    // If UiKit doesn't auto-restore placeholder, this is harmless.
                    if (searchField.getText() != null && searchField.getText().trim().isEmpty()) {
                        // Do NOT force placeholder text here; your UiKit likely handles it.
                        // Leaving blank keeps filtering logic clean.
                    }
                }
            });

            // pressing Enter just refreshes
            searchField.addActionListener(e -> {
                recentLimit = 5;
                refreshRecentList();
            });
        }

        // Bottom Row: Chips
        JPanel chips = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        chips.setOpaque(false);

        chipLast7 = chipInteractive("Last 7 Days \u25BC", () -> {
            filterLast7Days = !filterLast7Days;
            recentLimit = 5;
            updateChipStates();
            refreshRecentList();
        });

        chipRoomSize = chipInteractive("Room Size \u25BC", () -> {
            if (filterRoomSize == null) filterRoomSize = "Small";
            else if ("Small".equals(filterRoomSize)) filterRoomSize = "Medium";
            else if ("Medium".equals(filterRoomSize)) filterRoomSize = "Large";
            else filterRoomSize = null;

            if (chipRoomSize instanceof JLabel) {
                JLabel l = (JLabel) chipRoomSize;
                l.setText((filterRoomSize == null) ? "Room Size \u25BC" : "Room Size: " + filterRoomSize + " \u25BC");
            }

            recentLimit = 5;
            updateChipStates();
            refreshRecentList();
        });

        chipRoomShape = chipInteractive("Room Shape \u25BC", () -> {
            if (filterRoomShape == null) filterRoomShape = "Square";
            else if ("Square".equals(filterRoomShape)) filterRoomShape = "Rectangular";
            else if ("Rectangular".equals(filterRoomShape)) filterRoomShape = "L-Shape";
            else if ("L-Shape".equals(filterRoomShape)) filterRoomShape = "Custom";
            else filterRoomShape = null;

            if (chipRoomShape instanceof JLabel) {
                JLabel l = (JLabel) chipRoomShape;
                l.setText((filterRoomShape == null) ? "Room Shape \u25BC" : "Room Shape: " + filterRoomShape + " \u25BC");
            }

            recentLimit = 5;
            updateChipStates();
            refreshRecentList();
        });

        chips.add(chipLast7);
        chips.add(chipRoomSize);
        chips.add(chipRoomShape);

        card.add(top, BorderLayout.CENTER);
        card.add(chips, BorderLayout.SOUTH);

        updateChipStates();
        return card;
    }

    private JComponent chipInteractive(String text, Runnable onClick) {
        JLabel l = (JLabel) UiKit.chip(text);
        l.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        l.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { onClick.run(); }
        });
        return l;
    }

    private JComponent recentHeader() {
        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(false);

        JLabel title = new JLabel("Recent Designs");
        title.setForeground(UiKit.TEXT);
        title.setFont(UiKit.scaled(title, Font.BOLD, 1.2f));

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        right.setOpaque(false);

        JLabel sort = new JLabel("Sort by: Latest");
        sort.setForeground(UiKit.MUTED);
        sort.setFont(UiKit.scaled(sort, Font.PLAIN, 0.95f));

        right.add(sort);
        right.add(iconSquare(FontAwesome.BARS));
        right.add(iconSquare(FontAwesome.TH_LARGE));

        row.add(title, BorderLayout.WEST);
        row.add(right, BorderLayout.EAST);
        return row;
    }

    private JComponent designRow(Design d) {
        String title = safe(d.getDesignName(), "Untitled Design");
        String client = safe(d.getCustomerName(), "Client");
        RoomSpec rs = d.getRoomSpec();

        String size = (rs == null) ? "-" : safe(rs.toSizeLabel(), "-");
        String scheme = (rs == null) ? "-" : safe(rs.getColorScheme(), "-");
        String shape = (rs == null) ? "-" : safe(rs.getShape(), "-");

        String edited = "Last edited: " + timeAgoLabel(d.getLastUpdatedEpochMs());
        boolean hasItems = d.getItems() != null && !d.getItems().isEmpty();
        String status = hasItems ? "In Progress" : "Draft";
        Color statusColor = hasItems ? UiKit.PILL_WARN_FG : UiKit.STATUS_DRAFT_FG;

        UiKit.RoundedPanel card = new UiKit.RoundedPanel(16, UiKit.WHITE);
        card.setOpaque(false);
        card.setBorderPaint(UiKit.BORDER);
        card.setLayout(new BorderLayout(20, 0));
        card.setBorder(new EmptyBorder(16, 16, 16, 16));

        // Left section: Preview + Info
        JPanel left = new JPanel(new BorderLayout(18, 0));
        left.setOpaque(false);

        Mini2DPreviewPanel thumb = new Mini2DPreviewPanel();
        thumb.setPreferredSize(new Dimension(86, 62));
        thumb.setDesign(d);

        JPanel info = new JPanel();
        info.setOpaque(false);
        info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));

        JLabel t = new JLabel(title);
        t.setForeground(UiKit.TEXT);
        t.setFont(UiKit.scaled(t, Font.BOLD, 1.15f));
        t.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel clientLbl = new JLabel(client);
        clientLbl.setForeground(UiKit.MUTED);
        clientLbl.setFont(UiKit.scaled(clientLbl, Font.PLAIN, 1.05f));
        clientLbl.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel meta = new JLabel(edited);
        meta.setForeground(UiKit.MUTED);
        meta.setFont(UiKit.scaled(meta, Font.PLAIN, 0.90f));
        meta.setBorder(new EmptyBorder(4, 0, 8, 0));
        meta.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel pills = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        pills.setOpaque(false);
        pills.setAlignmentX(Component.LEFT_ALIGNMENT);
        pills.add(metaPill(size));
        pills.add(metaPill(shape));
        pills.add(metaPill(scheme));

        info.add(t);
        info.add(clientLbl);
        info.add(meta);
        info.add(pills);

        left.add(thumb, BorderLayout.WEST);
        left.add(info, BorderLayout.CENTER);

        // Right section: Status + Actions
        JPanel right = new JPanel();
        right.setOpaque(false);
        right.setLayout(new BoxLayout(right, BoxLayout.Y_AXIS));

        JLabel st = new JLabel(status.toUpperCase());
        st.setForeground(statusColor);
        st.setFont(UiKit.scaled(st, Font.BOLD, 0.85f));
        st.setAlignmentX(Component.RIGHT_ALIGNMENT);
        st.setBorder(new EmptyBorder(0, 0, 10, 4));

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        actions.setOpaque(false);
        actions.setAlignmentX(Component.RIGHT_ALIGNMENT);

        JButton open = UiKit.primaryButton("Open");
        open.setFont(UiKit.scaled(open, Font.BOLD, 0.95f));
        open.setMargin(new Insets(6, 14, 6, 14));
        open.addActionListener(e -> {
            if (appState != null) appState.setCurrentDesignId(d.getId());
            if (router != null) router.show(ScreenKeys.PLANNER_2D);
        });

        JButton dup = UiKit.ghostButton("Duplicate");
        dup.setFont(UiKit.scaled(dup, Font.BOLD, 0.95f));
        dup.addActionListener(e -> {
            if (appState != null && appState.getRepo() != null) {
                appState.getRepo().duplicate(d.getId());
                refreshDynamicSections();
            }
        });

        JButton del = UiKit.ghostButton("Delete");
        del.setForeground(new Color(0xDC2626));
        del.setFont(UiKit.scaled(del, Font.BOLD, 0.95f));
        del.addActionListener(e -> {
            int ok = JOptionPane.showConfirmDialog(
                    this,
                    "Delete \"" + title + "\"?\nThis cannot be undone.",
                    "Confirm delete",
                    JOptionPane.YES_NO_OPTION
            );
            if (ok == JOptionPane.YES_OPTION) {
                if (appState != null && appState.getRepo() != null) {
                    appState.getRepo().delete(d.getId());
                    if (d.getId() != null && d.getId().equals(appState.getCurrentDesignId())) {
                        appState.setCurrentDesignId(null);
                    }
                    refreshDynamicSections();
                }
            }
        });

        actions.add(open);
        actions.add(dup);
        actions.add(del);

        right.add(st);
        right.add(actions);

        card.add(left, BorderLayout.CENTER);
        card.add(right, BorderLayout.EAST);

        // Hover + double click open (guard setFill if your RoundedPanel supports it)
        card.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) {
                try { card.setFill(UiKit.CARD_HOVER); } catch (Throwable ignored) {}
                card.repaint();
            }
            @Override public void mouseExited(MouseEvent e) {
                try { card.setFill(UiKit.WHITE); } catch (Throwable ignored) {}
                card.repaint();
            }
            @Override public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    if (appState != null) appState.setCurrentDesignId(d.getId());
                    if (router != null) router.show(ScreenKeys.PLANNER_2D);
                }
            }
        });

        return card;
    }

    private JComponent iconSquare(String text) {
        UiKit.RoundedPanel p = new UiKit.RoundedPanel(10, UiKit.WHITE);
        p.setOpaque(false);
        p.setBorderPaint(UiKit.BORDER);
        p.setPreferredSize(new Dimension(32, 32));
        p.setLayout(new GridBagLayout());
        JLabel l = new JLabel(text);
        l.setForeground(UiKit.MUTED);
        l.setFont(FontAwesome.solid(12f));
        p.add(l);
        return p;
    }

    /* ===================== Helpers ===================== */

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

    private static long startOfTodayEpochMs() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
    }

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
        l.setBackground(UiKit.META_PILL_BG);
        l.setForeground(UiKit.META_PILL_FG);
        l.setFont(l.getFont().deriveFont(Font.PLAIN, 11.2f));
        l.setBorder(new EmptyBorder(3, 8, 3, 8));
        return l;
    }

    private static class ScrollablePanel extends JPanel implements Scrollable {
        @Override public Dimension getPreferredScrollableViewportSize() { return super.getPreferredSize(); }
        @Override public int getScrollableUnitIncrement(Rectangle r, int o, int d) { return 18; }
        @Override public int getScrollableBlockIncrement(Rectangle r, int o, int d) { return r.height; }
        @Override public boolean getScrollableTracksViewportWidth() { return true; }
        @Override public boolean getScrollableTracksViewportHeight() { return false; }
    }
}