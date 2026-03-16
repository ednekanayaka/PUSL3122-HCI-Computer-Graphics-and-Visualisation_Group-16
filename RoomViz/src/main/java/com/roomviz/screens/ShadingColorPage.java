package com.roomviz.screens;

import com.roomviz.app.AppFrame;
import com.roomviz.app.Router;
import com.roomviz.app.ScreenKeys;
import com.roomviz.data.AppState;
import com.roomviz.model.Design;
import com.roomviz.model.FurnitureItem;
import com.roomviz.ui.FontAwesome;
import com.roomviz.ui.UiKit;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

/**
 * Shading & Color Tools page (Swing UI).
 *
 * Undo/Redo support:
 * - When Apply Changes is pressed, we push a BEFORE snapshot and record an AFTER snapshot
 *   into AppState history, so Planner2D undo/redo works for color/shading/material/lighting changes.
 *
 * - If no design is selected -> show empty state.
 * - Rebuild UI on navigation here.
 */
public class ShadingColorPage extends JPanel {

    private final Router router;
    private final AppState appState;

    // ===== state =====
    private boolean afterMode = true;
    private boolean globalTab = true;

    private Color selectedColor = new Color(0x3B82F6);
    private int shading = 50; // 0..100
    private String material = "Matte";
    private String lighting = "Daylight";

    // ===== UI refs =====
    private final JTextField hexField = new JTextField("#3B82F6");
    private final JPanel swatch = new JPanel();
    private final JLabel shadingLabel = new JLabel("Current: 50%");

    // Tabs
    private final JButton tabGlobal = new JButton("Global Design");
    private final JButton tabSelected = new JButton("Selected Items");

    // Material buttons
    private final JButton matteBtn = new JButton("Matte");
    private final JButton satinBtn = new JButton("Satin");
    private final JButton glossBtn = new JButton("Gloss");

    // Lighting buttons
    private final JButton daylightBtn = new JButton(FontAwesome.SUN + "  Daylight");
    private final JButton warmBtn = new JButton(FontAwesome.FIRE + "  Warm");
    private final JButton coolBtn = new JButton(FontAwesome.SNOWFLAKE + "  Cool");
    private final JButton neutralBtn = new JButton(FontAwesome.SQUARE_REGULAR + "  Neutral");

    // Before/After
    private final JToggleButton beforeBtn = new JToggleButton("Before");
    private final JToggleButton afterBtn = new JToggleButton("After");

    private final PreviewPanel previewPanel = new PreviewPanel();

    // Guards to avoid duplicate listeners when we rebuild UI
    private boolean actionsWired = false;
    private boolean hexDocWired = false;

    public ShadingColorPage(AppFrame frame) {
        this(frame, null, null);
    }

    public ShadingColorPage(AppFrame frame, Router router, AppState appState) {
        this.router = router;
        this.appState = appState;

        setLayout(new BorderLayout());
        setBackground(UiKit.BG);
        setBorder(new EmptyBorder(14, 14, 14, 14));

        rebuildUI();

        try {
            if (router != null) {
                router.addListener(key -> {
                    if (ScreenKeys.SHADING_COLOR.equals(key)) {
                        rebuildUI();
                    }
                });
            }
        } catch (Throwable ignored) { }
    }

    private void rebuildUI() {
        removeAll();

        Design d = (appState == null) ? null : appState.getCurrentDesign();
        if (d == null) {
            add(buildNoDesignState(), BorderLayout.CENTER);
            revalidate();
            repaint();
            return;
        }

        add(buildCardShell(), BorderLayout.CENTER);

        wireActionsOnce();
        wireLiveHexValidationOnce();

        loadInitialFromDesign();
        syncUI();

        revalidate();
        repaint();
    }

    /* ========================= No Design Selected (Empty State) ========================= */

