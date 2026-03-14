package com.roomviz.screens;

import com.roomviz.app.AppFrame;
import com.roomviz.app.Router;
import com.roomviz.app.ScreenKeys;
import com.roomviz.data.AppState;
import com.roomviz.model.Design;
import com.roomviz.model.RoomSpec;
import com.roomviz.ui.RoomCanvas;
import com.roomviz.ui.UiKit;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Design Details Page (Step 1 wired)
 * - Opens when user clicks a design in the Design Library.
 * - Shows real data from repository via AppState.
 */
public class DesignDetailsPage extends JPanel {

    private final Router router;
    private final AppState appState;

    // ===== Palette (aligned with other pages) =====
    private static final Color TEXT = new Color(0x111827);
    private static final Color MUTED = new Color(0x6B7280);
    private static final Color BORDER = new Color(0xE5E7EB);
    private static final Color WHITE = Color.WHITE;

    private static final Color PRIMARY = new Color(0x4F46E5);
    private static final Color PRIMARY_DARK = new Color(0x6D28D9);

    private static final Color DANGER = new Color(0xEF4444);
    private static final Color WARNING_BG = new Color(0xFFFBEB);
    private static final Color WARNING_BORDER = new Color(0xFDE68A);
    private static final Color WARNING_TEXT = new Color(0x92400E);

    // ===== Dynamic refs (so we can refresh when rename/duplicate/delete) =====
    private JLabel headerTitle;
    private JLabel headerSubtitle;

    private JLabel customerName;
    private JLabel customerEmail;
    private JLabel roomTypeVal;
    private JLabel roomSizeVal;
    private JLabel roomShapeVal;
    private JLabel themeText;

    private JTextArea notesArea;
    
    private RoomCanvas previewCanvas; // Real preview

    private JPanel rootContent;      // either details layout or empty state
    private JPanel mainGridHost;     // holds main grid

    // ✅ keep existing constructor (fallback)
    public DesignDetailsPage(AppFrame frame, Router router) {
        this(frame, router, null);
    }

    // ✅ new constructor used by ShellScreen
    public DesignDetailsPage(AppFrame frame, Router router, AppState appState) {
        this.router = router;
        this.appState = appState;

        setLayout(new BorderLayout());
        setOpaque(false);

        rootContent = new JPanel();
        rootContent.setOpaque(false);
        rootContent.setLayout(new BoxLayout(rootContent, BoxLayout.Y_AXIS));
        rootContent.setBorder(new EmptyBorder(16, 18, 16, 18));

        // header + grid host
        rootContent.add(buildHeader(frame));
        rootContent.add(Box.createVerticalStrut(14));

        mainGridHost = new JPanel(new BorderLayout());
        mainGridHost.setOpaque(false);
        rootContent.add(mainGridHost);

        JScrollPane scroll = new JScrollPane(rootContent);
        scroll.setBorder(null);
        scroll.getViewport().setOpaque(false);
        scroll.setOpaque(false);
        scroll.getVerticalScrollBar().setUnitIncrement(16);

        add(scroll, BorderLayout.CENTER);

        // ✅ Listen for navigation
        router.addListener(key -> {
            if (ScreenKeys.DESIGN_DETAILS.equals(key)) {
                refresh(frame);
            }
        });

        refresh(frame);
    }

    /* ===================== Refresh ===================== */

