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
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DesignLibraryPage extends JPanel {

    private final Router router;
    private final AppState appState;

    // dynamic UI refs
    private JLabel showingLabel;
    private JPanel gridHost; // holds either empty state or grid panel

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
        content.add(searchFilterCard());
        content.add(Box.createVerticalStrut(16));
        content.add(metaRow());
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

    private JComponent headerRow() {
        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(false);

        JPanel left = new JPanel();
        left.setOpaque(false);
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("Design Library");
        title.setForeground(UiKit.TEXT);
        title.setFont(UiKit.semibold(title.getFont(), 24f));

        JLabel sub = new JLabel("Manage and organize your saved room designs");
        sub.setForeground(UiKit.MUTED);
        sub.setFont(UiKit.regular(sub.getFont(), 12.5f));
        sub.setBorder(new EmptyBorder(6, 0, 0, 0));

        left.add(title);
        left.add(sub);

        JButton cta = UiKit.primaryButton("+   Create New Design");
        cta.addActionListener(e -> this.router.show(ScreenKeys.NEW_DESIGN));

        row.add(left, BorderLayout.WEST);
        row.add(cta, BorderLayout.EAST);
        return row;
    }

    private JComponent searchFilterCard() {
        UiKit.RoundedPanel card = new UiKit.RoundedPanel(14, UiKit.WHITE);
        card.setBorderPaint(UiKit.BORDER);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(new EmptyBorder(12, 12, 12, 12));

        JPanel row1 = new JPanel(new BorderLayout(10, 0));
        row1.setOpaque(false);

        JTextField search = UiKit.searchField("Search designs by name, customer, or tags...");
        JButton filters = UiKit.ghostButton("Filters");
        filters.setPreferredSize(new Dimension(110, filters.getPreferredSize().height));

        row1.add(search, BorderLayout.CENTER);
        row1.add(filters, BorderLayout.EAST);

        JPanel tags = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 10));
        tags.setOpaque(false);
        tags.add(new JLabel("Tags:"));
        tags.add(UiKit.chipPrimary("Modern"));
        tags.add(UiKit.chip("Classic"));
        tags.add(UiKit.chip("Minimalist"));
        tags.add(UiKit.chip("Industrial"));
        tags.add(linkChip("+ More"));

        JPanel row3 = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        row3.setOpaque(false);

        JLabel roomShapeLbl = new JLabel("Room Shape:");
        roomShapeLbl.setForeground(UiKit.MUTED);
        roomShapeLbl.setFont(roomShapeLbl.getFont().deriveFont(11.5f));

        JComboBox<String> shapes = new JComboBox<>(new String[]{"All Shapes", "Rectangular", "Square", "L-Shape"});
        UiKit.styleDropdown(shapes);

        JLabel sizeLbl = new JLabel("Size:");
        sizeLbl.setForeground(UiKit.MUTED);
        sizeLbl.setFont(sizeLbl.getFont().deriveFont(11.5f));

        JComboBox<String> size = new JComboBox<>(new String[]{"All Sizes", "Small", "Medium", "Large"});
        UiKit.styleDropdown(size);

        row3.add(roomShapeLbl);
        row3.add(shapes);
        row3.add(sizeLbl);
        row3.add(size);

        card.add(row1);
        card.add(tags);
        card.add(row3);

        return card;
    }

    private JComponent metaRow() {
        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(false);

        showingLabel = new JLabel("Showing 0 designs    ");
        showingLabel.setForeground(UiKit.MUTED);
        showingLabel.setFont(showingLabel.getFont().deriveFont(11.8f));

        JLabel clear = new JLabel("Clear all filters");
        clear.setForeground(UiKit.PRIMARY);
        clear.setFont(clear.getFont().deriveFont(Font.PLAIN, 11.8f));

        JPanel leftWrap = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        leftWrap.setOpaque(false);
        leftWrap.add(showingLabel);
        leftWrap.add(clear);

        JPanel rightWrap = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        rightWrap.setOpaque(false);

        JLabel sortLbl = new JLabel("Sort by:");
        sortLbl.setForeground(UiKit.MUTED);
        sortLbl.setFont(sortLbl.getFont().deriveFont(11.8f));

        JComboBox<String> sort = new JComboBox<>(new String[]{"Most Recent", "Oldest", "Name A-Z"});
        UiKit.styleDropdown(sort);

        JButton grid = UiKit.iconButton("▦");
        JButton list = UiKit.iconButton("≡");

        rightWrap.add(sortLbl);
        rightWrap.add(sort);
        rightWrap.add(grid);
        rightWrap.add(list);

        row.add(leftWrap, BorderLayout.WEST);
        row.add(rightWrap, BorderLayout.EAST);
        return row;
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
        if (showingLabel != null) showingLabel.setText("Showing " + designs.size() + " designs    ");

        if (designs.isEmpty()) {
            gridHost.add(emptyState("No saved designs yet",
                    "Click “Create New Design” to start your first project."), BorderLayout.CENTER);
        } else {
            gridHost.add(grid(frame, designs), BorderLayout.CENTER);
        }

        revalidate();
        repaint();
    }

    private JComponent emptyState(String title, String subtitle) {
        UiKit.RoundedPanel card = new UiKit.RoundedPanel(14, UiKit.WHITE);
        card.setBorderPaint(UiKit.BORDER);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(new EmptyBorder(22, 22, 22, 22));

        JLabel t = new JLabel(title);
        t.setForeground(UiKit.TEXT);
        t.setFont(t.getFont().deriveFont(Font.BOLD, 14.5f));

        JLabel s = new JLabel(subtitle);
        s.setForeground(UiKit.MUTED);
        s.setFont(s.getFont().deriveFont(Font.PLAIN, 12.2f));
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

    private JComponent designCard(AppFrame frame, Design design, String title, String client, String size, String timeAgo) {
        UiKit.RoundedPanel card = new UiKit.RoundedPanel(14, UiKit.WHITE);
        card.setBorderPaint(UiKit.BORDER);
        card.setLayout(new BorderLayout());
        card.setBorder(new EmptyBorder(10, 10, 10, 10));

        UiKit.RoundedPanel img = new UiKit.RoundedPanel(14, new Color(0xE5E7EB));
        img.setPreferredSize(new Dimension(0, 130));
        img.setLayout(new GridBagLayout());
        JLabel imgLbl = new JLabel("Image");
        imgLbl.setForeground(new Color(0x9CA3AF));
        img.add(imgLbl);

        JPanel info = new JPanel();
        info.setOpaque(false);
        info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));
        info.setBorder(new EmptyBorder(10, 2, 6, 2));

        JLabel t = new JLabel(title);
        t.setForeground(UiKit.TEXT);
        t.setFont(t.getFont().deriveFont(Font.BOLD, 12.8f));

        JLabel sub = new JLabel(client);
        sub.setForeground(UiKit.MUTED);
        sub.setFont(sub.getFont().deriveFont(11.5f));
        sub.setBorder(new EmptyBorder(4, 0, 0, 0));

        JLabel meta = new JLabel(size + "   •   " + timeAgo);
        meta.setForeground(new Color(0x9CA3AF));
        meta.setFont(meta.getFont().deriveFont(11.2f));
        meta.setBorder(new EmptyBorder(6, 0, 0, 0));

        info.add(t);
        info.add(sub);
        info.add(meta);

        JPanel actions = new JPanel(new BorderLayout(10, 0));
        actions.setOpaque(false);
        actions.setBorder(new EmptyBorder(8, 0, 0, 0));

        JButton open = UiKit.primaryButton("Open");
        open.setBorder(new EmptyBorder(8, 12, 8, 12));
        open.setFont(open.getFont().deriveFont(Font.BOLD, 11.8f));
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
        edit.setToolTipText("Rename");
        edit.addActionListener(e -> {
            String newName = JOptionPane.showInputDialog(this, "Rename design:", title);
            if (newName != null) {
                newName = newName.trim();
                if (!newName.isEmpty()) {
                    design.setDesignName(newName);
                    appState.getRepo().upsert(design);
                    refreshGrid(frame);
                }
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
                card.setFill(new Color(0xFAFAFB));
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
        router.show(ScreenKeys.DESIGN_DETAILS);
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
        l.setBackground(new Color(0xEEF2FF));
        l.setForeground(UiKit.PRIMARY_DARK);
        l.setFont(l.getFont().deriveFont(Font.BOLD, 11.5f));
        l.setBorder(new EmptyBorder(6, 10, 6, 10));
        return l;
    }
}
