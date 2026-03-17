package com.roomviz.screens;

import com.roomviz.app.AppFrame;
import com.roomviz.app.Router;
import com.roomviz.app.ScreenKeys;
import com.roomviz.data.AppState;
import com.roomviz.data.Session;
import com.roomviz.model.Design;
import com.roomviz.model.RoomSpec;
import com.roomviz.model.User;
import com.roomviz.ui.Mini2DPreviewPanel;
import com.roomviz.ui.FontAwesome;
import com.roomviz.ui.UiKit;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import java.util.Locale;

public class DesignLibraryPage extends JPanel {
    private static final int GRID_GAP = 16;
    private static final int CARD_MIN_WIDTH = 270;
    private static final int PAGE_MAX_WIDTH = 1200;

    private final Router router;
    private final AppState appState;
    private final Session session;

    // dynamic UI refs
    private JLabel showingLabel;
    private JPanel gridHost; // holds either empty state or grid panel

    // filter/sort/view controls (wired)
    private JTextField searchField;
    private JComboBox<String> shapesBox;
    private JComboBox<String> sizeBox;
    private JComboBox<String> sortBox;
    private JLabel clearFiltersLabel;

    private JButton gridBtn;
    private JButton listBtn;
    private boolean gridMode = true;

    // Track filter state to avoid redundant/destructive refreshes
    private String lastAppliedFilterState = "";

    private static final String SEARCH_PLACEHOLDER_ADMIN = "Search designs by name, customer, or tags...";
    private static final String SEARCH_PLACEHOLDER_CUSTOMER = "Search designs by name...";

    // keep existing constructor (fallback)
    public DesignLibraryPage(AppFrame frame, Router router) {
        this(frame, router, null, null);
    }