    private void refresh(AppFrame frame) {
        // If appState not wired OR no selection -> show empty state
        if (appState == null || appState.getCurrentDesignId() == null) {
            mainGridHost.removeAll();
            customerName = null; // Invalidate cache
            mainGridHost.add(emptyState(
                    "No design selected",
                    "Go back to the Design Library and open a design."
            ), BorderLayout.CENTER);

            if (headerTitle != null) headerTitle.setText("Design Details");
            if (headerSubtitle != null) headerSubtitle.setText("—");

            revalidate();
            repaint();
            return;
        }

        Design d = appState.getRepo().getById(appState.getCurrentDesignId());
        if (d == null) {
            mainGridHost.removeAll();
            customerName = null; // Invalidate cache
            mainGridHost.add(emptyState(
                    "Design not found",
                    "This design may have been deleted. Go back to the Design Library."
            ), BorderLayout.CENTER);

            if (headerTitle != null) headerTitle.setText("Design Details");
            if (headerSubtitle != null) headerSubtitle.setText("—");

            revalidate();
            repaint();
            return;
        }

        // Build main grid only once, then just populate values on each refresh
        // Fix: Force rebuild if customerName is null or not showing
        if (mainGridHost.getComponentCount() == 0 || customerName == null) {
            mainGridHost.removeAll();
            mainGridHost.add(buildMainGrid(), BorderLayout.CENTER);
            mainGridHost.revalidate(); // Ensure hierarchy is updated
        }

        bindDesignToUI(d);
        revalidate();
        repaint();
    }

    private void bindDesignToUI(Design d) {
        String title = safe(d.getDesignName(), "Untitled Design");
        headerTitle.setText(title);
        headerSubtitle.setText("Last edited " + timeAgoLabel(d.getLastUpdatedEpochMs()));

        // Customer (we only store name in Step 1, so email is optional placeholder)
        String cust = safe(d.getCustomerName(), "Unknown Customer");
        customerName.setText(cust);
        customerEmail.setText(generateEmailHint(cust));

        RoomSpec spec = d.getRoomSpec();

        String unit = (spec == null) ? "" : safe(spec.getUnit(), "");
        String shape = (spec == null) ? "-" : safe(spec.getShape(), "-");
        String scheme = (spec == null) ? "-" : safe(spec.getColorScheme(), "-");
        String type = (spec == null) ? "-" : safe(spec.getRoomType(), "Room");

        roomTypeVal.setText(type);
        roomShapeVal.setText(shape);

        // Size label
        if (spec == null || spec.getWidth() <= 0 || spec.getLength() <= 0) {
            roomSizeVal.setText("-");
        } else {
            double w = spec.getWidth();
            double l = spec.getLength();
            double area = w * l;
            roomSizeVal.setText(formatNumber(w) + " " + unit + " × " + formatNumber(l) + " " + unit +
                    " (" + formatNumber(area) + " sq " + unit + ")");
        }

        themeText.setText(scheme);

        // Notes
        String notes = safe(d.getNotes(), "");
        if (notes.isEmpty()) {
            notesArea.setText("No notes added yet.");
            notesArea.setForeground(MUTED);
        } else {
            notesArea.setText(notes);
            notesArea.setForeground(TEXT);
        }
        
        // Update preview
        if (previewCanvas != null) {
            // RoomCanvas uses setRoomSpec and setItems
            previewCanvas.setRoomSpec(d.getRoomSpec());
            previewCanvas.setItems(d.getItems());
            previewCanvas.repaint();
        }
    }

    /* ===================== Header ===================== */

    private JComponent buildHeader(AppFrame frame) {
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);

        // Left: back + title/subtitle
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        left.setOpaque(false);

        JButton back = UiKit.iconButton("←");
        back.setToolTipText("Back to Design Library");
        back.addActionListener(e -> router.show(ScreenKeys.DESIGN_LIBRARY));

        JPanel titleBox = new JPanel();
        titleBox.setOpaque(false);
        titleBox.setLayout(new BoxLayout(titleBox, BoxLayout.Y_AXIS));

        headerTitle = new JLabel("Design Details");
        headerTitle.setForeground(TEXT);
        headerTitle.setFont(headerTitle.getFont().deriveFont(Font.BOLD, 18f));

        headerSubtitle = new JLabel("—");
        headerSubtitle.setForeground(MUTED);
        headerSubtitle.setFont(headerSubtitle.getFont().deriveFont(Font.PLAIN, 11.5f));

        titleBox.add(headerTitle);
        titleBox.add(Box.createVerticalStrut(3));
        titleBox.add(headerSubtitle);