    private JComponent buildNoDesignState() {
        JPanel wrap = new JPanel(new GridBagLayout());
        wrap.setOpaque(false);

        UiKit.RoundedPanel card = new UiKit.RoundedPanel(18, UiKit.WHITE);
        card.setBorderPaint(UiKit.BORDER);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(new EmptyBorder(28, 28, 28, 28));

        JLabel title = new JLabel("Select or create a design first");
        title.setForeground(UiKit.TEXT);
        title.setFont(UiKit.scaled(title, Font.BOLD, 1.10f));
        title.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel sub = new JLabel("<html>Shading & Colour Tools apply to the <b>currently selected design</b>.<br/>Go to the Design Library to pick one, or create a new design.</html>");
        sub.setForeground(UiKit.MUTED);
        sub.setFont(UiKit.scaled(sub, Font.PLAIN, 0.95f));
        sub.setBorder(new EmptyBorder(8, 0, 0, 0));
        sub.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 6));
        btnRow.setOpaque(false);
        btnRow.setBorder(new EmptyBorder(12, 0, 0, 0));
        btnRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        JButton goLibrary = UiKit.primaryButton("Go to Design Library");
        goLibrary.addActionListener(e -> {
            if (router != null) router.show(ScreenKeys.DESIGN_LIBRARY);
        });

        JButton createNew = UiKit.ghostButton("Create New Design");
        createNew.addActionListener(e -> {
            if (router != null) router.show(ScreenKeys.NEW_DESIGN);
        });

        JButton importBtn = UiKit.ghostButton("Import Design JSON");
        importBtn.addActionListener(e -> onImportDesigns());

        JButton backPlanner = UiKit.ghostButton("Back to Planner");
        backPlanner.addActionListener(e -> {
            if (router != null) router.show(ScreenKeys.PLANNER_2D);
        });

        btnRow.add(goLibrary);
        btnRow.add(createNew);
        btnRow.add(importBtn);
        btnRow.add(backPlanner);

        card.add(title);
        card.add(sub);
        card.add(btnRow);

        Dimension pref = card.getPreferredSize();
        card.setMinimumSize(new Dimension(0, pref.height));
        card.setPreferredSize(new Dimension(Math.min(760, pref.width), pref.height));
        card.setMaximumSize(new Dimension(760, Integer.MAX_VALUE));

        JPanel row = new JPanel();
        row.setOpaque(false);
        row.setLayout(new BoxLayout(row, BoxLayout.X_AXIS));
        row.add(Box.createHorizontalGlue());
        row.add(card);
        row.add(Box.createHorizontalGlue());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(24, 18, 24, 18);
        wrap.add(row, gbc);
        return wrap;
    }

    private void onImportDesigns() {
        if (appState == null || appState.getRepo() == null) {
            JOptionPane.showMessageDialog(this,
                    "Design repository not available.",
                    "Cannot import", JOptionPane.WARNING_MESSAGE);
            return;
        }

        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Import RoomViz Designs");
        chooser.setFileFilter(new FileNameExtensionFilter("JSON files (*.json)", "json"));

        int result = chooser.showOpenDialog(this);
        if (result != JFileChooser.APPROVE_OPTION) return;

        java.io.File in = chooser.getSelectedFile();
        int importedCount = appState.getRepo().importFrom(in);

        if (importedCount <= 0) {
            JOptionPane.showMessageDialog(this,
                    "Could not import designs.\nPlease select a valid RoomViz export JSON file.",
                    "Import Failed", JOptionPane.WARNING_MESSAGE);
            return;
        }

        java.util.List<Design> all = appState.getRepo().getAllSortedByLastUpdatedDesc();
        if (!all.isEmpty()) {
            appState.setCurrentDesignId(all.get(0).getId());
        }

        JOptionPane.showMessageDialog(this,
                "Imported " + importedCount + " design(s) from:\n" + in.getAbsolutePath(),
                "Import Complete", JOptionPane.INFORMATION_MESSAGE);

        rebuildUI();
    }

    /* ========================= Wiring (ONLY ONCE) ========================= */

    private void wireActionsOnce() {
        if (actionsWired) return;
        actionsWired = true;

        tabGlobal.addActionListener(e -> { globalTab = true; syncUI(); });
        tabSelected.addActionListener(e -> { globalTab = false; syncUI(); });

        matteBtn.addActionListener(e -> { material = "Matte"; syncUI(); });
        satinBtn.addActionListener(e -> { material = "Satin"; syncUI(); });
        glossBtn.addActionListener(e -> { material = "Gloss"; syncUI(); });

        daylightBtn.addActionListener(e -> { lighting = "Daylight"; syncUI(); });
        warmBtn.addActionListener(e -> { lighting = "Warm"; syncUI(); });
        coolBtn.addActionListener(e -> { lighting = "Cool"; syncUI(); });
        neutralBtn.addActionListener(e -> { lighting = "Neutral"; syncUI(); });

        beforeBtn.addActionListener(e -> { afterMode = false; syncUI(); });
        afterBtn.addActionListener(e -> { afterMode = true; syncUI(); });

        hexField.addActionListener(e -> {
            Color c = parseHex(hexField.getText());
            if (c != null) {
                selectedColor = c;
                syncUI();
            } else {
                hexField.setText(toHex(selectedColor));
            }
        });
    }

    private void wireLiveHexValidationOnce() {
        if (hexDocWired) return;
        hexDocWired = true;

        hexField.getDocument().addDocumentListener(new DocumentListener() {
            void onChange() {
                Color c = parseHex(hexField.getText());
                if (c != null) {
                    selectedColor = c;
                    swatch.setBackground(selectedColor);
                    previewPanel.repaint();
                }
            }
            @Override public void insertUpdate(DocumentEvent e) { onChange(); }
            @Override public void removeUpdate(DocumentEvent e) { onChange(); }
            @Override public void changedUpdate(DocumentEvent e) { onChange(); }
        });
    }

    /* ========================= Load initial state from design ========================= */

    private void loadInitialFromDesign() {
        if (appState == null) return;

        Design d = appState.getCurrentDesign();
        if (d == null) return;

        List<FurnitureItem> items = d.getItems();
        if (items == null || items.isEmpty()) return;

        FurnitureItem target = findSelectedItemOrFirst(items);

        Color c = parseHex(target.getColorHex());
        if (c != null) selectedColor = c;

        shading = clamp(target.getShadingPercent(), 0, 100);

        if (target.getMaterial() != null && !target.getMaterial().isBlank()) material = target.getMaterial();
        if (target.getLighting() != null && !target.getLighting().isBlank()) lighting = target.getLighting();

        hexField.setText(toHex(selectedColor));
    }

    private FurnitureItem findSelectedItemOrFirst(List<FurnitureItem> items) {
        String selId = (appState == null) ? null : appState.getSelectedItemId();
        if (selId != null) {
            for (FurnitureItem it : items) {
                if (selId.equals(it.getId())) return it;
            }
        }
        return items.get(0);
    }

    /* ========================= Shell Card ========================= */

    private JComponent buildCardShell() {
        UiKit.RoundedPanel shell = new UiKit.RoundedPanel(16, UiKit.WHITE);
        shell.setBorderPaint(UiKit.BORDER);
        shell.setLayout(new BorderLayout());

        JPanel tools = buildToolsPanel();
        tools.setPreferredSize(new Dimension(340, 0));
        tools.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, UiKit.BORDER));

        JPanel preview = buildPreviewSide();

        shell.add(tools, BorderLayout.WEST);
        shell.add(preview, BorderLayout.CENTER);

        return shell;
    }

    /* ========================= Left Tools ========================= */

    private JPanel buildToolsPanel() {
        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setOpaque(true);
        wrap.setBackground(UiKit.WHITE);

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.setBorder(new EmptyBorder(16, 16, 12, 16));

        JLabel title = new JLabel("Shading & Colour Tools");
        title.setForeground(UiKit.TEXT);
        title.setFont(UiKit.scaled(title, Font.BOLD, 1.10f));

        JButton close = UiKit.iconButton(FontAwesome.XMARK);
        close.setToolTipText("Close");
        close.addActionListener(e -> {
            if (router != null) router.show(ScreenKeys.PLANNER_2D);
            else JOptionPane.showMessageDialog(this, "Close tools (demo).", "Close", JOptionPane.INFORMATION_MESSAGE);
        });

        header.add(title, BorderLayout.WEST);
        header.add(close, BorderLayout.EAST);

        JPanel tabs = new JPanel(new GridLayout(1, 2, 10, 0));
        tabs.setOpaque(false);
        tabs.setBorder(new EmptyBorder(0, 16, 10, 16));

        styleTabButton(tabGlobal);
        styleTabButton(tabSelected);

        tabs.add(tabGlobal);
        tabs.add(tabSelected);

        JLabel hint = new JLabel("Apply changes to entire room or selected furniture");
        hint.setForeground(UiKit.MUTED);
        hint.setFont(UiKit.scaled(hint, Font.PLAIN, 0.88f));
        hint.setBorder(new EmptyBorder(0, 16, 12, 16));

        JPanel content = new JPanel();
        content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBorder(new EmptyBorder(0, 16, 16, 16));

        content.add(sectionTitle("Colour"));
        content.add(Box.createVerticalStrut(8));
        content.add(colorGradientPicker());
        content.add(Box.createVerticalStrut(10));
        content.add(hexRow());
        content.add(Box.createVerticalStrut(12));
        content.add(quickPalettes());
        content.add(Box.createVerticalStrut(12));
        content.add(materialStyles());
        content.add(Box.createVerticalStrut(18));

        content.add(sectionTitle("Shading"));
        content.add(Box.createVerticalStrut(10));
        content.add(shadingSlider());
        content.add(Box.createVerticalStrut(14));
        content.add(lightingPresets());
        content.add(Box.createVerticalStrut(18));
        content.add(bottomButtons());

        JScrollPane sc = new JScrollPane(content);
        sc.setBorder(BorderFactory.createEmptyBorder());
        sc.setOpaque(false);
        sc.getViewport().setOpaque(false);
        sc.getVerticalScrollBar().setUnitIncrement(18);

        JPanel topStack = new JPanel();
        topStack.setOpaque(false);
        topStack.setLayout(new BoxLayout(topStack, BoxLayout.Y_AXIS));
        topStack.add(header);
        topStack.add(tabs);
        topStack.add(hint);

        wrap.add(topStack, BorderLayout.NORTH);
        wrap.add(sc, BorderLayout.CENTER);

        return wrap;
    }

    private void styleTabButton(JButton b) {
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setFont(UiKit.scaled(b, Font.BOLD, 0.92f));
        b.setBorder(new EmptyBorder(10, 12, 10, 12));
        b.setOpaque(true);
    }

    /* ========================= Right Preview ========================= */

    private JPanel buildPreviewSide() {
        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setOpaque(true);
        wrap.setBackground(UiKit.WHITE);

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.setBorder(new EmptyBorder(14, 16, 10, 16));

        JLabel title = new JLabel("Preview");
        title.setForeground(UiKit.TEXT);
        title.setFont(UiKit.scaled(title, Font.BOLD, 1.03f));

        JPanel toggle = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        toggle.setOpaque(false);

        ButtonGroup g = new ButtonGroup();
        g.add(beforeBtn);
        g.add(afterBtn);

        toggle.add(beforeBtn);
        toggle.add(afterBtn);

        top.add(title, BorderLayout.WEST);
        top.add(toggle, BorderLayout.EAST);

        previewPanel.setBorder(new EmptyBorder(8, 16, 16, 16));

        wrap.add(top, BorderLayout.NORTH);
        wrap.add(previewPanel, BorderLayout.CENTER);
        return wrap;
    }

    /* ========================= Components ========================= */

    private JComponent sectionTitle(String text) {
        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(false);

        JLabel t = new JLabel(text);
        t.setForeground(UiKit.TEXT);
        t.setFont(UiKit.scaled(t, Font.BOLD, 0.98f));

        JLabel info = new JLabel(FontAwesome.CIRCLE_INFO);
        info.setForeground(UiKit.MUTED);
        info.setFont(FontAwesome.solid(12f));
        info.setBorder(new EmptyBorder(0, 6, 0, 0));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        left.setOpaque(false);
        left.add(t);
        left.add(info);

        row.add(left, BorderLayout.WEST);
        return row;
    }

    private JComponent colorGradientPicker() {
        GradientPicker picker = new GradientPicker();
        picker.setPreferredSize(new Dimension(0, 92));
        picker.setBorder(new LineBorder(UiKit.BORDER, 1, true));
        picker.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        MouseAdapter ma = new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) { sampleFromPicker(picker, e); }
            @Override public void mouseDragged(MouseEvent e) { sampleFromPicker(picker, e); }
        };
        picker.addMouseListener(ma);
        picker.addMouseMotionListener(ma);

        return picker;
    }

    private void sampleFromPicker(GradientPicker picker, MouseEvent e) {
        selectedColor = picker.colorAt(e.getX(), e.getY());
        if (!hexField.hasFocus()) hexField.setText(toHex(selectedColor));
        syncUI();
    }

    private JComponent hexRow() {
        JPanel row = new JPanel(new BorderLayout(10, 0));
        row.setOpaque(false);

        swatch.setPreferredSize(new Dimension(34, 34));
        swatch.setBackground(selectedColor);
        swatch.setBorder(new LineBorder(UiKit.BORDER, 1, true));

        styleTextField(hexField);

        row.add(swatch, BorderLayout.WEST);
        row.add(hexField, BorderLayout.CENTER);
        return row;
    }

    private JComponent quickPalettes() {
        JPanel wrap = new JPanel();
        wrap.setOpaque(false);
        wrap.setLayout(new BoxLayout(wrap, BoxLayout.Y_AXIS));

        JLabel t = new JLabel("Quick Palettes");
        t.setForeground(UiKit.MUTED);
        t.setFont(UiKit.scaled(t, Font.PLAIN, 0.88f));

        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
        row.setOpaque(false);

        row.add(paletteButton(new Color(0x2563EB), new Color(0x60A5FA), new Color(0x93C5FD), new Color(0xDBEAFE)));
        row.add(paletteButton(new Color(0x16A34A), new Color(0x4ADE80), new Color(0x86EFAC), new Color(0xDCFCE7)));
        row.add(paletteButton(new Color(0x7C3AED), new Color(0xA78BFA), new Color(0xC4B5FD), new Color(0xEDE9FE)));
        row.add(paletteButton(new Color(0x111827), new Color(0x374151), new Color(0x6B7280), new Color(0xE5E7EB)));

        wrap.add(t);
        wrap.add(row);
        return wrap;
    }

    private JComponent paletteButton(Color a, Color b, Color c, Color d) {
        JPanel btn = new JPanel(new GridLayout(2, 2, 3, 3));
        btn.setOpaque(true);
        btn.setBackground(UiKit.WHITE);
        btn.setBorder(new LineBorder(UiKit.BORDER, 1, true));
        btn.setPreferredSize(new Dimension(54, 38));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        btn.add(colorDot(a));
        btn.add(colorDot(b));
        btn.add(colorDot(c));
        btn.add(colorDot(d));

        btn.addMouseListener(new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) {
                selectedColor = a;
                hexField.setText(toHex(selectedColor));
                syncUI();
            }
        });

        return btn;
    }

    private JComponent colorDot(Color c) {
        JPanel p = new JPanel();
        p.setOpaque(true);
        p.setBackground(c);
        p.setBorder(BorderFactory.createEmptyBorder());
        return p;
    }

    private JComponent materialStyles() {
        JPanel wrap = new JPanel();
        wrap.setOpaque(false);
        wrap.setLayout(new BoxLayout(wrap, BoxLayout.Y_AXIS));

        JLabel t = new JLabel("Material Styles");
        t.setForeground(UiKit.MUTED);
        t.setFont(UiKit.scaled(t, Font.PLAIN, 0.88f));

        JPanel row = new JPanel(new GridLayout(1, 3, 10, 0));
        row.setOpaque(false);
        row.setBorder(new EmptyBorder(8, 0, 0, 0));

        styleSmallToggle(matteBtn);
        styleSmallToggle(satinBtn);
        styleSmallToggle(glossBtn);

        row.add(matteBtn);
        row.add(satinBtn);
        row.add(glossBtn);

        wrap.add(t);
        wrap.add(row);
        return wrap;
    }

    private void styleSmallToggle(JButton btn) {
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setFont(UiKit.scaled(btn, Font.BOLD, 0.90f));
        btn.setBorder(new EmptyBorder(10, 10, 10, 10));
        btn.setOpaque(true);
    }

    private JComponent shadingSlider() {
        JPanel wrap = new JPanel();
        wrap.setOpaque(false);
        wrap.setLayout(new BoxLayout(wrap, BoxLayout.Y_AXIS));

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);

        JLabel left = new JLabel("Darker");
        left.setForeground(UiKit.MUTED);
        left.setFont(UiKit.scaled(left, Font.PLAIN, 0.88f));

        JLabel right = new JLabel("Lighter");
        right.setForeground(UiKit.MUTED);
        right.setFont(UiKit.scaled(right, Font.PLAIN, 0.88f));

        shadingLabel.setForeground(UiKit.MUTED);
        shadingLabel.setFont(UiKit.scaled(shadingLabel, Font.PLAIN, 0.88f));
        shadingLabel.setHorizontalAlignment(SwingConstants.CENTER);

        top.add(left, BorderLayout.WEST);
        top.add(shadingLabel, BorderLayout.CENTER);
        top.add(right, BorderLayout.EAST);

        JSlider slider = new JSlider(0, 100, shading);
        slider.setOpaque(false);
        slider.addChangeListener(e -> {
            shading = slider.getValue();
            shadingLabel.setText("Current: " + shading + "%");
            previewPanel.repaint();
        });

        wrap.add(top);
        wrap.add(Box.createVerticalStrut(8));
        wrap.add(slider);
        return wrap;
    }

    private JComponent lightingPresets() {
        JPanel wrap = new JPanel();
        wrap.setOpaque(false);
        wrap.setLayout(new BoxLayout(wrap, BoxLayout.Y_AXIS));

        JLabel t = new JLabel("Lighting Presets");
        t.setForeground(UiKit.MUTED);
        t.setFont(UiKit.scaled(t, Font.PLAIN, 0.88f));

        JPanel grid = new JPanel(new GridLayout(2, 2, 10, 10));
        grid.setOpaque(false);
        grid.setBorder(new EmptyBorder(8, 0, 0, 0));

        stylePreset(daylightBtn);
        stylePreset(warmBtn);
        stylePreset(coolBtn);
        stylePreset(neutralBtn);

        grid.add(daylightBtn);
        grid.add(warmBtn);
        grid.add(coolBtn);
        grid.add(neutralBtn);

        wrap.add(t);
        wrap.add(grid);
        return wrap;
    }

    private void stylePreset(JButton b) {
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setBorder(new EmptyBorder(10, 12, 10, 12));
        b.setFont(UiKit.scaled(b, Font.BOLD, 0.90f));
        b.setOpaque(true);
    }

    private JComponent bottomButtons() {
        JPanel wrap = new JPanel();
        wrap.setOpaque(false);
        wrap.setLayout(new BoxLayout(wrap, BoxLayout.Y_AXIS));
        wrap.setBorder(new EmptyBorder(10, 0, 0, 0));

        JPanel row = new JPanel(new GridLayout(1, 2, 10, 0));
        row.setOpaque(false);

        JButton apply = UiKit.primaryButton("Apply Changes");
        JButton revert = UiKit.ghostButton("Revert");

        apply.setFont(UiKit.scaled(apply, Font.BOLD, 0.98f));
        revert.setFont(UiKit.scaled(revert, Font.PLAIN, 0.98f));

        apply.setBorder(new EmptyBorder(10, 12, 10, 12));
        revert.setBorder(new EmptyBorder(10, 12, 10, 12));

        apply.addActionListener(e -> {
            ApplyResult res = applyToDesign();
            JOptionPane.showMessageDialog(
                    this,
                    res.message,
                    "Apply Changes",
                    res.ok ? JOptionPane.INFORMATION_MESSAGE : JOptionPane.WARNING_MESSAGE
            );
        });

        revert.addActionListener(e -> resetDefaults());

        row.add(apply);
        row.add(revert);

        JLabel reset = new JLabel("Reset to Default");
        reset.setForeground(UiKit.MUTED);
        reset.setFont(UiKit.scaled(reset, Font.PLAIN, 0.88f));
        reset.setHorizontalAlignment(SwingConstants.CENTER);
        reset.setBorder(new EmptyBorder(10, 0, 0, 0));
        reset.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        reset.addMouseListener(new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) { resetDefaults(); }
        });

        wrap.add(row);
        wrap.add(reset);
        return wrap;
    }

    private void resetDefaults() {
        selectedColor = new Color(0x3B82F6);
        shading = 50;
        material = "Matte";
        lighting = "Daylight";
        afterMode = true;
        globalTab = true;
        hexField.setText("#3B82F6");
        syncUI();
    }

    /* ========================= REAL AppState + Design integration (UNDO/REDO aware) ========================= */

    private ApplyResult applyToDesign() {
        if (appState == null) {
            return new ApplyResult(false, "AppState is not wired into this screen yet.\n(Use: new ShadingColorPage(frame, router, appState))");
        }

        Design design = appState.getCurrentDesign();
        if (design == null) {
            return new ApplyResult(false, "No design is selected.\nGo to Design Library and select (or create) a design first.");
        }

        List<FurnitureItem> items = design.getItems();
        if (items == null || items.isEmpty()) {
            return new ApplyResult(false, "No furniture items exist in this design yet.\nGo to Planner 2D and add an item first.");
        }

        // ✅ BEFORE snapshot for undo
        appState.pushBeforeChange(design.getId(), items);

        String hex = toHex(selectedColor);
        int updated = 0;

        if (globalTab) {
            for (FurnitureItem it : items) {
                applyStyleToItem(it, hex);
                updated++;
            }
        } else {
            String selectedId = appState.getSelectedItemId();
            if (selectedId == null || selectedId.isBlank()) {
                return new ApplyResult(false, "No item is selected.\nGo to Planner 2D, click an item, then come back here.");
            }
            for (FurnitureItem it : items) {
                if (selectedId.equals(it.getId())) {
                    applyStyleToItem(it, hex);
                    updated++;
                    break;
                }
            }
            if (updated == 0) {
                return new ApplyResult(false, "Selected item not found in the current design.\nTry selecting again in Planner 2D.");
            }
        }

        design.setLastUpdatedEpochMs(System.currentTimeMillis());
        if (appState.getRepo() != null) appState.getRepo().upsert(design);

        // ✅ AFTER snapshot for redo / undo correctness
        appState.recordAfterChange(design.getId(), items);

        String target = globalTab ? "Global Design (all items)" : "Selected Item";
        String msg =
                "Applied successfully!\n\n" +
                        "Target: " + target + "\n" +
                        "Updated items: " + updated + "\n" +
                        "Color: " + hex + "\n" +
                        "Material: " + material + "\n" +
                        "Shading: " + shading + "%\n" +
                        "Lighting: " + lighting;

        return new ApplyResult(true, msg);
    }

    private void applyStyleToItem(FurnitureItem it, String hex) {
        it.setColorHex(hex);
        it.setShadingPercent(clamp(shading, 0, 100));
        it.setMaterial(material);
        it.setLighting(lighting);
    }

    private static class ApplyResult {
        final boolean ok;
        final String message;
        ApplyResult(boolean ok, String message) {
            this.ok = ok;
            this.message = message;
        }
    }

    /* ========================= Helpers ========================= */

    private void styleTextField(JTextField tf) {
        tf.setFont(UiKit.scaled(tf, Font.PLAIN, 1.00f));
        tf.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(UiKit.BORDER, 1, true),
                new EmptyBorder(8, 10, 8, 10)
        ));
        tf.setBackground(UiKit.WHITE);
        tf.setForeground(UiKit.TEXT);
        tf.setCaretColor(UiKit.TEXT);
        tf.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
    }

    private void syncUI() {
        styleTab(tabGlobal, globalTab);
        styleTab(tabSelected, !globalTab);

        swatch.setBackground(selectedColor);

        highlightButton(matteBtn, "Matte".equals(material));
        highlightButton(satinBtn, "Satin".equals(material));
        highlightButton(glossBtn, "Gloss".equals(material));

        highlightButton(daylightBtn, "Daylight".equals(lighting));
        highlightButton(warmBtn, "Warm".equals(lighting));
        highlightButton(coolBtn, "Cool".equals(lighting));
        highlightButton(neutralBtn, "Neutral".equals(lighting));

        shadingLabel.setText("Current: " + shading + "%");

        afterBtn.setSelected(afterMode);
        beforeBtn.setSelected(!afterMode);
        styleToggle(beforeBtn, !afterMode);
        styleToggle(afterBtn, afterMode);

        previewPanel.repaint();
        revalidate();
        repaint();
    }

    private void styleTab(JButton b, boolean active) {
        if (active) {
            b.setBackground(UiKit.PRIMARY);
            b.setForeground(Color.WHITE);
            b.setBorder(new LineBorder(UiKit.PRIMARY, 1, true));
        } else {
            b.setBackground(UiKit.META_PILL_BG);
            b.setForeground(UiKit.TEXT);
            b.setBorder(new LineBorder(UiKit.BORDER, 1, true));
        }
    }

    private void highlightButton(JButton b, boolean active) {
        if (active) {
            b.setBackground(UiKit.CHIP_ACTIVE_BG);
            b.setForeground(UiKit.CHIP_ACTIVE_TEXT);
            b.setBorder(new LineBorder(UiKit.isDarkBlueMode() ? new Color(0x3B82F6) : new Color(0xC7D2FE), 1, true));
        } else {
            b.setBackground(UiKit.WHITE);
            b.setForeground(UiKit.TEXT);
            b.setBorder(new LineBorder(UiKit.BORDER, 1, true));
        }
    }

    private void styleToggle(JToggleButton b, boolean active) {
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setBorder(new EmptyBorder(6, 10, 6, 10));
        b.setFont(UiKit.scaled(b, Font.BOLD, 0.90f));
        b.setOpaque(true);
        if (active) {
            b.setBackground(UiKit.PRIMARY);
            b.setForeground(Color.WHITE);
        } else {
            b.setBackground(UiKit.META_PILL_BG);
            b.setForeground(UiKit.TEXT);
        }
    }

    private static String toHex(Color c) {
        return String.format("#%02X%02X%02X", c.getRed(), c.getGreen(), c.getBlue());
    }

    private static Color parseHex(String s) {
        if (s == null) return null;
        String v = s.trim();
        if (v.startsWith("#")) v = v.substring(1);
        if (v.length() != 6) return null;
        try {
            int rgb = Integer.parseInt(v, 16);
            return new Color(rgb);
        } catch (Exception e) {
            return null;
        }
    }

    private static int clamp(int v, int lo, int hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    /* ========================= Gradient Picker ========================= */

    private static class GradientPicker extends JPanel {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();

            float[] stops = {0f, 0.2f, 0.4f, 0.6f, 0.8f, 1f};
            Color[] colors = {
                    new Color(0xEF4444),
                    new Color(0xF59E0B),
                    new Color(0xEAB308),
                    new Color(0x22C55E),
                    new Color(0x3B82F6),
                    new Color(0xA855F7)
            };
            LinearGradientPaint lg = new LinearGradientPaint(0, 0, w, 0, stops, colors);
            g2.setPaint(lg);
            g2.fillRoundRect(0, 0, w, h, 12, 12);

            GradientPaint vp = new GradientPaint(
                    0, 0, new Color(255, 255, 255, 0),
                    0, h, new Color(255, 255, 255, 90)
            );
            g2.setPaint(vp);
            g2.fillRoundRect(0, 0, w, h, 12, 12);

            g2.dispose();
        }

        Color colorAt(int x, int y) {
            int w = Math.max(1, getWidth());
            float t = Math.min(1f, Math.max(0f, x / (float) w));

            Color[] cs = {
                    new Color(0xEF4444),
                    new Color(0xF59E0B),
                    new Color(0xEAB308),
                    new Color(0x22C55E),
                    new Color(0x3B82F6),
                    new Color(0xA855F7)
            };
            float scaled = t * (cs.length - 1);
            int i = (int) Math.floor(scaled);
            int j = Math.min(cs.length - 1, i + 1);
            float local = scaled - i;

            Color a = cs[i];
            Color b = cs[j];

            int r = (int) (a.getRed() + (b.getRed() - a.getRed()) * local);
            int g = (int) (a.getGreen() + (b.getGreen() - a.getGreen()) * local);
            int bl = (int) (a.getBlue() + (b.getBlue() - a.getBlue()) * local);

            int h = Math.max(1, getHeight());
            float vy = Math.min(1f, Math.max(0f, y / (float) h));
            float white = vy * 0.25f;

            r = (int) (r + (255 - r) * white);
            g = (int) (g + (255 - g) * white);
            bl = (int) (bl + (255 - bl) * white);

            return new Color(clamp255(r), clamp255(g), clamp255(bl));
        }

        private static int clamp255(int v) { return Math.max(0, Math.min(255, v)); }
    }

    /* ========================= Preview Panel ========================= */

    private class PreviewPanel extends JPanel {
        PreviewPanel() {
            setOpaque(true);
            setBackground(UiKit.META_PILL_BG);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();

            g2.setColor(UiKit.META_PILL_BG);
            g2.fillRect(0, 0, w, h);

            g2.setColor(UiKit.BORDER);
            g2.drawLine(0, 0, w, 0);

            int boxW = Math.min(520, Math.max(220, w - 120));
            int boxH = Math.min(320, Math.max(160, h - 140));
            int x = (w - boxW) / 2;
            int y = (h - boxH) / 2;

            Color base = afterMode ? selectedColor : UiKit.BORDER;

            float glossBoost = "Gloss".equals(material) ? 0.22f : ("Satin".equals(material) ? 0.12f : 0.06f);

            float shade = shading / 100f;
            float adj = (shade - 0.5f) * 0.9f;

            Color adjusted = adjust(base, adj);

            if ("Warm".equals(lighting)) adjusted = blend(adjusted, new Color(255, 170, 120), 0.18f);
            if ("Cool".equals(lighting)) adjusted = blend(adjusted, new Color(120, 180, 255), 0.18f);
            if ("Neutral".equals(lighting)) adjusted = blend(adjusted, new Color(240, 240, 240), 0.10f);

            g2.setColor(adjusted);
            g2.fillRoundRect(x, y, boxW, boxH, 22, 22);

            g2.setColor(UiKit.TOGGLE_OFF_BORDER);
            g2.drawRoundRect(x, y, boxW, boxH, 22, 22);

            g2.setColor(new Color(255, 255, 255, (int) (255 * glossBoost)));
            g2.fillRoundRect(x + 14, y + 14, boxW - 28, 38, 18, 18);

            g2.setColor(new Color(0, 0, 0, 110));
            g2.setFont(UiKit.scaled(new JLabel(), Font.BOLD, 1.00f));
            g2.drawString(afterMode ? "After Preview" : "Before Preview", x + 18, y + 70);

            g2.setFont(UiKit.scaled(new JLabel(), Font.PLAIN, 0.92f));
            g2.drawString("Material: " + material, x + 18, y + boxH - 44);
            g2.drawString("Lighting: " + lighting + "   •   Shading: " + shading + "%", x + 18, y + boxH - 22);

            g2.dispose();
        }

        private Color adjust(Color c, float amount) {
            int r = c.getRed();
            int g = c.getGreen();
            int b = c.getBlue();
            if (amount > 0) {
                r = (int) (r + (255 - r) * amount);
                g = (int) (g + (255 - g) * amount);
                b = (int) (b + (255 - b) * amount);
            } else {
                float a = -amount;
                r = (int) (r * (1f - a));
                g = (int) (g * (1f - a));
                b = (int) (b * (1f - a));
            }
            return new Color(clamp255(r), clamp255(g), clamp255(b));
        }

        private Color blend(Color a, Color b, float t) {
            int r = (int) (a.getRed() + (b.getRed() - a.getRed()) * t);
            int g = (int) (a.getGreen() + (b.getGreen() - a.getGreen()) * t);
            int bl = (int) (a.getBlue() + (b.getBlue() - a.getBlue()) * t);
            return new Color(clamp255(r), clamp255(g), clamp255(bl));
        }

        private int clamp255(int v) { return Math.max(0, Math.min(255, v)); }
    }
}