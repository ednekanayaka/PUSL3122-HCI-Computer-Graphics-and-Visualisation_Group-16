package com.roomviz.screens;

import com.roomviz.app.AppFrame;
import com.roomviz.app.Router;
import com.roomviz.app.ScreenKeys;
import com.roomviz.data.AppState;
import com.roomviz.model.Design;
import com.roomviz.model.RoomSpec;
import com.roomviz.ui.UiKit;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import java.util.Locale;

public class DesignLibraryPage extends JPanel {

    private final Router router;
    private final AppState appState;

    // dynamic UI refs
    private JLabel showingLabel;
    private JPanel gridHost; // holds either empty state or grid panel

    // ✅ NEW: filter/sort/view controls (wired)
    private JTextField searchField;
    private JComboBox<String> shapesBox;
    private JComboBox<String> sizeBox;
    private JComboBox<String> sortBox;
    private JLabel clearFiltersLabel;

    private JButton gridBtn;
    private JButton listBtn;
    private boolean gridMode = true;

    private static final String SEARCH_PLACEHOLDER = "Search designs by name, customer, or tags...";

    // ✅ keep existing constructor (fallback)
    public DesignLibraryPage(AppFrame frame, Router router) {
        this(frame, router, null);
    }

    // ✅ new constructor used by ShellScreen
    public DesignLibraryPage(AppFrame frame, Router router, AppState appState) {
        this.router = router;
        this.appState = appState;

        setLayout(new BorderLayout());
        setOpaque(false);

        JPanel content = new JPanel();
        content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBorder(new EmptyBorder(22, 24, 22, 24));

        content.add(headerRow());
        content.add(Box.createVerticalStrut(14));
        content.add(searchFilterCard(frame));
        content.add(Box.createVerticalStrut(16));
        content.add(metaRow(frame));
        content.add(Box.createVerticalStrut(12));

        // dynamic designs grid
        gridHost = new JPanel(new BorderLayout());
        gridHost.setOpaque(false);
        content.add(gridHost);

        content.add(Box.createVerticalStrut(18));

        JScrollPane scroller = new JScrollPane(content);
        scroller.setBorder(BorderFactory.createEmptyBorder());
        scroller.getViewport().setOpaque(true);
        scroller.getViewport().setBackground(UiKit.BG);
        scroller.setOpaque(false);
        scroller.getVerticalScrollBar().setUnitIncrement(18);

        add(scroller, BorderLayout.CENTER);

        refreshGrid(frame);
    }

    private boolean isHighContrast() {
        return UiKit.TEXT.equals(Color.BLACK) && UiKit.BORDER.equals(Color.BLACK);
    }

    private Color softMuted() {
        // neutral muted used in a few places (placeholder/meta)
        return isHighContrast() ? UiKit.TEXT : new Color(0x9CA3AF);
    }

    private Color hoverFill() {
        // card hover background
        return isHighContrast() ? UiKit.WHITE : new Color(0xFAFAFB);
    }

    private Color imagePlaceholderFill() {
        // image block background
        return isHighContrast() ? UiKit.WHITE : new Color(0xE5E7EB);
    }

    private JComponent headerRow() {
        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(false);

        JPanel left = new JPanel();
        left.setOpaque(false);
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("Design Library");
        title.setForeground(UiKit.TEXT);
        title.setFont(UiKit.scaled(title, Font.BOLD, 1.45f));

        JLabel sub = new JLabel("Manage and organize your saved room designs");
        sub.setForeground(UiKit.MUTED);
        sub.setFont(UiKit.scaled(sub, Font.PLAIN, 0.95f));
        sub.setBorder(new EmptyBorder(6, 0, 0, 0));

        left.add(title);
        left.add(sub);

        JButton cta = UiKit.primaryButton("+   Create New Design");
        cta.setFont(UiKit.scaled(cta, Font.BOLD, 1.00f));
        cta.addActionListener(e -> this.router.show(ScreenKeys.NEW_DESIGN));

        row.add(left, BorderLayout.WEST);
        row.add(cta, BorderLayout.EAST);
        return row;
    }