        left.add(back);
        left.add(titleBox);

        // Right: actions
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        actions.setOpaque(false);

        JButton duplicate = ghostButton("Duplicate");
        JButton edit = ghostButton("Edit Details");
        JButton delete = dangerOutlineButton("Delete");

        duplicate.addActionListener(e -> {
            if (appState == null || appState.getCurrentDesignId() == null) return;

            Design copy = appState.getRepo().duplicate(appState.getCurrentDesignId());
            if (copy != null) {
                appState.setCurrentDesignId(copy.getId());
                JOptionPane.showMessageDialog(this, "Duplicated ✅");
            }
        });

        edit.addActionListener(e -> {
            if (appState == null || appState.getCurrentDesignId() == null) return;
            Design d = appState.getRepo().getById(appState.getCurrentDesignId());
            if (d == null) return;

            new EditDesignDialog(
                    SwingUtilities.getWindowAncestor(this),
                    d,
                    appState,
                    () -> refresh(frame)
            ).setVisible(true);
        });

        delete.addActionListener(e -> {
            if (appState == null || appState.getCurrentDesignId() == null) return;
            Design d = appState.getRepo().getById(appState.getCurrentDesignId());
            String current = d == null ? "this design" : safe(d.getDesignName(), "this design");

            int ok = JOptionPane.showConfirmDialog(
                    this,
                    "Delete \"" + current + "\"?\nThis cannot be undone.",
                    "Confirm delete",
                    JOptionPane.YES_NO_OPTION
            );
            if (ok == JOptionPane.YES_OPTION) {
                appState.getRepo().delete(appState.getCurrentDesignId());
                appState.setCurrentDesignId(null);
                router.show(ScreenKeys.DESIGN_LIBRARY);
            }
        });

        actions.add(duplicate);
        actions.add(edit);
        actions.add(delete);

