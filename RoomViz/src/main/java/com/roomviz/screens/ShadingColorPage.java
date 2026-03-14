package com.roomviz.screens;

import com.roomviz.app.AppFrame;
import com.roomviz.app.Router;
import com.roomviz.app.ScreenKeys;
import com.roomviz.data.AppState;
import com.roomviz.model.Design;
import com.roomviz.model.FurnitureItem;
import com.roomviz.ui.UiKit;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

/**
 * Shading & Color Tools page (Swing UI) – polished + real AppState integration.
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
    private final JButton daylightBtn = new JButton("☀  Daylight");
    private final JButton warmBtn = new JButton("🔥  Warm");
    private final JButton coolBtn = new JButton("❄  Cool");
    private final JButton neutralBtn = new JButton("◻  Neutral");

    // Before/After
    private final JToggleButton beforeBtn = new JToggleButton("Before");
    private final JToggleButton afterBtn = new JToggleButton("After");

    private final PreviewPanel previewPanel = new PreviewPanel();

    /** ✅ Backward compatible constructor (old calls still work) */
    public ShadingColorPage(AppFrame frame) {
        this(frame, null, null);
    }

    /** ✅ NEW constructor to match ShellScreen: (frame, router, appState) */
    public ShadingColorPage(AppFrame frame, Router router, AppState appState) {
        this.router = router;
        this.appState = appState;

        setLayout(new BorderLayout());
        setBackground(UiKit.BG);
        setBorder(new EmptyBorder(14, 14, 14, 14));

        add(buildCardShell(), BorderLayout.CENTER);

        wireLiveHexValidation();
        loadInitialFromDesign(); // ✅ NEW: prefill from selected item or first item
        syncUI();
    }

    /* ========================= Load initial state from design ========================= */

    private void loadInitialFromDesign() {
        if (appState == null) return;

        Design d = appState.getOrCreateCurrentDesign();
        List<FurnitureItem> items = d.getItems();
        if (items == null || items.isEmpty()) return;

        FurnitureItem target = findSelectedItemOrFirst(items);

        // Pull values from the item into UI state (fallback to defaults)
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

        // Left tools panel
        JPanel tools = buildToolsPanel();
        tools.setPreferredSize(new Dimension(340, 0));
        tools.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, UiKit.BORDER));

        // Right preview
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

        // Top header
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.setBorder(new EmptyBorder(16, 16, 12, 16));

        JLabel title = new JLabel("Shading & Colour Tools");
        title.setForeground(UiKit.TEXT);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 14.5f));

        JButton close = UiKit.iconButton("✕");
        close.setToolTipText("Close");
        close.addActionListener(e -> {
            if (router != null) {
                router.show(ScreenKeys.PLANNER_2D);
            } else {
                JOptionPane.showMessageDialog(
                        this,
                        "Close tools (demo).",
                        "Close",
                        JOptionPane.INFORMATION_MESSAGE
                );
            }
        });

        header.add(title, BorderLayout.WEST);
        header.add(close, BorderLayout.EAST);

        // Tabs row (custom styled, no disabled look)
        JPanel tabs = new JPanel(new GridLayout(1, 2, 10, 0));
        tabs.setOpaque(false);
        tabs.setBorder(new EmptyBorder(0, 16, 10, 16));

        styleTabButton(tabGlobal);
        styleTabButton(tabSelected);

        tabGlobal.addActionListener(e -> {
            globalTab = true;
            syncUI();
        });
        tabSelected.addActionListener(e -> {
            globalTab = false;
            syncUI();
        });

        tabs.add(tabGlobal);
        tabs.add(tabSelected);

        JLabel hint = new JLabel("Apply changes to entire room or selected furniture");
        hint.setForeground(UiKit.MUTED);
        hint.setFont(hint.getFont().deriveFont(11.3f));
        hint.setBorder(new EmptyBorder(0, 16, 12, 16));

        // Scrollable content
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
        b.setFont(b.getFont().deriveFont(Font.BOLD, 11.8f));
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
        title.setFont(title.getFont().deriveFont(Font.BOLD, 13.5f));

        // Before/After toggle
        JPanel toggle = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        toggle.setOpaque(false);

        ButtonGroup g = new ButtonGroup();
        g.add(beforeBtn);
        g.add(afterBtn);

        beforeBtn.addActionListener(e -> {
            afterMode = false;
            syncUI();
        });
        afterBtn.addActionListener(e -> {
            afterMode = true;
            syncUI();
        });

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
        t.setFont(t.getFont().deriveFont(Font.BOLD, 12.3f));

        JLabel info = new JLabel("ⓘ");
        info.setForeground(new Color(0x9CA3AF));
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
        if (!hexField.hasFocus()) {
            hexField.setText(toHex(selectedColor));
        }
        syncUI();
    }

    private JComponent hexRow() {
        JPanel row = new JPanel(new BorderLayout(10, 0));
        row.setOpaque(false);

        swatch.setPreferredSize(new Dimension(34, 34));
        swatch.setBackground(selectedColor);
        swatch.setBorder(new LineBorder(UiKit.BORDER, 1, true));

        styleTextField(hexField);

        // Enter to apply
        hexField.addActionListener(e -> {
            Color c = parseHex(hexField.getText());
            if (c != null) {
                selectedColor = c;
                syncUI();
            } else {
                hexField.setText(toHex(selectedColor));
            }
        });

        row.add(swatch, BorderLayout.WEST);
        row.add(hexField, BorderLayout.CENTER);

        return row;
    }

    private void wireLiveHexValidation() {
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

    private JComponent quickPalettes() {
        JPanel wrap = new JPanel();
        wrap.setOpaque(false);
        wrap.setLayout(new BoxLayout(wrap, BoxLayout.Y_AXIS));

        JLabel t = new JLabel("Quick Palettes");
        t.setForeground(UiKit.MUTED);
        t.setFont(t.getFont().deriveFont(11.3f));

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
        t.setFont(t.getFont().deriveFont(11.3f));

        JPanel row = new JPanel(new GridLayout(1, 3, 10, 0));
        row.setOpaque(false);
        row.setBorder(new EmptyBorder(8, 0, 0, 0));

        styleSmallToggle(matteBtn, "Matte");
        styleSmallToggle(satinBtn, "Satin");
        styleSmallToggle(glossBtn, "Gloss");

        row.add(matteBtn);
        row.add(satinBtn);
        row.add(glossBtn);

        wrap.add(t);
        wrap.add(row);
        return wrap;
    }

    private void styleSmallToggle(JButton btn, String name) {
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setFont(btn.getFont().deriveFont(Font.BOLD, 11.5f));
        btn.setBorder(new EmptyBorder(10, 10, 10, 10));
        btn.setOpaque(true);
        btn.addActionListener(e -> {
            material = name;
            syncUI();
        });
    }

    private JComponent shadingSlider() {
        JPanel wrap = new JPanel();
        wrap.setOpaque(false);
        wrap.setLayout(new BoxLayout(wrap, BoxLayout.Y_AXIS));

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);

        JLabel left = new JLabel("Darker");
        left.setForeground(new Color(0x9CA3AF));
        left.setFont(left.getFont().deriveFont(11.3f));

        JLabel right = new JLabel("Lighter");
        right.setForeground(new Color(0x9CA3AF));
        right.setFont(right.getFont().deriveFont(11.3f));

        shadingLabel.setForeground(UiKit.MUTED);
        shadingLabel.setFont(shadingLabel.getFont().deriveFont(11.3f));
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
        t.setFont(t.getFont().deriveFont(11.3f));

        JPanel grid = new JPanel(new GridLayout(2, 2, 10, 10));
        grid.setOpaque(false);
        grid.setBorder(new EmptyBorder(8, 0, 0, 0));

        stylePreset(daylightBtn, "Daylight");
        stylePreset(warmBtn, "Warm");
        stylePreset(coolBtn, "Cool");
        stylePreset(neutralBtn, "Neutral");

        grid.add(daylightBtn);
        grid.add(warmBtn);
        grid.add(coolBtn);
        grid.add(neutralBtn);

        wrap.add(t);
        wrap.add(grid);
        return wrap;
    }

    private void stylePreset(JButton b, String preset) {
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setBorder(new EmptyBorder(10, 12, 10, 12));
        b.setFont(b.getFont().deriveFont(Font.BOLD, 11.6f));
        b.setOpaque(true);
        b.addActionListener(e -> {
            lighting = preset;
            syncUI();
        });
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
        reset.setForeground(new Color(0x6B7280));
        reset.setFont(reset.getFont().deriveFont(Font.PLAIN, 11.3f));
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

    /* ========================= REAL AppState + Design integration ========================= */

    private ApplyResult applyToDesign() {
        if (appState == null) {
            return new ApplyResult(false, "AppState is not wired into this screen yet.\n(Use the constructor: new ShadingColorPage(frame, router, appState))");
        }

        Design design = appState.getOrCreateCurrentDesign();
        List<FurnitureItem> items = design.getItems();
        if (items == null || items.isEmpty()) {
            return new ApplyResult(false, "No furniture items exist in this design yet.\nGo to Planner 2D and add an item first.");
        }

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

        // touch updated time + persist
        design.setLastUpdatedEpochMs(System.currentTimeMillis());
        appState.getRepo().upsert(design);

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
        tf.setFont(tf.getFont().deriveFont(12.8f));
        tf.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(UiKit.BORDER, 1, true),
                new EmptyBorder(8, 10, 8, 10)
        ));
        tf.setBackground(Color.WHITE);
        tf.setForeground(new Color(0x111827));
        tf.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
    }

    private void syncUI() {
        // tabs styling
        styleTab(tabGlobal, globalTab);
        styleTab(tabSelected, !globalTab);

        // swatch
        swatch.setBackground(selectedColor);

        // material highlight
        highlightButton(matteBtn, "Matte".equals(material));
        highlightButton(satinBtn, "Satin".equals(material));
        highlightButton(glossBtn, "Gloss".equals(material));

        // lighting highlight
        highlightButton(daylightBtn, "Daylight".equals(lighting));
        highlightButton(warmBtn, "Warm".equals(lighting));
        highlightButton(coolBtn, "Cool".equals(lighting));
        highlightButton(neutralBtn, "Neutral".equals(lighting));

        // shading label
        shadingLabel.setText("Current: " + shading + "%");

        // before/after
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
            b.setBackground(new Color(0x2563EB));
            b.setForeground(Color.WHITE);
            b.setBorder(new LineBorder(new Color(0x2563EB), 1, true));
        } else {
            b.setBackground(new Color(0xF3F4F6));
            b.setForeground(new Color(0x111827));
            b.setBorder(new LineBorder(UiKit.BORDER, 1, true));
        }
    }

    private void highlightButton(JButton b, boolean active) {
        if (active) {
            b.setBackground(new Color(0xEEF2FF));
            b.setForeground(new Color(0x1D4ED8));
            b.setBorder(new LineBorder(new Color(0xC7D2FE), 1, true));
        } else {
            b.setBackground(Color.WHITE);
            b.setForeground(new Color(0x111827));
            b.setBorder(new LineBorder(UiKit.BORDER, 1, true));
        }
    }

    private void styleToggle(JToggleButton b, boolean active) {
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setBorder(new EmptyBorder(6, 10, 6, 10));
        b.setFont(b.getFont().deriveFont(Font.BOLD, 11.4f));
        b.setOpaque(true);
        if (active) {
            b.setBackground(new Color(0x2563EB));
            b.setForeground(Color.WHITE);
        } else {
            b.setBackground(new Color(0xF3F4F6));
            b.setForeground(new Color(0x111827));
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

            // vertical whitening overlay (top -> bottom)
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
            float white = vy * 0.25f; // more white lower down

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
            setBackground(new Color(0xF3F4F6));
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();

            g2.setColor(new Color(0xF3F4F6));
            g2.fillRect(0, 0, w, h);

            // subtle top border
            g2.setColor(new Color(0xE5E7EB));
            g2.drawLine(0, 0, w, 0);

            int boxW = Math.min(520, Math.max(220, w - 120));
            int boxH = Math.min(320, Math.max(160, h - 140));
            int x = (w - boxW) / 2;
            int y = (h - boxH) / 2;

            Color base = afterMode ? selectedColor : new Color(0xE5E7EB);

            float glossBoost = "Gloss".equals(material) ? 0.22f : ("Satin".equals(material) ? 0.12f : 0.06f);

            float shade = shading / 100f;
            float adj = (shade - 0.5f) * 0.9f;

            Color adjusted = adjust(base, adj);

            if ("Warm".equals(lighting)) adjusted = blend(adjusted, new Color(255, 170, 120), 0.18f);
            if ("Cool".equals(lighting)) adjusted = blend(adjusted, new Color(120, 180, 255), 0.18f);
            if ("Neutral".equals(lighting)) adjusted = blend(adjusted, new Color(240, 240, 240), 0.10f);

            // card
            g2.setColor(adjusted);
            g2.fillRoundRect(x, y, boxW, boxH, 22, 22);

            g2.setColor(new Color(0xD1D5DB));
            g2.drawRoundRect(x, y, boxW, boxH, 22, 22);

            // highlight strip for material effect
            g2.setColor(new Color(255, 255, 255, (int) (255 * glossBoost)));
            g2.fillRoundRect(x + 14, y + 14, boxW - 28, 38, 18, 18);

            // label
            g2.setColor(new Color(0, 0, 0, 110));
            g2.setFont(g2.getFont().deriveFont(Font.BOLD, 13f));
            g2.drawString(afterMode ? "After Preview" : "Before Preview", x + 18, y + 70);

            // small metadata
            g2.setFont(g2.getFont().deriveFont(Font.PLAIN, 12f));
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