    // ✅ UPDATED: accept frame and wire listeners
    private JComponent searchFilterCard(AppFrame frame) {
        UiKit.RoundedPanel card = new UiKit.RoundedPanel(14, UiKit.WHITE);
        card.setBorderPaint(UiKit.BORDER);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(new EmptyBorder(12, 12, 12, 12));

        JPanel row1 = new JPanel(new BorderLayout(10, 0));
        row1.setOpaque(false);

        searchField = UiKit.searchField(SEARCH_PLACEHOLDER);
        JButton filters = UiKit.ghostButton("Filters");
        filters.setFont(UiKit.scaled(filters, Font.PLAIN, 1.00f));
        filters.setPreferredSize(new Dimension(110, filters.getPreferredSize().height));

        // ✅ live search
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { refreshGrid(frame); }
            @Override public void removeUpdate(DocumentEvent e) { refreshGrid(frame); }
            @Override public void changedUpdate(DocumentEvent e) { refreshGrid(frame); }
        });

        row1.add(searchField, BorderLayout.CENTER);
        row1.add(filters, BorderLayout.EAST);

        JPanel tags = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 10));
        tags.setOpaque(false);

        JLabel tagsLbl = new JLabel("Tags:");
        tagsLbl.setForeground(UiKit.MUTED);
        tagsLbl.setFont(UiKit.scaled(tagsLbl, Font.PLAIN, 0.92f));

        tags.add(tagsLbl);
        tags.add(UiKit.chipPrimary("Modern"));
        tags.add(UiKit.chip("Classic"));
        tags.add(UiKit.chip("Minimalist"));
        tags.add(UiKit.chip("Industrial"));
        tags.add(linkChip("+ More"));

        JPanel row3 = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        row3.setOpaque(false);

        JLabel roomShapeLbl = new JLabel("Room Shape:");
        roomShapeLbl.setForeground(UiKit.MUTED);
        roomShapeLbl.setFont(UiKit.scaled(roomShapeLbl, Font.PLAIN, 0.90f));

        shapesBox = new JComboBox<>(new String[]{"All Shapes", "Rectangular", "Square", "L-Shape"});
        UiKit.styleDropdown(shapesBox);
        shapesBox.addActionListener(e -> refreshGrid(frame));

        JLabel sizeLbl = new JLabel("Size:");
        sizeLbl.setForeground(UiKit.MUTED);
        sizeLbl.setFont(UiKit.scaled(sizeLbl, Font.PLAIN, 0.90f));

        sizeBox = new JComboBox<>(new String[]{"All Sizes", "Small", "Medium", "Large"});
        UiKit.styleDropdown(sizeBox);
        sizeBox.addActionListener(e -> refreshGrid(frame));

        row3.add(roomShapeLbl);
        row3.add(shapesBox);
        row3.add(sizeLbl);
        row3.add(sizeBox);

        card.add(row1);
        card.add(tags);
        card.add(row3);

        return card;
    }

    // ✅ UPDATED: accept frame and wire sort + view toggle + clear filters
    private JComponent metaRow(AppFrame frame) {
        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(false);

        showingLabel = new JLabel("Showing 0 designs    ");
        showingLabel.setForeground(UiKit.MUTED);
        showingLabel.setFont(UiKit.scaled(showingLabel, Font.PLAIN, 0.90f));

        clearFiltersLabel = new JLabel("Clear all filters");
        clearFiltersLabel.setForeground(isHighContrast() ? UiKit.TEXT : UiKit.PRIMARY);
        clearFiltersLabel.setFont(UiKit.scaled(clearFiltersLabel, Font.PLAIN, 0.90f));
        clearFiltersLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        clearFiltersLabel.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                resetFilters();
                refreshGrid(frame);
            }
        });

        JPanel leftWrap = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        leftWrap.setOpaque(false);
        leftWrap.add(showingLabel);
        leftWrap.add(clearFiltersLabel);

        JPanel rightWrap = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        rightWrap.setOpaque(false);

        JLabel sortLbl = new JLabel("Sort by:");
        sortLbl.setForeground(UiKit.MUTED);
        sortLbl.setFont(UiKit.scaled(sortLbl, Font.PLAIN, 0.90f));

        sortBox = new JComboBox<>(new String[]{"Most Recent", "Oldest", "Name A-Z"});
        UiKit.styleDropdown(sortBox);
        sortBox.addActionListener(e -> refreshGrid(frame));

        gridBtn = UiKit.iconButton("▦");
        listBtn = UiKit.iconButton("≡");

        // ✅ view toggle wiring
        gridBtn.setToolTipText("Grid view");
        listBtn.setToolTipText("List view");

        gridBtn.addActionListener(e -> {
            gridMode = true;
            refreshGrid(frame);
        });

        listBtn.addActionListener(e -> {
            gridMode = false;
            refreshGrid(frame);
        });

        rightWrap.add(sortLbl);
        rightWrap.add(sortBox);
        rightWrap.add(gridBtn);
        rightWrap.add(listBtn);

        row.add(leftWrap, BorderLayout.WEST);
        row.add(rightWrap, BorderLayout.EAST);
        return row;
    }

    private void resetFilters() {
        if (searchField != null) searchField.setText("");
        if (shapesBox != null) shapesBox.setSelectedIndex(0);
        if (sizeBox != null) sizeBox.setSelectedIndex(0);
        if (sortBox != null) sortBox.setSelectedIndex(0);
        gridMode = true;
    }

    /* ========================= Dynamic grid ========================= */

    private void refreshGrid(AppFrame frame) {
        gridHost.removeAll();

        if (appState == null) {
            gridHost.add(emptyState("AppState not wired",
                    "Please use the 3-argument constructor in ShellScreen."), BorderLayout.CENTER);
            if (showingLabel != null) showingLabel.setText("Showing 0 designs    ");
            revalidate();
            repaint();
            return;
        }

        List<Design> designs = appState.getRepo().getAllSortedByLastUpdatedDesc();
        List<Design> filtered = applyFilters(designs);
        filtered = applySort(filtered);

        if (showingLabel != null) showingLabel.setText("Showing " + filtered.size() + " designs    ");

        if (filtered.isEmpty()) {
            gridHost.add(emptyState("No matching designs",
                    "Try clearing filters or searching with a different keyword."), BorderLayout.CENTER);
        } else {
            gridHost.add(gridMode ? grid(frame, filtered) : list(frame, filtered), BorderLayout.CENTER);
        }

        revalidate();
        repaint();
    }

    private List<Design> applyFilters(List<Design> designs) {
        String q = (searchField == null) ? "" : searchField.getText();
        if (SEARCH_PLACEHOLDER.equals(q)) {
            q = "";
        }
        q = safe(q, "").toLowerCase(Locale.ENGLISH);
        
        String shapeFilter = (shapesBox == null) ? "All Shapes" : (String) shapesBox.getSelectedItem();
        String sizeFilter = (sizeBox == null) ? "All Sizes" : (String) sizeBox.getSelectedItem();

        List<Design> out = new ArrayList<>();
        if (designs == null) return out;

        for (Design d : designs) {
            if (d == null) continue;

            // search (name + customer + tags if present)
            if (!q.isEmpty()) {
                String hay = buildSearchHaystack(d);
                if (!hay.contains(q)) continue;
            }

            RoomSpec spec = d.getRoomSpec();

            // shape
            if (shapeFilter != null && !"All Shapes".equalsIgnoreCase(shapeFilter)) {
                if (!matchesShape(spec, shapeFilter)) continue;
            }

            // size
            if (sizeFilter != null && !"All Sizes".equalsIgnoreCase(sizeFilter)) {
                if (!matchesSize(spec, sizeFilter)) continue;
            }

            out.add(d);
        }
        return out;
    }

    private List<Design> applySort(List<Design> designs) {
        String sort = (sortBox == null) ? "Most Recent" : (String) sortBox.getSelectedItem();
        if (sort == null) sort = "Most Recent";

        List<Design> out = new ArrayList<>(designs);

        switch (sort) {
            case "Oldest":
                out.sort(Comparator.comparingLong(Design::getLastUpdatedEpochMs));
                break;
            case "Name A-Z":
                out.sort((a, b) -> safe(a.getDesignName(), "Untitled").compareToIgnoreCase(safe(b.getDesignName(), "Untitled")));
                break;
            case "Most Recent":
            default:
                out.sort((a, b) -> Long.compare(b.getLastUpdatedEpochMs(), a.getLastUpdatedEpochMs()));
                break;
        }

        return out;
    }

    private String buildSearchHaystack(Design d) {
        StringBuilder sb = new StringBuilder();
        sb.append(safe(d.getDesignName(), "")).append(" ");
        sb.append(safe(d.getCustomerName(), "")).append(" ");

        // tags are optional (avoid compile risk by using reflection)
        Object tags = invoke(d, "getTags");
        if (tags instanceof Collection) {
            for (Object t : (Collection<?>) tags) {
                if (t != null) sb.append(t.toString()).append(" ");
            }
        } else if (tags != null) {
            sb.append(tags.toString()).append(" ");
        }

        return sb.toString().toLowerCase(Locale.ENGLISH);
    }

    private boolean matchesShape(RoomSpec spec, String shapeFilter) {
        if (spec == null) return false;

        // Try common getters via reflection to avoid breaking compilation
        Object shape = invoke(spec, "getShape");
        if (shape == null) shape = invoke(spec, "getRoomShape");
        if (shape == null) shape = invoke(spec, "shape");
        String s = (shape == null) ? "" : shape.toString();

        s = s.toLowerCase(Locale.ENGLISH);
        String f = shapeFilter.toLowerCase(Locale.ENGLISH);

        if (f.contains("rect")) return s.contains("rect");
        if (f.contains("square")) return s.contains("square");
        if (f.contains("l-shape") || f.contains("lshape") || f.equals("l-shape")) return s.contains("l");
        return false;
    }

    private boolean matchesSize(RoomSpec spec, String sizeFilter) {
        if (spec == null) return false;

        // First: try numeric width/length (meters) by reflection
        Double w = firstDouble(spec, "getWidthM", "getWidth", "getWidthMeters", "widthM", "width");
        Double l = firstDouble(spec, "getLengthM", "getLength", "getLengthMeters", "lengthM", "length");

        if (w != null && l != null) {
            double area = Math.max(0.0, w) * Math.max(0.0, l);

            // Small: < 12, Medium: 12–24.99, Large: >= 25
            switch (sizeFilter) {
                case "Small": return area < 12.0;
                case "Medium": return area >= 12.0 && area < 25.0;
                case "Large": return area >= 25.0;
            }
        }

        // Fallback: rely on RoomSpec.toSizeLabel() if it includes hints
        String label = safe(spec.toSizeLabel(), "").toLowerCase(Locale.ENGLISH);

        if (sizeFilter.equalsIgnoreCase("Small")) return label.contains("small");
        if (sizeFilter.equalsIgnoreCase("Medium")) return label.contains("medium");
        if (sizeFilter.equalsIgnoreCase("Large")) return label.contains("large");

        // If label doesn't include those words, don't filter out incorrectly
        return true;
    }

    private JComponent emptyState(String title, String subtitle) {
        UiKit.RoundedPanel card = new UiKit.RoundedPanel(14, UiKit.WHITE);
        card.setBorderPaint(UiKit.BORDER);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(new EmptyBorder(22, 22, 22, 22));

        JLabel t = new JLabel(title);
        t.setForeground(UiKit.TEXT);
        t.setFont(UiKit.scaled(t, Font.BOLD, 1.10f));

        JLabel s = new JLabel(subtitle);
        s.setForeground(UiKit.MUTED);
        s.setFont(UiKit.scaled(s, Font.PLAIN, 0.98f));
        s.setBorder(new EmptyBorder(8, 0, 0, 0));

        card.add(t);
        card.add(s);
        return card;
    }

    private JComponent grid(AppFrame frame, List<Design> designs) {
        JPanel grid = new JPanel(new GridLayout(0, 4, 14, 14));
        grid.setOpaque(false);

        for (Design d : designs) {
            String title = safe(d.getDesignName(), "Untitled Design");
            String client = safe(d.getCustomerName(), "Unknown Client");

            RoomSpec spec = d.getRoomSpec();
            String size = (spec == null) ? "-" : spec.toSizeLabel();

            String timeAgo = timeAgoLabel(d.getLastUpdatedEpochMs());
            grid.add(designCard(frame, d, title, client, size, timeAgo));
        }
        return grid;
    }

    // ✅ NEW: list view (same cards, 1 per row)
    private JComponent list(AppFrame frame, List<Design> designs) {
        JPanel list = new JPanel(new GridLayout(0, 1, 14, 14));
        list.setOpaque(false);

        for (Design d : designs) {
            String title = safe(d.getDesignName(), "Untitled Design");
            String client = safe(d.getCustomerName(), "Unknown Client");

            RoomSpec spec = d.getRoomSpec();
            String size = (spec == null) ? "-" : spec.toSizeLabel();

            String timeAgo = timeAgoLabel(d.getLastUpdatedEpochMs());
            list.add(designCard(frame, d, title, client, size, timeAgo));
        }
        return list;
    }

    private JComponent designCard(AppFrame frame, Design design, String title, String client, String size, String timeAgo) {
        UiKit.RoundedPanel card = new UiKit.RoundedPanel(14, UiKit.WHITE);
        card.setBorderPaint(UiKit.BORDER);
        card.setLayout(new BorderLayout());
        card.setBorder(new EmptyBorder(10, 10, 10, 10));

        UiKit.RoundedPanel img = new UiKit.RoundedPanel(14, imagePlaceholderFill());
        img.setBorderPaint(isHighContrast() ? UiKit.BORDER : null);
        img.setPreferredSize(new Dimension(0, 130));
        img.setLayout(new GridBagLayout());
        JLabel imgLbl = new JLabel("Image");
        imgLbl.setForeground(softMuted());
        imgLbl.setFont(UiKit.scaled(imgLbl, Font.PLAIN, 0.92f));
        img.add(imgLbl);

        JPanel info = new JPanel();
        info.setOpaque(false);
        info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));
        info.setBorder(new EmptyBorder(10, 2, 6, 2));

        JLabel t = new JLabel(title);
        t.setForeground(UiKit.TEXT);
        t.setFont(UiKit.scaled(t, Font.BOLD, 1.00f));

        JLabel sub = new JLabel(client);
        sub.setForeground(UiKit.MUTED);
        sub.setFont(UiKit.scaled(sub, Font.PLAIN, 0.92f));
        sub.setBorder(new EmptyBorder(4, 0, 0, 0));

        JLabel meta = new JLabel(size + "   •   " + timeAgo);
        meta.setForeground(softMuted());
        meta.setFont(UiKit.scaled(meta, Font.PLAIN, 0.90f));
        meta.setBorder(new EmptyBorder(6, 0, 0, 0));

        info.add(t);
        info.add(sub);
        info.add(meta);

        JPanel actions = new JPanel(new BorderLayout(10, 0));
        actions.setOpaque(false);
        actions.setBorder(new EmptyBorder(8, 0, 0, 0));

        JButton open = UiKit.primaryButton("Open");
        open.setBorder(new EmptyBorder(8, 12, 8, 12));
        open.setFont(UiKit.scaled(open, Font.BOLD, 0.95f));
        open.addActionListener(e -> openDesign(design));

        JPanel iconRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        iconRow.setOpaque(false);

        JButton duplicate = UiKit.iconButton("⧉");
        duplicate.setToolTipText("Duplicate");
        duplicate.addActionListener(e -> {
            appState.getRepo().duplicate(design.getId());
            refreshGrid(frame);
        });

        JButton edit = UiKit.iconButton("✎");
        edit.setToolTipText("Edit Details");
        edit.addActionListener(e -> {
            if (appState != null) {
                appState.setCurrentDesignId(design.getId());
                router.show(ScreenKeys.DESIGN_DETAILS);
            }
        });

        JButton delete = UiKit.iconButton("🗑");
        delete.setToolTipText("Delete");
        delete.setForeground(UiKit.DANGER);
        delete.addActionListener(e -> {
            int ok = JOptionPane.showConfirmDialog(
                    this,
                    "Delete \"" + title + "\"?\nThis cannot be undone.",
                    "Confirm delete",
                    JOptionPane.YES_NO_OPTION
            );
            if (ok == JOptionPane.YES_OPTION) {
                appState.getRepo().delete(design.getId());
                // if current selection was this design, clear it
                if (design.getId().equals(appState.getCurrentDesignId())) {
                    appState.setCurrentDesignId(null);
                }
                refreshGrid(frame);
            }
        });

        iconRow.add(duplicate);
        iconRow.add(edit);
        iconRow.add(delete);

        actions.add(open, BorderLayout.CENTER);
        actions.add(iconRow, BorderLayout.EAST);

        JPanel body = new JPanel(new BorderLayout());
        body.setOpaque(false);
        body.add(info, BorderLayout.CENTER);
        body.add(actions, BorderLayout.SOUTH);

        card.add(img, BorderLayout.NORTH);
        card.add(body, BorderLayout.CENTER);

        card.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) {
                card.setFill(hoverFill());
                card.repaint();
            }
            @Override public void mouseExited(MouseEvent e) {
                card.setFill(UiKit.WHITE);
                card.repaint();
            }
            @Override public void mouseClicked(MouseEvent e) {
                // Clicking the card also opens
                if (e.getClickCount() == 2) openDesign(design);
            }
        });

        return card;
    }

    private void openDesign(Design d) {
        if (appState == null || d == null) return;
        appState.setCurrentDesignId(d.getId());
        router.show(ScreenKeys.PLANNER_2D);
    }

    /* ========================= helpers ========================= */

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
        // older: show date
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("d MMM yyyy", Locale.ENGLISH);
            return sdf.format(new Date(epochMs));
        } catch (Exception e) {
            return "Earlier";
        }
    }

    private JComponent linkChip(String text) {
        JLabel l = new JLabel(text);
        l.setOpaque(true);
        l.setBackground(isHighContrast() ? UiKit.WHITE : new Color(0xEEF2FF));
        l.setForeground(isHighContrast() ? UiKit.TEXT : UiKit.PRIMARY_DARK);
        l.setFont(UiKit.scaled(l, Font.BOLD, 0.92f));
        l.setBorder(new EmptyBorder(6, 10, 6, 10));
        return l;
    }

    // -------- reflection helpers (safe + compile-proof) --------

    private static Object invoke(Object target, String methodName) {
        if (target == null || methodName == null) return null;
        try {
            Method m = target.getClass().getMethod(methodName);
            m.setAccessible(true);
            return m.invoke(target);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static Double firstDouble(Object target, String... methodNames) {
        if (target == null || methodNames == null) return null;
        for (String name : methodNames) {
            try {
                Method m = target.getClass().getMethod(name);
                m.setAccessible(true);
                Object v = m.invoke(target);
                if (v instanceof Number) return ((Number) v).doubleValue();
            } catch (Exception ignored) {}
        }
        return null;
    }
}