        header.add(left, BorderLayout.WEST);
        header.add(actions, BorderLayout.EAST);
        return header;
    }

    /* ===================== Main grid ===================== */

    private JComponent buildMainGrid() {
        JPanel grid = new JPanel(new GridBagLayout());
        grid.setOpaque(false);

        GridBagConstraints gc = new GridBagConstraints();
        gc.gridy = 0;
        gc.fill = GridBagConstraints.BOTH;
        gc.weighty = 1;

        // Left column
        JPanel leftCol = new JPanel();
        leftCol.setOpaque(false);
        leftCol.setLayout(new BoxLayout(leftCol, BoxLayout.Y_AXIS));

        leftCol.add(buildPreviewCard());
        leftCol.add(Box.createVerticalStrut(14));
        leftCol.add(buildNotesCard());
        // removed activity log

        // Right column
        JPanel rightCol = new JPanel();
        rightCol.setOpaque(false);
        rightCol.setLayout(new BoxLayout(rightCol, BoxLayout.Y_AXIS));

        rightCol.add(buildInfoCard());
        rightCol.add(Box.createVerticalStrut(14));
        rightCol.add(buildTagsCard());
        // removed quick stats

        // Add left
        gc.gridx = 0;
        gc.weightx = 1.0;
        gc.insets = new Insets(0, 0, 0, 0);
        grid.add(leftCol, gc);

        // Add right
        gc.gridx = 1;
        gc.weightx = 0.38;
        gc.insets = new Insets(0, 14, 0, 0);
        grid.add(rightCol, gc);

        return grid;
    }

    /* ===================== Cards ===================== */

    private JComponent buildPreviewCard() {
        RoundedPanel card = cardPanel();
        card.setLayout(new BorderLayout());
        card.setBorder(new EmptyBorder(14, 14, 14, 14));

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);

        JLabel title = sectionTitle("Design Preview");

        JPanel toggle = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        toggle.setOpaque(false);

        JToggleButton v2d = toggleChip("2D View", true);
        JToggleButton v3d = toggleChip("3D View", false);

        ButtonGroup bg = new ButtonGroup();
        bg.add(v2d);
        bg.add(v3d);

        toggle.add(v2d);
        toggle.add(v3d);

        top.add(title, BorderLayout.WEST);
        top.add(toggle, BorderLayout.EAST);

        // Use RoomCanvas for real preview
        // We need to pass the current current design to it. 
        // Since we are in buildPreviewCard (called once), we need a reference to modify it later or bind it.
        // Actually, let's create a RoomCanvas and store it in a field so bindDesignToUI can update it.
        
        previewCanvas = new RoomCanvas(null); // Will be set in bindDesignToUI
        previewCanvas.setPreferredSize(new Dimension(0, 300));
        
        JPanel previewWrap = new JPanel(new BorderLayout());
        previewWrap.setOpaque(false);
        previewWrap.setBorder(new EmptyBorder(12, 0, 12, 0));

        JPanel previewIcons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        previewIcons.setOpaque(false);

        JButton full = iconPill("⛶");
        full.setToolTipText("Fullscreen (demo)");
        JButton dl = iconPill("⬇");
        dl.setToolTipText("Download (demo)");

        previewIcons.add(full);
        previewIcons.add(dl);

        JPanel overlay = new JPanel(new BorderLayout());
        overlay.setOpaque(false);
        overlay.add(previewIcons, BorderLayout.NORTH);

        JPanel stack = new JPanel();
        stack.setOpaque(false);
        stack.setLayout(new OverlayLayout(stack));
        stack.add(overlay);
        stack.add(previewCanvas);

        previewWrap.add(stack, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new GridLayout(1, 2, 10, 0));
        bottom.setOpaque(false);

        JButton edit2d = primaryPill("Edit in 2D");
        edit2d.addActionListener(e -> router.show(ScreenKeys.PLANNER_2D));

        JButton view3d = primaryGradientPill("View in 3D");
        view3d.addActionListener(e -> router.show(ScreenKeys.VIEW_3D));

        bottom.add(edit2d);
        bottom.add(view3d);

        card.add(top, BorderLayout.NORTH);
        card.add(previewWrap, BorderLayout.CENTER);
        card.add(bottom, BorderLayout.SOUTH);
        return card;
    }

    private JComponent buildNotesCard() {
        RoundedPanel card = cardPanel();
        card.setLayout(new BorderLayout());
        card.setBorder(new EmptyBorder(14, 14, 14, 14));

        card.add(sectionTitle("Designer Notes"), BorderLayout.NORTH);

        JPanel body = new JPanel();
        body.setOpaque(false);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBorder(new EmptyBorder(10, 0, 0, 0));

        notesArea = new JTextArea("No notes added yet.");
        notesArea.setWrapStyleWord(true);
        notesArea.setLineWrap(true);
        notesArea.setEditable(false);
        notesArea.setOpaque(false);
        notesArea.setForeground(MUTED);
        notesArea.setFont(notesArea.getFont().deriveFont(Font.PLAIN, 12.2f));
        notesArea.setBorder(null);

        body.add(notesArea);
        body.add(Box.createVerticalStrut(10));

        JPanel warn = new JPanel(new BorderLayout());
        warn.setOpaque(true);
        warn.setBackground(WARNING_BG);
        warn.setBorder(new CompoundBorder(
                new LineBorder(WARNING_BORDER, 1, true),
                new EmptyBorder(10, 12, 10, 12)
        ));

        JLabel warnText = new JLabel("⚠  Tip: Add lighting notes to improve the final render mood.");
        warnText.setForeground(WARNING_TEXT);
        warnText.setFont(warnText.getFont().deriveFont(Font.PLAIN, 11.8f));
        warn.add(warnText, BorderLayout.CENTER);

        body.add(warn);

        card.add(body, BorderLayout.CENTER);
        return card;
    }

    private JComponent buildInfoCard() {
        RoundedPanel card = cardPanel();
        card.setLayout(new BorderLayout());
        card.setBorder(new EmptyBorder(14, 14, 14, 14));

        card.add(sectionTitle("Design Information"), BorderLayout.NORTH);

        JPanel body = new JPanel();
        body.setOpaque(false);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBorder(new EmptyBorder(10, 0, 0, 0));

        JPanel cust = new JPanel(new BorderLayout());
        cust.setOpaque(false);
        cust.add(keyLabel("Customer"), BorderLayout.NORTH);

        JPanel custRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 6));
        custRow.setOpaque(false);

        JComponent avatar = new AvatarCircle("D"); // will be updated by bind if needed
        customerName = new JLabel("—");
        customerName.setForeground(TEXT);
        customerName.setFont(customerName.getFont().deriveFont(Font.BOLD, 12.2f));

        customerEmail = new JLabel("—");
        customerEmail.setForeground(MUTED);
        customerEmail.setFont(customerEmail.getFont().deriveFont(Font.PLAIN, 11.3f));

        JPanel nm = new JPanel();
        nm.setOpaque(false);
        nm.setLayout(new BoxLayout(nm, BoxLayout.Y_AXIS));
        nm.add(customerName);
        nm.add(customerEmail);

        custRow.add(avatar);
        custRow.add(nm);
        cust.add(custRow, BorderLayout.CENTER);

        body.add(cust);
        body.add(divider());

        roomTypeVal = new JLabel("—");
        roomSizeVal = new JLabel("—");
        roomShapeVal = new JLabel("—");

        body.add(kv("Room Type", roomTypeVal));
        body.add(divider());
        body.add(kv("Room Size", roomSizeVal));
        body.add(divider());
        body.add(kv("Room Shape", roomShapeVal));
        body.add(divider());

        JPanel theme = new JPanel(new BorderLayout());
        theme.setOpaque(false);
        theme.add(keyLabel("Colour Theme"), BorderLayout.NORTH);

        JPanel sw = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        sw.setOpaque(false);
        sw.add(colorDot(new Color(0xE5E7EB)));
        sw.add(colorDot(new Color(0x111827)));
        sw.add(colorDot(new Color(0xFDE68A)));

        themeText = new JLabel("—");
        themeText.setForeground(MUTED);
        themeText.setFont(themeText.getFont().deriveFont(Font.PLAIN, 11.5f));
        sw.add(themeText);

        theme.add(sw, BorderLayout.CENTER);

        body.add(theme);
        body.add(divider());

        // created / last modified from model timestamps
        JLabel createdVal = new JLabel("—");
        JLabel modifiedVal = new JLabel("—");

        body.add(kv("Created", createdVal));
        body.add(divider());
        body.add(kv("Last Modified", modifiedVal));

        // update created/modified labels dynamically when refresh binds
        // (we can compute from current design id)
        // We'll compute right now if possible:
        if (appState != null && appState.getCurrentDesignId() != null) {
            Design d = appState.getRepo().getById(appState.getCurrentDesignId());
            if (d != null) {
                createdVal.setText(dateLabel(d.getCreatedAtEpochMs()));
                modifiedVal.setText(dateLabel(d.getLastUpdatedEpochMs()));
            }
        }

        card.add(body, BorderLayout.CENTER);
        return card;
    }

    private JComponent kv(String k, JLabel valueLabel) {
        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(false);

        JLabel key = keyLabel(k);
        valueLabel.setForeground(TEXT);
        valueLabel.setFont(valueLabel.getFont().deriveFont(Font.PLAIN, 12.0f));

        row.add(key, BorderLayout.NORTH);
        row.add(valueLabel, BorderLayout.CENTER);
        row.setBorder(new EmptyBorder(2, 0, 2, 0));
        return row;
    }

    private JComponent buildTagsCard() {
        RoundedPanel card = cardPanel();
        card.setLayout(new BorderLayout());
        card.setBorder(new EmptyBorder(14, 14, 14, 14));

        card.add(sectionTitle("Tags"), BorderLayout.NORTH);

        JPanel body = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 10));
        body.setOpaque(false);

        // Step 1: tags not persisted; keep demo chips (later wire)
        body.add(UiKit.chipPrimary("Modern"));
        body.add(UiKit.chip("Minimalist"));
        body.add(UiKit.chip("Neutral"));

        JLabel add = new JLabel("+ Add Tag");
        add.setForeground(PRIMARY);
        add.setFont(add.getFont().deriveFont(Font.BOLD, 11.5f));
        add.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        add.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                JOptionPane.showMessageDialog(DesignDetailsPage.this, "Tags will be added in a later step.");
            }
        });
        body.add(add);

        card.add(body, BorderLayout.CENTER);
        return card;
    }

    /* ===================== Empty state ===================== */

    private JComponent emptyState(String title, String subtitle) {
        RoundedPanel card = cardPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(new EmptyBorder(22, 22, 22, 22));

        JLabel t = new JLabel(title);
        t.setForeground(TEXT);
        t.setFont(t.getFont().deriveFont(Font.BOLD, 14.5f));

        JLabel s = new JLabel(subtitle);
        s.setForeground(MUTED);
        s.setFont(s.getFont().deriveFont(Font.PLAIN, 12.2f));
        s.setBorder(new EmptyBorder(8, 0, 0, 0));

        card.add(t);
        card.add(s);
        return card;
    }

    /* ===================== Edit Dialog ===================== */

    private RoundedPanel cardPanel() {
        RoundedPanel p = new RoundedPanel(16, WHITE);
        p.setBorderPaint(BORDER);
        p.setOpaque(false);
        return p;
    }

    private JLabel sectionTitle(String text) {
        JLabel l = new JLabel(text);
        l.setForeground(TEXT);
        l.setFont(l.getFont().deriveFont(Font.BOLD, 13.3f));
        return l;
    }

    private JLabel keyLabel(String text) {
        JLabel l = new JLabel(text);
        l.setForeground(MUTED);
        l.setFont(l.getFont().deriveFont(Font.PLAIN, 11.4f));
        return l;
    }

    private JComponent divider() {
        JPanel d = new JPanel();
        d.setOpaque(true);
        d.setBackground(BORDER);
        d.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        d.setPreferredSize(new Dimension(0, 1));
        d.setBorder(new EmptyBorder(8, 0, 8, 0));
        return d;
    }




    private JToggleButton toggleChip(String text, boolean selected) {
        JToggleButton b = new JToggleButton(text, selected);
        b.setFocusPainted(false);
        b.setBorderPainted(false);
        b.setContentAreaFilled(true);
        b.setOpaque(true);
        b.setFont(b.getFont().deriveFont(Font.BOLD, 11.0f));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        styleToggle(b);
        b.addChangeListener(e -> styleToggle(b));
        return b;
    }

    private void styleToggle(AbstractButton b) {
        boolean sel = b.isSelected();
        b.setBackground(sel ? PRIMARY : new Color(0xF3F4F6));
        b.setForeground(sel ? Color.WHITE : TEXT);
        b.setBorder(new EmptyBorder(6, 10, 6, 10));
    }

    private JButton ghostButton(String text) {
        JButton b = new JButton(text);
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setBackground(WHITE);
        b.setForeground(TEXT);
        b.setBorder(new CompoundBorder(
                new LineBorder(BORDER, 1, true),
                new EmptyBorder(8, 12, 8, 12)
        ));
        return b;
    }

    private JButton dangerOutlineButton(String text) {
        JButton b = new JButton(text);
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setBackground(WHITE);
        b.setForeground(DANGER);
        b.setBorder(new CompoundBorder(
                new LineBorder(new Color(0xFCA5A5), 1, true),
                new EmptyBorder(8, 12, 8, 12)
        ));
        return b;
    }

    private JButton primaryPill(String text) {
        JButton b = new JButton(text);
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setBackground(PRIMARY);
        b.setForeground(Color.WHITE);
        b.setBorder(new EmptyBorder(10, 14, 10, 14));
        return b;
    }

    private JButton primaryGradientPill(String text) {
        JButton b = new JButton(text);
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setBackground(PRIMARY_DARK);
        b.setForeground(Color.WHITE);
        b.setBorder(new EmptyBorder(10, 14, 10, 14));
        return b;
    }

    private JButton iconPill(String text) {
        JButton b = UiKit.iconButton(text);
        b.setBackground(new Color(0xF9FAFB));
        b.setBorder(new CompoundBorder(
                new LineBorder(BORDER, 1, true),
                new EmptyBorder(6, 8, 6, 8)
        ));
        return b;
    }

    private JComponent colorDot(Color c) {
        JPanel p = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(c);
                g2.fillOval(0, 0, getWidth() - 1, getHeight() - 1);
                g2.setColor(BORDER);
                g2.drawOval(0, 0, getWidth() - 1, getHeight() - 1);
                g2.dispose();
            }
        };
        p.setOpaque(false);
        p.setPreferredSize(new Dimension(18, 18));
        return p;
    }



    /* ===================== Utilities ===================== */

    private static String safe(String s, String fallback) {
        if (s == null) return fallback;
        s = s.trim();
        return s.isEmpty() ? fallback : s;
    }

    private static String generateEmailHint(String name) {
        if (name == null) return "—";
        String n = name.trim().toLowerCase(Locale.ENGLISH).replaceAll("[^a-z0-9]+", ".");
        if (n.isEmpty()) return "—";
        return n + "@example.com";
    }

    private static String formatNumber(double v) {
        if (Math.abs(v - Math.round(v)) < 1e-9) return String.valueOf((long) Math.round(v));
        return String.format(Locale.ENGLISH, "%.2f", v);
    }

    private static String dateLabel(long epochMs) {
        if (epochMs <= 0) return "—";
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("d MMM yyyy", Locale.ENGLISH);
            return sdf.format(new Date(epochMs));
        } catch (Exception e) {
            return "—";
        }
    }

    private static String timeAgoLabel(long epochMs) {
        if (epochMs <= 0) return "—";
        long diff = System.currentTimeMillis() - epochMs;
        if (diff < 60_000) return "just now";
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
            return "earlier";
        }
    }

    /* ===================== Small custom components ===================== */

    private static class AvatarCircle extends JPanel {
        private final String letter;

        AvatarCircle(String letter) {
            this.letter = letter;
            setPreferredSize(new Dimension(34, 34));
            setOpaque(false);
        }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth();
            int h = getHeight();
            g2.setColor(new Color(0xE5E7EB));
            g2.fillOval(0, 0, w - 1, h - 1);
            g2.setColor(new Color(0x9CA3AF));
            g2.drawOval(0, 0, w - 1, h - 1);

            g2.setColor(new Color(0x111827));
            Font f = getFont().deriveFont(Font.BOLD, 12.5f);
            g2.setFont(f);
            FontMetrics fm = g2.getFontMetrics();
            int tx = (w - fm.stringWidth(letter)) / 2;
            int ty = (h + fm.getAscent() - fm.getDescent()) / 2;
            g2.drawString(letter, tx, ty);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    /** Rounded panel with optional border (used across cards). */
    private static class RoundedPanel extends JPanel {
        private final int radius;
        private Color fill;
        private Color border;

        RoundedPanel(int radius, Color fill) {
            this.radius = radius;
            this.fill = fill;
            setOpaque(false);
        }

        public void setBorderPaint(Color c) { this.border = c; }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth();
            int h = getHeight();
            g2.setColor(fill != null ? fill : getBackground());
            g2.fillRoundRect(0, 0, w, h, radius, radius);
            if (border != null) {
                g2.setColor(border);
                g2.drawRoundRect(0, 0, w - 1, h - 1, radius, radius);
            }
            g2.dispose();
            super.paintComponent(g);
        }
    }
}