    // constructor used by ShellScreen (updated)
    public DesignLibraryPage(AppFrame frame, Router router, AppState appState, Session session) {
        this.router = router;
        this.appState = appState;
        this.session = session;

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

        // Inner column that can scale up/down with the window.
        JPanel column = new JPanel();
        column.setOpaque(false);
        column.setLayout(new BoxLayout(column, BoxLayout.Y_AXIS));
        column.setMaximumSize(new Dimension(PAGE_MAX_WIDTH, Integer.MAX_VALUE));

        column.add(headerRow());
        column.add(vSpace(28));
        column.add(searchFilterCard(frame));
        column.add(vSpace(32));
        column.add(metaRow(frame));
        column.add(vSpace(14));

        // dynamic designs grid
        gridHost = new JPanel(new BorderLayout());
        gridHost.setOpaque(false);
        column.add(gridHost);

        // Add resize listener for responsive grid columns
        gridHost.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                refreshGrid(frame);
            }
        });

        column.add(vSpace(40));

        // Wrapper keeps content centered while allowing wider screens to use space.
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);
        wrapper.setMaximumSize(new Dimension(PAGE_MAX_WIDTH, Integer.MAX_VALUE));
        wrapper.add(column, BorderLayout.CENTER);

        content.add(wrapper, gbc);

        JScrollPane scroller = new JScrollPane(content);
        scroller.setBorder(BorderFactory.createEmptyBorder());
        scroller.getViewport().setOpaque(false);
        scroller.getViewport().setBackground(UiKit.BG);
        scroller.setOpaque(false);
        scroller.getVerticalScrollBar().setUnitIncrement(18);

        add(scroller, BorderLayout.CENTER);

        // Refresh when this screen is shown
        router.addListener(key -> {
            if (ScreenKeys.DESIGN_LIBRARY.equals(key)) {
                refreshGrid(frame);
            }
        });

        refreshGrid(frame);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        if (!UiKit.isHighContrastMode() && !UiKit.isDarkBlueMode()) {
            int w = getWidth();
            int h = getHeight();
            
            // Base Gradient: Soft purple to soft cyan
            LinearGradientPaint lgp = new LinearGradientPaint(
                    0, 0, w, h,
                    new float[]{ 0.0f, 0.5f, 1.0f },
                    new Color[]{ new Color(223, 172, 255), new Color(210, 190, 250), new Color(130, 240, 240) }
            );
            g2.setPaint(lgp);
            g2.fillRect(0, 0, w, h);
            
            // Abstract wave 1
            g2.setPaint(new Color(255, 255, 255, 60));
            java.awt.geom.Path2D wave = new java.awt.geom.Path2D.Double();
            wave.moveTo(0, h * 0.4);
            wave.curveTo(w * 0.3, h * 0.6, w * 0.6, h * 0.2, w, h * 0.5);
            wave.lineTo(w, h);
            wave.lineTo(0, h);
            wave.closePath();
            g2.fill(wave);
            
            // Abstract wave 2
            g2.setPaint(new Color(255, 255, 255, 30));
            java.awt.geom.Path2D wave2 = new java.awt.geom.Path2D.Double();
            wave2.moveTo(0, h * 0.6);
            wave2.curveTo(w * 0.4, h * 0.8, w * 0.8, h * 0.3, w, h * 0.7);
            wave2.lineTo(w, h);
            wave2.lineTo(0, h);
            wave2.closePath();
            g2.fill(wave2);

        } else {
            g2.setColor(UiKit.BG);
            g2.fillRect(0, 0, getWidth(), getHeight());
        }
        g2.dispose();
    }

    private boolean isCustomer() {
        if (session == null) return false;
        User u = session.getCurrentUser();
        if (u == null) return false;
        return u.isCustomer();
    }

    private boolean isHighContrast() {
        return UiKit.isHighContrastMode();
    }

    private Color softMuted() {
        // neutral muted used in a few places (placeholder/meta)
        return isHighContrast() ? UiKit.TEXT : UiKit.MUTED;
    }

    private JComponent headerRow() {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));

        JLabel h1 = new JLabel("Design Library", SwingConstants.CENTER);
        h1.setForeground(UiKit.TEXT);
        h1.setFont(UiKit.scaled(h1, Font.BOLD, 1.85f));
        h1.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel sub = new JLabel(
                isCustomer()
                        ? "View your saved room designs"
                        : "Manage and organize your saved room designs",
                SwingConstants.CENTER
        );
        sub.setForeground(UiKit.MUTED);
        sub.setFont(UiKit.scaled(sub, Font.PLAIN, 1.05f));
        sub.setBorder(new EmptyBorder(8, 0, 0, 0));
        sub.setAlignmentX(Component.CENTER_ALIGNMENT);

        p.add(h1);
        p.add(sub);

        // Create New Design is ADMIN-ONLY
        if (!isCustomer()) {
            JButton cta = UiKit.primaryButton("+   Create New Design");
            cta.setFont(UiKit.scaled(cta, Font.BOLD, 1.00f));
            cta.setPreferredSize(new Dimension(220, 42));
            cta.setMaximumSize(new Dimension(220, 42));
            cta.setAlignmentX(Component.CENTER_ALIGNMENT);
            cta.addActionListener(e -> this.router.show(ScreenKeys.NEW_DESIGN));

            p.add(vSpace(22));
            p.add(cta);
        } else {
            p.add(vSpace(22));
        }

        return p;
    }

    // accept frame and wire listeners
    private JComponent searchFilterCard(AppFrame frame) {
        UiKit.RoundedPanel card = new UiKit.RoundedPanel(16, UiKit.WHITE);
        card.setOpaque(false);
        card.setBorderPaint(UiKit.BORDER);
        card.setLayout(new GridBagLayout());
        card.setBorder(new EmptyBorder(18, 18, 18, 18));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(0, 0, 16, 0);

        // Search Bar Section
        JPanel searchRow = new JPanel(new BorderLayout(14, 0));
        searchRow.setOpaque(false);

        String placeholder = isCustomer() ? SEARCH_PLACEHOLDER_CUSTOMER : SEARCH_PLACEHOLDER_ADMIN;
        UiKit.SearchResult sr = UiKit.searchFieldWithIcon(placeholder);
        searchField = sr.field;
        sr.panel.setPreferredSize(new Dimension(300, 42));

        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { refreshGrid(frame); }
            @Override public void removeUpdate(DocumentEvent e) { refreshGrid(frame); }
            @Override public void changedUpdate(DocumentEvent e) { refreshGrid(frame); }
        });

        searchRow.add(sr.panel, BorderLayout.CENTER);
        card.add(searchRow, gbc);

        // Filters Section (Responsive Flow)
        gbc.gridy = 1;
        gbc.insets = new Insets(0, 0, 0, 0);

        JPanel filtersRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 10));
        filtersRow.setOpaque(false);

        JLabel shapeLbl = new JLabel("Room Shape:");
        shapeLbl.setForeground(UiKit.MUTED);
        shapeLbl.setFont(UiKit.scaled(shapeLbl, Font.BOLD, 0.88f));

        shapesBox = new JComboBox<>(new String[]{"All Shapes", "Rectangular", "Square", "L-Shape"});
        UiKit.styleDropdown(shapesBox);
        shapesBox.setPreferredSize(new Dimension(140, 36));
        shapesBox.addActionListener(e -> refreshGrid(frame));

        JLabel sizeLbl = new JLabel("Size:");
        sizeLbl.setForeground(UiKit.MUTED);
        sizeLbl.setFont(UiKit.scaled(sizeLbl, Font.BOLD, 0.88f));

        sizeBox = new JComboBox<>(new String[]{"All Sizes", "Small", "Medium", "Large"});
        UiKit.styleDropdown(sizeBox);
        sizeBox.setPreferredSize(new Dimension(120, 36));
        sizeBox.addActionListener(e -> refreshGrid(frame));

        JButton resetBtn = UiKit.ghostButton("Reset All");
        resetBtn.setFont(UiKit.scaled(resetBtn, Font.BOLD, 0.88f));
        resetBtn.setPreferredSize(new Dimension(100, 36));
        resetBtn.addActionListener(e -> {
            resetFilters();
            refreshGrid(frame);
        });

        filtersRow.add(shapeLbl);
        filtersRow.add(shapesBox);
        filtersRow.add(Box.createHorizontalStrut(12));
        filtersRow.add(sizeLbl);
        filtersRow.add(sizeBox);
        filtersRow.add(Box.createHorizontalStrut(12));
        filtersRow.add(resetBtn);

        card.add(filtersRow, gbc);

        return card;
    }

    // accept frame and wire sort + view toggle + clear filters
    private JComponent metaRow(AppFrame frame) {
        JPanel row = new JPanel(new GridBagLayout());
        row.setOpaque(false);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0; gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;

        showingLabel = new JLabel("Showing 0 designs   ");
        showingLabel.setForeground(UiKit.MUTED);
        showingLabel.setFont(UiKit.scaled(showingLabel, Font.BOLD, 0.92f));

        clearFiltersLabel = new JLabel("Clear all filters");
        clearFiltersLabel.setForeground(UiKit.PRIMARY);
        clearFiltersLabel.setFont(UiKit.scaled(clearFiltersLabel, Font.BOLD, 0.92f));
        clearFiltersLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        clearFiltersLabel.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                resetFilters();
                refreshGrid(frame);
            }
        });

        JPanel leftWrap = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        leftWrap.setOpaque(false);
        leftWrap.add(showingLabel);
        leftWrap.add(clearFiltersLabel);

        JPanel rightWrap = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 5));
        rightWrap.setOpaque(false);

        JLabel sortLbl = new JLabel("Sort by:");
        sortLbl.setForeground(UiKit.MUTED);
        sortLbl.setFont(UiKit.scaled(sortLbl, Font.PLAIN, 0.95f));

        sortBox = new JComboBox<>(new String[]{"Most Recent", "Oldest", "Name A-Z"});
        UiKit.styleDropdown(sortBox);
        sortBox.addActionListener(e -> refreshGrid(frame));

        gridBtn = UiKit.iconButton(FontAwesome.TH_LARGE);
        listBtn = UiKit.iconButton(FontAwesome.BARS);

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

        row.add(leftWrap, gbc);
        gbc.gridx = 1; gbc.weightx = 0.0;
        gbc.anchor = GridBagConstraints.EAST;
        row.add(rightWrap, gbc);
        return row;
    }

    private void resetFilters() {
        if (searchField != null) searchField.setText("");
        if (shapesBox != null) shapesBox.setSelectedIndex(0);
        if (sizeBox != null) sizeBox.setSelectedIndex(0);
        if (sortBox != null) sortBox.setSelectedIndex(0);
        gridMode = true;
    }

    // Grid management

    private void refreshGrid(AppFrame frame) {
        if (appState == null) {
            gridHost.removeAll();
            gridHost.add(emptyState("AppState not wired",
                    "Please use the constructor in ShellScreen."), BorderLayout.CENTER);
            if (showingLabel != null) showingLabel.setText("Showing 0 designs    ");
            revalidate();
            repaint();
            return;
        }

        List<Design> designs = appState.getRepo().getAllSortedByLastUpdatedDesc();

        // Check if rebuild is necessary
        String q = (searchField == null) ? "" : searchField.getText();
        String placeholder = isCustomer() ? SEARCH_PLACEHOLDER_CUSTOMER : SEARCH_PLACEHOLDER_ADMIN;

        if (q != null && (q.trim().equalsIgnoreCase(placeholder) || q.trim().isEmpty())) {
            q = "";
        }
        q = q.trim();

        String shape = (shapesBox == null) ? "" : (String) shapesBox.getSelectedItem();
        String size = (sizeBox == null) ? "" : (String) sizeBox.getSelectedItem();
        String sort = (sortBox == null) ? "" : (String) sortBox.getSelectedItem();

        // Track metrics for state change check
        int count = (designs == null) ? 0 : designs.size();
        long latest = (designs == null || designs.isEmpty()) ? 0 : designs.get(0).getLastUpdatedEpochMs();

        int columns = gridMode ? getGridColumns() : 1;
        String state = count + "|" + latest + "|" + q + "|" + shape + "|" + size + "|" + sort + "|" + gridMode + "|" + columns;

        // If nothing changed and we already have content, skip.
        if (state.equals(lastAppliedFilterState) && gridHost.getComponentCount() > 0) {
            return;
        }
        lastAppliedFilterState = state;

        gridHost.removeAll();
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
        String placeholder = isCustomer() ? SEARCH_PLACEHOLDER_CUSTOMER : SEARCH_PLACEHOLDER_ADMIN;
        if (placeholder.equalsIgnoreCase(q)) q = "";
        q = safe(q, "").toLowerCase(Locale.ENGLISH);

        String shapeFilter = (shapesBox == null) ? "All Shapes" : (String) shapesBox.getSelectedItem();
        String sizeFilter = (sizeBox == null) ? "All Sizes" : (String) sizeBox.getSelectedItem();

        List<Design> out = new ArrayList<>();
        if (designs == null) return out;

        for (Design d : designs) {
            if (d == null) continue;

            // search (name + customer for admin)
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

        // admin can search by customer; customer doesn't need it
        if (!isCustomer()) {
            sb.append(safe(d.getCustomerName(), "")).append(" ");
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
        UiKit.RoundedPanel card = new UiKit.RoundedPanel(16, UiKit.WHITE);
        card.setOpaque(false);
        card.setBorderPaint(UiKit.BORDER);
        card.setLayout(new GridBagLayout()); // Center the box
        card.setBorder(new EmptyBorder(60, 20, 60, 20));

        JPanel box = new JPanel();
        box.setOpaque(false);
        box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));

        JLabel t = new JLabel(title, SwingConstants.CENTER);
        t.setForeground(UiKit.TEXT);
        t.setFont(UiKit.scaled(t, Font.BOLD, 1.4f));
        t.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel s = new JLabel(subtitle, SwingConstants.CENTER);
        s.setForeground(UiKit.MUTED);
        s.setFont(UiKit.scaled(s, Font.PLAIN, 1.1f));
        s.setBorder(new EmptyBorder(10, 0, 0, 0));
        s.setAlignmentX(Component.CENTER_ALIGNMENT);

        box.add(t);
        box.add(s);

        card.add(box);
        return card;
    }

    private JComponent grid(AppFrame frame, List<Design> designs) {
        JPanel grid = new JPanel(new GridLayout(0, getGridColumns(), GRID_GAP, GRID_GAP));
        grid.setOpaque(false);

        for (Design d : designs) {
            String title = safe(d.getDesignName(), "Untitled Design");
            String client = safe(d.getCustomerName(), ""); // customer might not need it

            RoomSpec spec = d.getRoomSpec();
            String size = (spec == null) ? "-" : spec.toSizeLabel();

            String timeAgo = timeAgoLabel(d.getLastUpdatedEpochMs());
            grid.add(designCard(frame, d, title, client, size, timeAgo));
        }
        return grid;
    }

    // list view (same cards, 1 per row)
    private JComponent list(AppFrame frame, List<Design> designs) {
        JPanel list = new JPanel(new GridLayout(0, 1, GRID_GAP, GRID_GAP));
        list.setOpaque(false);

        for (Design d : designs) {
            String title = safe(d.getDesignName(), "Untitled Design");
            String client = safe(d.getCustomerName(), "");

            RoomSpec spec = d.getRoomSpec();
            String size = (spec == null) ? "-" : spec.toSizeLabel();

            String timeAgo = timeAgoLabel(d.getLastUpdatedEpochMs());
            list.add(designCard(frame, d, title, client, size, timeAgo));
        }
        return list;
    }

    private JComponent designCard(AppFrame frame, Design design, String title, String client, String size, String timeAgo) {
        UiKit.RoundedPanel card = new UiKit.RoundedPanel(16, UiKit.WHITE);
        card.setOpaque(false);
        card.setBorderPaint(UiKit.BORDER);
        card.setLayout(new BorderLayout(0, 12));
        card.setBorder(new EmptyBorder(14, 14, 14, 14));
        card.setPreferredSize(new Dimension(CARD_MIN_WIDTH, 300));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));

        // Preview Area
        Mini2DPreviewPanel img = new Mini2DPreviewPanel();
        img.setPreferredSize(new Dimension(CARD_MIN_WIDTH - 28, 160));
        img.setDesign(design);

        // Info Area
        JPanel info = new JPanel();
        info.setOpaque(false);
        info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));
        info.setBorder(new EmptyBorder(0, 4, 0, 4));

        JLabel t = new JLabel(title);
        t.setForeground(UiKit.TEXT);
        t.setFont(UiKit.scaled(t, Font.BOLD, 1.15f));
        t.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel sub = new JLabel(isCustomer() ? "" : safe(client, "Unknown Client"));
        sub.setForeground(UiKit.MUTED);
        sub.setFont(UiKit.scaled(sub, Font.PLAIN, 0.95f));
        sub.setBorder(new EmptyBorder(4, 0, 0, 0));
        sub.setAlignmentX(Component.LEFT_ALIGNMENT);
        sub.setVisible(!isCustomer());

        JLabel meta = new JLabel(size + "   •   " + timeAgo);
        meta.setForeground(softMuted());
        meta.setFont(UiKit.scaled(meta, Font.PLAIN, 0.88f));
        meta.setBorder(new EmptyBorder(8, 0, 0, 0));
        meta.setAlignmentX(Component.LEFT_ALIGNMENT);

        info.add(t);
        if (!isCustomer()) info.add(sub);
        info.add(meta);

        // Actions Area
        JPanel actionsRow = new JPanel(new BorderLayout(12, 0));
        actionsRow.setOpaque(false);
        actionsRow.setBorder(new EmptyBorder(4, 0, 0, 0));

        if (isCustomer()) {
            JButton open2d = UiKit.primaryButton("Open 2D");
            open2d.setToolTipText("Open in 2D View (read-only)");
            open2d.setMargin(new Insets(6, 16, 6, 16));
            open2d.setFont(UiKit.scaled(open2d, Font.BOLD, 0.95f));
            open2d.addActionListener(e -> openDesign2D(design));

            JButton open3d = UiKit.ghostButton("View 3D");
            open3d.setToolTipText("Open in 3D View");
            open3d.setMargin(new Insets(6, 14, 6, 14));
            open3d.setFont(UiKit.scaled(open3d, Font.BOLD, 0.90f));
            open3d.addActionListener(e -> openDesign3D(design));

            JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
            row.setOpaque(false);
            row.add(open2d);
            row.add(open3d);

            actionsRow.add(row, BorderLayout.WEST);
        } else {
            JButton open = UiKit.primaryButton("Open");
            open.setToolTipText("Open in 2D Planner");
            open.setMargin(new Insets(6, 16, 6, 16));
            open.setFont(UiKit.scaled(open, Font.BOLD, 0.95f));
            open.addActionListener(e -> openDesign2D(design));

            JPanel iconRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
            iconRow.setOpaque(false);

            JButton duplicate = UiKit.iconButton(FontAwesome.CLONE);
            duplicate.setToolTipText("Duplicate");
            duplicate.addActionListener(e -> {
                appState.getRepo().duplicate(design.getId());
                refreshGrid(frame);
            });

            JButton edit = UiKit.ghostButton("Edit");
            edit.setToolTipText("Edit design info");
            edit.setFont(UiKit.scaled(edit, Font.BOLD, 0.88f));
            edit.addActionListener(e -> {
                if (appState != null) {
                    appState.setCurrentDesignId(design.getId());
                    router.show(ScreenKeys.DESIGN_DETAILS);
                }
            });

            JButton delete = UiKit.ghostButton("Delete");
            delete.setToolTipText("Delete");
            delete.setForeground(UiKit.DANGER);
            delete.setFont(UiKit.scaled(delete, Font.BOLD, 0.88f));
            delete.addActionListener(e -> {
                int ok = JOptionPane.showConfirmDialog(
                        this,
                        "Delete \"" + title + "\"?\nThis cannot be undone.",
                        "Confirm delete",
                        JOptionPane.YES_NO_OPTION
                );
                if (ok == JOptionPane.YES_OPTION) {
                    appState.getRepo().delete(design.getId());
                    if (design.getId().equals(appState.getCurrentDesignId())) {
                        appState.setCurrentDesignId(null);
                    }
                    refreshGrid(frame);
                }
            });

            iconRow.add(duplicate);
            iconRow.add(edit);
            iconRow.add(delete);

            actionsRow.add(open, BorderLayout.CENTER);
            actionsRow.add(iconRow, BorderLayout.EAST);
        }

        JPanel main = new JPanel();
        main.setOpaque(false);
        main.setLayout(new BorderLayout(0, 12));
        main.add(img, BorderLayout.NORTH);
        main.add(info, BorderLayout.CENTER);

        card.add(main, BorderLayout.CENTER);
        card.add(actionsRow, BorderLayout.SOUTH);

        // Interactivity handlers
        card.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) {
                card.setFill(UiKit.CARD_HOVER);
                card.setBorderPaint(UiKit.PRIMARY);
                card.repaint();
            }
            @Override public void mouseExited(MouseEvent e) {
                card.setFill(UiKit.WHITE);
                card.setBorderPaint(UiKit.BORDER);
                card.repaint();
            }
            @Override public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) openDesign2D(design);
            }
        });

        return card;
    }

    private void openDesign2D(Design d) {
        if (appState == null || d == null) return;
        appState.setCurrentDesignId(d.getId());
        router.show(ScreenKeys.PLANNER_2D);
    }

    private void openDesign3D(Design d) {
        if (appState == null || d == null) return;
        appState.setCurrentDesignId(d.getId());
        router.show(ScreenKeys.VIEW_3D);
    }

    // Utils

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

    // Reflection utils

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

    private static Component vSpace(int px) {
        return Box.createRigidArea(new Dimension(0, px));
    }

    private int getGridColumns() {
        int hostWidth = gridHost == null ? 0 : gridHost.getWidth();
        if (hostWidth <= 0) hostWidth = PAGE_MAX_WIDTH;
        int cardWithGap = CARD_MIN_WIDTH + GRID_GAP;
        return Math.max(1, (hostWidth + GRID_GAP) / cardWithGap);
    }

    private static class ScrollablePanel extends JPanel implements Scrollable {
        @Override public Dimension getPreferredScrollableViewportSize() { return super.getPreferredSize(); }
        @Override public int getScrollableUnitIncrement(Rectangle r, int o, int d) { return 18; }
        @Override public int getScrollableBlockIncrement(Rectangle r, int o, int d) { return r.height; }
        @Override public boolean getScrollableTracksViewportWidth() { return true; }
        @Override public boolean getScrollableTracksViewportHeight() { return false; }
    }
}