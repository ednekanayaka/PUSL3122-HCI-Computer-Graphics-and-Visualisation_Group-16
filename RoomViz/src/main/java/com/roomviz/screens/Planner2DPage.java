// (FULL FILE IS LONG) — paste this entire file exactly as-is:

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
import java.awt.*;
import java.awt.event.*;
import java.util.UUID;

/**
 * 2D Planner page (Swing UI) – matches the provided screenshot layout:
 * Left: Furniture Library
 * Center: Canvas (grid + room) with draggable items
 * Right: Properties panel (position/rotation/scale/color/shading/layer/delete)
 *
 * ✅ Updated (Coursework Step):
 * - Loads and edits the current Design from AppState
 * - Auto-saves changes to DesignRepository
 * - Exposes selected item id in AppState (for Shading & Colour tools)
 */
public class Planner2DPage extends JPanel {

    private final AppFrame frame;
    private final Router router;
    private final AppState appState;

    private final RoomCanvas canvas;

    // Right panel controls
    private final JLabel selectedTitle = new JLabel("No selection");
    private final JTextField xField = new JTextField("0");
    private final JTextField yField = new JTextField("0");
    private final JSlider rotationSlider = new JSlider(0, 360, 0);
    private final JTextField rotField = new JTextField("0");

    private final JTextField wField = new JTextField("60");
    private final JTextField hField = new JTextField("40");
    private final JCheckBox lockAspect = new JCheckBox("Lock aspect ratio", true);

    private final JSlider shadingSlider = new JSlider(0, 100, 50);

    private final JButton deleteBtn = new JButton("Delete Item");
    private final JButton shadingToolsBtn = UiKit.ghostButton("Open Shading & Colour Tools");

    private final JLabel autosaved = new JLabel("● Not saved yet");
    private long lastSavedAtMs = 0;

    private boolean programmaticUpdate = false;

    // Debounced autosave timer
    private final javax.swing.Timer autosaveTimer;

    public Planner2DPage(AppFrame frame, Router router, AppState appState) {
        this.frame = frame;
        this.router = router;
        this.appState = appState;

        setLayout(new BorderLayout());
        setBackground(UiKit.BG);
        setBorder(new EmptyBorder(16, 16, 16, 16));

        // ===== Main layout: left | center | right =====
        JPanel left = buildFurnitureLibrary();
        JPanel right = buildPropertiesPanel();
        JPanel center = new JPanel(new BorderLayout(12, 12));
        center.setOpaque(false);

        JPanel pageHeader = buildPageHeader();
        center.add(pageHeader, BorderLayout.NORTH);

        canvas = new RoomCanvas();
        UiKit.RoundedPanel canvasCard = new UiKit.RoundedPanel(18, Color.WHITE);
        canvasCard.setBorderPaint(UiKit.BORDER);
        canvasCard.setLayout(new BorderLayout());
        canvasCard.add(canvas, BorderLayout.CENTER);
        center.add(canvasCard, BorderLayout.CENTER);

        add(left, BorderLayout.WEST);
        add(center, BorderLayout.CENTER);
        add(right, BorderLayout.EAST);

        // Load items from current design
        loadDesignIntoCanvas();

        wireRightPanel();
        updatePropertiesFromSelection();

        autosaveTimer = new javax.swing.Timer(700, e -> {
            ((javax.swing.Timer) e.getSource()).stop();
            saveDesign("Auto-saved");
        });
        autosaveTimer.setRepeats(false);

    }

    private void loadDesignIntoCanvas() {
        Design d = appState.getOrCreateCurrentDesign();
        java.util.List<FurnitureItem> items = d.getItems();
        canvas.setItems(items);
        autosaved.setForeground(new Color(0x6B7280));
        autosaved.setText("● Loaded");
    }

    private void markDirtyAndAutosave() {
        if (autosaveTimer.isRunning()) autosaveTimer.restart();
        else autosaveTimer.start();
        autosaved.setForeground(new Color(0xF59E0B));
        autosaved.setText("● Unsaved changes");
    }

    private void saveDesign(String reasonLabel) {
        Design d = appState.getOrCreateCurrentDesign();
        d.setItems(canvas.getItems());
        d.touchUpdatedAtNow();
        appState.getRepo().upsert(d);

        lastSavedAtMs = System.currentTimeMillis();
        autosaved.setForeground(new Color(0x10B981));
        autosaved.setText("● " + reasonLabel);
    }

    /* ========================= LEFT: Furniture Library ========================= */

    private JPanel buildFurnitureLibrary() {
        UiKit.RoundedPanel card = new UiKit.RoundedPanel(18, Color.WHITE);
        card.setBorderPaint(UiKit.BORDER);
        card.setPreferredSize(new Dimension(285, 0));
        card.setLayout(new BorderLayout());
        card.setBorder(new EmptyBorder(14, 14, 14, 14));

        JLabel title = new JLabel("Furniture Library");
        title.setForeground(UiKit.TEXT);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 13.5f));

        JTextField search = UiKit.searchField("Search furniture...");
        search.setPreferredSize(new Dimension(0, 40));

        JPanel chips = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        chips.setOpaque(false);
        chips.add(UiKit.chipPrimary("All"));
        chips.add(UiKit.chip("Seating"));
        chips.add(UiKit.chip("Tables"));

        JPanel top = new JPanel();
        top.setOpaque(false);
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
        top.add(title);
        top.add(Box.createVerticalStrut(10));
        top.add(search);
        top.add(Box.createVerticalStrut(10));
        top.add(chips);

        DefaultListModel<FurnitureTemplate> model = new DefaultListModel<>();
        model.addElement(new FurnitureTemplate("Accent Chair", "32\" × 34\"", FurnitureKind.CHAIR, 64, 48));
        model.addElement(new FurnitureTemplate("Dining Chair", "18\" × 22\"", FurnitureKind.CHAIR, 44, 36));
        model.addElement(new FurnitureTemplate("Lounge Chair", "36\" × 38\"", FurnitureKind.CHAIR, 70, 56));
        model.addElement(new FurnitureTemplate("Rectangular Table", "72\" × 36\"", FurnitureKind.TABLE_RECT, 120, 70));
        model.addElement(new FurnitureTemplate("Round Table", "48\" Ø", FurnitureKind.TABLE_ROUND, 90, 90));
        model.addElement(new FurnitureTemplate("Coffee Table", "48\" × 24\"", FurnitureKind.TABLE_RECT, 90, 50));
        model.addElement(new FurnitureTemplate("End Table", "20\" × 20\"", FurnitureKind.TABLE_RECT, 44, 44));
        model.addElement(new FurnitureTemplate("Console Table", "48\" × 16\"", FurnitureKind.TABLE_RECT, 90, 38));

        JList<FurnitureTemplate> list = new JList<>(model);
        list.setCellRenderer(new FurnitureCell());
        list.setFixedCellHeight(56);
        list.setBorder(new EmptyBorder(10, 6, 10, 6));
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        list.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    FurnitureTemplate t = list.getSelectedValue();
                    if (t != null) {
                        canvas.addItemFromTemplate(t);
                        markDirtyAndAutosave();
                        canvas.requestFocusInWindow();
                    }
                }
            }
        });

        JScrollPane sc = new JScrollPane(list);
        sc.setBorder(BorderFactory.createEmptyBorder());
        sc.getVerticalScrollBar().setUnitIncrement(14);

        card.add(top, BorderLayout.NORTH);
        card.add(sc, BorderLayout.CENTER);
        return card;
    }

    private static class FurnitureCell extends JPanel implements ListCellRenderer<FurnitureTemplate> {
        private final JLabel name = new JLabel();
        private final JLabel size = new JLabel();
        private final JLabel icon = new JLabel();

        FurnitureCell() {
            setLayout(new BorderLayout(10, 0));
            setOpaque(true);
            setBorder(new EmptyBorder(10, 10, 10, 10));

            icon.setPreferredSize(new Dimension(28, 28));
            icon.setHorizontalAlignment(SwingConstants.CENTER);
            icon.setFont(icon.getFont().deriveFont(Font.BOLD, 13f));
            icon.setForeground(new Color(0x4F46E5));

            JPanel text = new JPanel();
            text.setOpaque(false);
            text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));

            name.setFont(name.getFont().deriveFont(Font.BOLD, 12.4f));
            name.setForeground(UiKit.TEXT);

            size.setFont(size.getFont().deriveFont(Font.PLAIN, 11.2f));
            size.setForeground(UiKit.MUTED);

            text.add(name);
            text.add(Box.createVerticalStrut(3));
            text.add(size);

            add(icon, BorderLayout.WEST);
            add(text, BorderLayout.CENTER);
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends FurnitureTemplate> list, FurnitureTemplate value,
                                                      int index, boolean isSelected, boolean cellHasFocus) {
            name.setText(value.name);
            size.setText(value.displaySize);
            icon.setText(value.kind.iconText);

            if (isSelected) {
                setBackground(new Color(0xEEF2FF));
                setBorder(BorderFactory.createCompoundBorder(
                        new LineBorder(new Color(0xC7D2FE), 1, true),
                        new EmptyBorder(10, 10, 10, 10)
                ));
            } else {
                setBackground(Color.WHITE);
                setBorder(new EmptyBorder(10, 10, 10, 10));
            }
            return this;
        }
    }

    /* ========================= CENTER: Page header controls ========================= */

    private JPanel buildPageHeader() {
        UiKit.RoundedPanel header = new UiKit.RoundedPanel(18, Color.WHITE);
        header.setBorderPaint(UiKit.BORDER);
        header.setLayout(new BorderLayout(12, 0));
        header.setBorder(new EmptyBorder(10, 12, 10, 12));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        left.setOpaque(false);

        JLabel appName = new JLabel("RoomPlan");
        appName.setFont(appName.getFont().deriveFont(Font.BOLD, 12.8f));
        appName.setForeground(UiKit.TEXT);

        JLabel divider = new JLabel("|");
        divider.setForeground(UiKit.BORDER);

        Design d = appState.getOrCreateCurrentDesign();
        JLabel designName = new JLabel(d.getName());
        designName.setFont(designName.getFont().deriveFont(Font.PLAIN, 12.6f));
        designName.setForeground(new Color(0x374151));

        left.add(appName);
        left.add(divider);
        left.add(designName);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        right.setOpaque(false);

        JButton undo = UiKit.iconButton("↶");
        JButton redo = UiKit.iconButton("↷");

        JButton save = UiKit.ghostButton("Save");
        save.addActionListener(e -> saveDesign("Saved"));

        autosaved.setForeground(new Color(0x6B7280));
        autosaved.setFont(autosaved.getFont().deriveFont(Font.PLAIN, 11.6f));

        JToggleButton toggle2d = new JToggleButton("2D");
        JToggleButton toggle3d = new JToggleButton("3D");
        ButtonGroup g = new ButtonGroup();
        g.add(toggle2d);
        g.add(toggle3d);
        toggle2d.setSelected(true);

        styleMiniToggle(toggle2d, true);
        styleMiniToggle(toggle3d, false);

        toggle2d.addActionListener(e -> {
            styleMiniToggle(toggle2d, true);
            styleMiniToggle(toggle3d, false);
        });
        toggle3d.addActionListener(e -> {
            saveDesign("Saved before 3D");
            styleMiniToggle(toggle2d, false);
            styleMiniToggle(toggle3d, true);
            if (router != null) router.show(ScreenKeys.VIEW_3D);
        });

        JButton export = UiKit.ghostButton("Export");
        export.addActionListener(e -> JOptionPane.showMessageDialog(
                this,
                "Export is a future enhancement.\n(You already have the UI hook here.)",
                "Export",
                JOptionPane.INFORMATION_MESSAGE
        ));

        right.add(undo);
        right.add(redo);
        right.add(save);
        right.add(autosaved);
        right.add(toggle2d);
        right.add(toggle3d);
        right.add(export);

        header.add(left, BorderLayout.WEST);
        header.add(right, BorderLayout.EAST);
        return header;
    }

    private void styleMiniToggle(AbstractButton b, boolean active) {
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setBorder(new EmptyBorder(8, 12, 8, 12));
        if (active) {
            b.setBackground(new Color(0xEEF2FF));
            b.setForeground(new Color(0x4338CA));
            b.setBorder(BorderFactory.createCompoundBorder(
                    new LineBorder(new Color(0xC7D2FE), 1, true),
                    new EmptyBorder(8, 12, 8, 12)
            ));
        } else {
            b.setBackground(Color.WHITE);
            b.setForeground(new Color(0x374151));
            b.setBorder(BorderFactory.createCompoundBorder(
                    new LineBorder(UiKit.BORDER, 1, true),
                    new EmptyBorder(8, 12, 8, 12)
            ));
        }
    }

    /* ========================= RIGHT: Properties ========================= */

    private JPanel buildPropertiesPanel() {
        UiKit.RoundedPanel card = new UiKit.RoundedPanel(18, Color.WHITE);
        card.setBorderPaint(UiKit.BORDER);
        card.setPreferredSize(new Dimension(320, 0));
        card.setLayout(new BorderLayout());
        card.setBorder(new EmptyBorder(14, 14, 14, 14));

        JPanel content = new JPanel();
        content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("Properties");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 13.5f));
        title.setForeground(UiKit.TEXT);

        selectedTitle.setFont(selectedTitle.getFont().deriveFont(Font.PLAIN, 11.5f));
        selectedTitle.setForeground(UiKit.MUTED);
        selectedTitle.setBorder(new EmptyBorder(4, 0, 0, 0));

        content.add(title);
        content.add(selectedTitle);
        content.add(Box.createVerticalStrut(14));

        content.add(sectionLabel("Position"));
        content.add(twoFieldRow("X Position", xField, "Y Position", yField));
        content.add(Box.createVerticalStrut(14));

        content.add(sectionLabel("Rotation"));
        content.add(rotationRow());
        content.add(Box.createVerticalStrut(14));

        content.add(sectionLabel("Scale"));
        content.add(twoFieldRow("Width", wField, "Height", hField));
        lockAspect.setOpaque(false);
        lockAspect.setForeground(UiKit.MUTED);
        lockAspect.setFont(lockAspect.getFont().deriveFont(Font.PLAIN, 11.5f));
        lockAspect.setBorder(new EmptyBorder(6, 2, 0, 0));
        content.add(lockAspect);
        content.add(Box.createVerticalStrut(14));

        content.add(sectionLabel("Shading Intensity"));
        content.add(shadingRow());
        content.add(Box.createVerticalStrut(10));

        shadingToolsBtn.setBorder(new EmptyBorder(10, 12, 10, 12));
        shadingToolsBtn.addActionListener(e -> {
            if (router != null) router.show(ScreenKeys.SHADING_COLOR);
        });
        content.add(shadingToolsBtn);

        content.add(Box.createVerticalStrut(14));
        content.add(sectionLabel("Layer Order"));
        content.add(layerRow());
        content.add(Box.createVerticalStrut(16));

        deleteBtn.setForeground(UiKit.DANGER);
        deleteBtn.setBackground(Color.WHITE);
        deleteBtn.setFocusPainted(false);
        deleteBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        deleteBtn.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(UiKit.BORDER, 1, true),
                new EmptyBorder(10, 12, 10, 12)
        ));
        content.add(deleteBtn);

        JScrollPane sc = new JScrollPane(content);
        sc.setBorder(BorderFactory.createEmptyBorder());
        sc.getVerticalScrollBar().setUnitIncrement(14);

        card.add(sc, BorderLayout.CENTER);
        return card;
    }

    private JComponent sectionLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(l.getFont().deriveFont(Font.BOLD, 12.4f));
        l.setForeground(new Color(0x374151));
        l.setBorder(new EmptyBorder(0, 0, 8, 0));
        return l;
    }

    private JComponent twoFieldRow(String l1, JTextField f1, String l2, JTextField f2) {
        JPanel row = new JPanel(new GridLayout(1, 2, 10, 0));
        row.setOpaque(false);

        row.add(labeledField(l1, f1));
        row.add(labeledField(l2, f2));
        return row;
    }

    private JComponent labeledField(String label, JTextField field) {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));

        JLabel l = new JLabel(label);
        l.setFont(l.getFont().deriveFont(Font.PLAIN, 11.3f));
        l.setForeground(UiKit.MUTED);

        field.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(UiKit.BORDER, 1, true),
                new EmptyBorder(8, 10, 8, 10)
        ));

        p.add(l);
        p.add(Box.createVerticalStrut(6));
        p.add(field);
        return p;
    }

    private JComponent rotationRow() {
        JPanel row = new JPanel(new BorderLayout(10, 0));
        row.setOpaque(false);

        rotationSlider.setOpaque(false);

        rotField.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(UiKit.BORDER, 1, true),
                new EmptyBorder(8, 10, 8, 10)
        ));
        rotField.setPreferredSize(new Dimension(64, 34));

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        btns.setOpaque(false);

        JButton left90 = UiKit.ghostButton("↺ 90°");
        JButton right90 = UiKit.ghostButton("↻ 90°");

        left90.addActionListener(e -> { canvas.nudgeRotation(-90); markDirtyAndAutosave(); });
        right90.addActionListener(e -> { canvas.nudgeRotation(90); markDirtyAndAutosave(); });

        btns.add(left90);
        btns.add(right90);

        JPanel bottom = new JPanel(new BorderLayout(10, 8));
        bottom.setOpaque(false);
        bottom.add(btns, BorderLayout.WEST);

        row.add(rotationSlider, BorderLayout.CENTER);
        row.add(rotField, BorderLayout.EAST);

        JPanel wrap = new JPanel();
        wrap.setOpaque(false);
        wrap.setLayout(new BoxLayout(wrap, BoxLayout.Y_AXIS));
        wrap.add(row);
        wrap.add(Box.createVerticalStrut(8));
        wrap.add(bottom);

        return wrap;
    }

    private JComponent shadingRow() {
        JPanel row = new JPanel(new BorderLayout(10, 0));
        row.setOpaque(false);

        JLabel dark = new JLabel("Darker");
        dark.setForeground(UiKit.MUTED);
        dark.setFont(dark.getFont().deriveFont(Font.PLAIN, 11.2f));

        JLabel light = new JLabel("Lighter");
        light.setForeground(UiKit.MUTED);
        light.setFont(light.getFont().deriveFont(Font.PLAIN, 11.2f));

        shadingSlider.setOpaque(false);

        row.add(dark, BorderLayout.WEST);
        row.add(shadingSlider, BorderLayout.CENTER);
        row.add(light, BorderLayout.EAST);

        return row;
    }

    private JComponent layerRow() {
        JPanel row = new JPanel(new GridLayout(2, 2, 10, 10));
        row.setOpaque(false);

        JButton forward = UiKit.ghostButton("Forward");
        JButton backward = UiKit.ghostButton("Backward");
        JButton toFront = UiKit.ghostButton("To Front");
        JButton toBack = UiKit.ghostButton("To Back");

        forward.addActionListener(e -> { canvas.layerForward(); markDirtyAndAutosave(); });
        backward.addActionListener(e -> { canvas.layerBackward(); markDirtyAndAutosave(); });
        toFront.addActionListener(e -> { canvas.layerToFront(); markDirtyAndAutosave(); });
        toBack.addActionListener(e -> { canvas.layerToBack(); markDirtyAndAutosave(); });

        row.add(forward);
        row.add(backward);
        row.add(toFront);
        row.add(toBack);

        return row;
    }

    private void wireRightPanel() {
        installApplyOnEnterOrBlur(xField, () -> { applyPositionFromFields(); markDirtyAndAutosave(); });
        installApplyOnEnterOrBlur(yField, () -> { applyPositionFromFields(); markDirtyAndAutosave(); });
        installApplyOnEnterOrBlur(wField, () -> { applyScaleFromFields(); markDirtyAndAutosave(); });
        installApplyOnEnterOrBlur(hField, () -> { applyScaleFromFields(); markDirtyAndAutosave(); });

        rotationSlider.addChangeListener(e -> {
            if (programmaticUpdate) return;
            canvas.setSelectedRotation(rotationSlider.getValue());
            rotField.setText(String.valueOf(rotationSlider.getValue()));
            markDirtyAndAutosave();
        });

        installApplyOnEnterOrBlur(rotField, () -> {
            try {
                int v = Integer.parseInt(rotField.getText().trim());
                v = Math.max(0, Math.min(360, v));
                canvas.setSelectedRotation(v);
                rotationSlider.setValue(v);
                markDirtyAndAutosave();
            } catch (Exception ignored) {}
        });

        shadingSlider.addChangeListener(e -> {
            if (programmaticUpdate) return;
            canvas.setSelectedShading(shadingSlider.getValue());
            markDirtyAndAutosave();
        });

        deleteBtn.addActionListener(e -> {
            canvas.deleteSelected();
            updatePropertiesFromSelection();
            markDirtyAndAutosave();
        });

        canvas.setOnSelectionChanged(() -> {
            updatePropertiesFromSelection();
            FurnitureItem sel = canvas.getSelected();
            appState.setSelectedItemId(sel == null ? null : sel.getId());
        });
    }

    private void applyPositionFromFields() {
        if (programmaticUpdate) return;
        try {
            int x = Integer.parseInt(xField.getText().trim());
            int y = Integer.parseInt(yField.getText().trim());
            canvas.setSelectedPosition(x, y);
        } catch (Exception ignored) {}
    }

    private void applyScaleFromFields() {
        if (programmaticUpdate) return;

        FurnitureItem sel = canvas.getSelected();
        if (sel == null) return;

        try {
            int w = Integer.parseInt(wField.getText().trim());
            int h = Integer.parseInt(hField.getText().trim());

            w = Math.max(10, Math.min(1000, w));
            h = Math.max(10, Math.min(1000, h));

            if (lockAspect.isSelected()) {
                float aspect = sel.getH() == 0 ? 1f : (sel.getW() / (float) sel.getH());
                int computedH = Math.max(10, Math.round(w / aspect));
                h = computedH;
                programmaticUpdate = true;
                hField.setText(String.valueOf(h));
                programmaticUpdate = false;
            }

            canvas.setSelectedSize(w, h);
        } catch (Exception ignored) {}
    }

    private void updatePropertiesFromSelection() {
        FurnitureItem sel = canvas.getSelected();
        programmaticUpdate = true;
        try {
            if (sel == null) {
                selectedTitle.setText("No item selected");
                xField.setText("0");
                yField.setText("0");
                rotField.setText("0");
                rotationSlider.setValue(0);
                wField.setText("0");
                hField.setText("0");
                shadingSlider.setValue(50);
                deleteBtn.setEnabled(false);
                shadingToolsBtn.setEnabled(true); // still can open global tools
                setRightPanelEnabled(false);
                return;
            }

            selectedTitle.setText(sel.getName() + " selected");
            xField.setText(String.valueOf(sel.getX()));
            yField.setText(String.valueOf(sel.getY()));
            rotField.setText(String.valueOf(sel.getRotation()));
            rotationSlider.setValue(sel.getRotation());

            wField.setText(String.valueOf(sel.getW()));
            hField.setText(String.valueOf(sel.getH()));

            shadingSlider.setValue(sel.getShadingPercent());
            deleteBtn.setEnabled(true);
            shadingToolsBtn.setEnabled(true);
            setRightPanelEnabled(true);
        } finally {
            programmaticUpdate = false;
        }
    }

    private void setRightPanelEnabled(boolean enabled) {
        xField.setEnabled(enabled);
        yField.setEnabled(enabled);
        rotField.setEnabled(enabled);
        rotationSlider.setEnabled(enabled);
        wField.setEnabled(enabled);
        hField.setEnabled(enabled);
        lockAspect.setEnabled(enabled);
        shadingSlider.setEnabled(enabled);
    }

    private void installApplyOnEnterOrBlur(JTextField tf, Runnable apply) {
        tf.addActionListener(e -> apply.run());
        tf.addFocusListener(new FocusAdapter() {
            @Override public void focusLost(FocusEvent e) { apply.run(); }
        });
    }

    /* ========================= Model Types ========================= */

    private enum FurnitureKind {
        CHAIR("🪑"),
        TABLE_RECT("▭"),
        TABLE_ROUND("●");

        final String iconText;
        FurnitureKind(String iconText) { this.iconText = iconText; }
    }

    private static class FurnitureTemplate {
        final String name;
        final String displaySize;
        final FurnitureKind kind;
        final int defaultW;
        final int defaultH;

        FurnitureTemplate(String name, String displaySize, FurnitureKind kind, int defaultW, int defaultH) {
            this.name = name;
            this.displaySize = displaySize;
            this.kind = kind;
            this.defaultW = defaultW;
            this.defaultH = defaultH;
        }

        @Override public String toString() { return name; }
    }

    /* ========================= Canvas ========================= */

    private static class RoomCanvas extends JPanel {

        private java.util.List<FurnitureItem> items = new java.util.ArrayList<>();
        private FurnitureItem selected = null;

        private Point dragStartMouse = null;
        private Point dragStartItem = null;

        private Runnable onSelectionChanged = null;

        RoomCanvas() {
            setOpaque(false);
            setFocusable(true);
            setLayout(null);

            setBorder(new EmptyBorder(10, 10, 10, 10));

            MouseAdapter ma = new MouseAdapter() {
                @Override public void mousePressed(MouseEvent e) {
                    requestFocusInWindow();
                    FurnitureItem hit = hitTest(e.getPoint());
                    setSelected(hit);
                    if (hit != null) {
                        dragStartMouse = e.getPoint();
                        dragStartItem = new Point(hit.getX(), hit.getY());
                    } else {
                        dragStartMouse = null;
                        dragStartItem = null;
                    }
                    repaint();
                }

                @Override public void mouseDragged(MouseEvent e) {
                    if (selected == null || dragStartMouse == null || dragStartItem == null) return;
                    int dx = e.getX() - dragStartMouse.x;
                    int dy = e.getY() - dragStartMouse.y;
                    selected.setX(dragStartItem.x + dx);
                    selected.setY(dragStartItem.y + dy);
                    repaint();
                    fireSelectionChanged();
                }

                @Override public void mouseReleased(MouseEvent e) {
                    dragStartMouse = null;
                    dragStartItem = null;
                }
            };

            addMouseListener(ma);
            addMouseMotionListener(ma);

            getInputMap(WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "deleteSelected");
            getActionMap().put("deleteSelected", new AbstractAction() {
                @Override public void actionPerformed(ActionEvent e) {
                    deleteSelected();
                }
            });
        }

        void setOnSelectionChanged(Runnable r) { this.onSelectionChanged = r; }

        java.util.List<FurnitureItem> getItems() { return items; }

        void setItems(java.util.List<FurnitureItem> items) {
            this.items = (items == null) ? new java.util.ArrayList<>() : items;
            if (!this.items.contains(selected)) selected = null;
            repaint();
            fireSelectionChanged();
        }

        FurnitureItem getSelected() { return selected; }

        void addItemFromTemplate(FurnitureTemplate t) {
            int cx = Math.max(40, getWidth() / 2 - t.defaultW / 2);
            int cy = Math.max(40, getHeight() / 2 - t.defaultH / 2);

            FurnitureItem it = new FurnitureItem(
                    UUID.randomUUID().toString(),
                    t.name,
                    t.kind.name(),
                    cx, cy, t.defaultW, t.defaultH
            );

            items.add(it);
            setSelected(it);
            repaint();
        }

        void setSelected(FurnitureItem it) {
            selected = it;
            fireSelectionChanged();
        }

        void fireSelectionChanged() {
            if (onSelectionChanged != null) onSelectionChanged.run();
        }

        FurnitureItem hitTest(Point p) {
            for (int i = items.size() - 1; i >= 0; i--) {
                FurnitureItem it = items.get(i);
                Rectangle r = new Rectangle(it.getX(), it.getY(), it.getW(), it.getH());
                if (r.contains(p)) return it;
            }
            return null;
        }

        void setSelectedPosition(int x, int y) {
            if (selected == null) return;
            selected.setX(x);
            selected.setY(y);
            repaint();
            fireSelectionChanged();
        }

        void setSelectedSize(int w, int h) {
            if (selected == null) return;
            selected.setW(w);
            selected.setH(h);
            repaint();
            fireSelectionChanged();
        }

        void setSelectedRotation(int deg) {
            if (selected == null) return;
            int v = ((deg % 360) + 360) % 360;
            selected.setRotation(v);
            repaint();
            fireSelectionChanged();
        }

        void nudgeRotation(int delta) {
            if (selected == null) return;
            setSelectedRotation(selected.getRotation() + delta);
        }

        void setSelectedShading(int v) {
            if (selected == null) return;
            selected.setShadingPercent(Math.max(0, Math.min(100, v)));
            repaint();
            fireSelectionChanged();
        }

        void deleteSelected() {
            if (selected == null) return;
            items.remove(selected);
            selected = null;
            repaint();
            fireSelectionChanged();
        }

        void layerForward() {
            if (selected == null) return;
            int idx = items.indexOf(selected);
            if (idx < 0 || idx == items.size() - 1) return;
            items.remove(idx);
            items.add(idx + 1, selected);
            repaint();
        }

        void layerBackward() {
            if (selected == null) return;
            int idx = items.indexOf(selected);
            if (idx <= 0) return;
            items.remove(idx);
            items.add(idx - 1, selected);
            repaint();
        }

        void layerToFront() {
            if (selected == null) return;
            items.remove(selected);
            items.add(selected);
            repaint();
        }

        void layerToBack() {
            if (selected == null) return;
            items.remove(selected);
            items.add(0, selected);
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();

            paintGrid(g2, w, h);

            int pad = 46;
            int roomW = w - pad * 2;
            int roomH = h - pad * 2;

            g2.setColor(new Color(0, 0, 0, 18));
            g2.fillRoundRect(pad + 2, pad + 3, roomW, roomH, 22, 22);

            g2.setColor(new Color(0xF3F4F6));
            g2.fillRoundRect(pad, pad, roomW, roomH, 22, 22);

            g2.setColor(new Color(0xD1D5DB));
            g2.setStroke(new BasicStroke(1.2f));
            g2.drawRoundRect(pad, pad, roomW, roomH, 22, 22);

            paintRulers(g2, w, h);

            for (FurnitureItem it : items) {
                paintItem(g2, it);
            }

            g2.dispose();
        }

        private void paintGrid(Graphics2D g2, int w, int h) {
            g2.setColor(new Color(0xEEF2F7));
            int step = 24;
            for (int x = 0; x < w; x += step) g2.drawLine(x, 0, x, h);
            for (int y = 0; y < h; y += step) g2.drawLine(0, y, w, y);
        }

        private void paintRulers(Graphics2D g2, int w, int h) {
            g2.setColor(new Color(0xE5E7EB));
            g2.fillRect(0, 0, w, 28);
            g2.fillRect(0, 0, 28, h);

            g2.setColor(new Color(0x9CA3AF));
            g2.setFont(g2.getFont().deriveFont(Font.PLAIN, 10.5f));

            int step = 60;
            for (int x = 28; x < w; x += step) {
                g2.drawLine(x, 28, x, 22);
                g2.drawString(String.valueOf(x - 28), x + 2, 18);
            }
            for (int y = 28; y < h; y += step) {
                g2.drawLine(28, y, 22, y);
                g2.drawString(String.valueOf(y - 28), 4, y + 4);
            }

            g2.setColor(new Color(0xD1D5DB));
            g2.drawLine(0, 28, w, 28);
            g2.drawLine(28, 0, 28, h);
        }

        private void paintItem(Graphics2D g2, FurnitureItem it) {
            Rectangle r = new Rectangle(it.getX(), it.getY(), it.getW(), it.getH());
            boolean isSel = (it == selected);

            Color base = parseHexOrDefault(it.getColorHex(), new Color(0x3B82F6));
            float shade = it.getShadingPercent() / 100f;
            Color shaded = mix(base, Color.BLACK, 0.22f * (1f - shade)); // lower shade => darker

            g2.setColor(shaded);

            String kind = it.getKind() == null ? "CHAIR" : it.getKind();
            boolean round = "TABLE_ROUND".equals(kind);

            if (round) g2.fillOval(r.x, r.y, r.width, r.height);
            else g2.fillRoundRect(r.x, r.y, r.width, r.height, 14, 14);

            g2.setColor(new Color(0, 0, 0, 40));
            if (round) g2.drawOval(r.x, r.y, r.width, r.height);
            else g2.drawRoundRect(r.x, r.y, r.width, r.height, 14, 14);

            g2.setColor(Color.WHITE);
            g2.setFont(g2.getFont().deriveFont(Font.BOLD, 12f));
            String icon = round ? "●" : ("TABLE_RECT".equals(kind) ? "▭" : "🪑");
            FontMetrics fm = g2.getFontMetrics();
            int tx = r.x + (r.width - fm.stringWidth(icon)) / 2;
            int ty = r.y + (r.height + fm.getAscent()) / 2 - 2;
            g2.drawString(icon, tx, ty);

            if (isSel) {
                g2.setColor(new Color(0x2563EB));
                g2.setStroke(new BasicStroke(2.0f));
                if (round) g2.drawOval(r.x - 2, r.y - 2, r.width + 4, r.height + 4);
                else g2.drawRoundRect(r.x - 2, r.y - 2, r.width + 4, r.height + 4, 16, 16);
            }
        }

        private static Color parseHexOrDefault(String hex, Color fallback) {
            if (hex == null) return fallback;
            String v = hex.trim();
            if (v.startsWith("#")) v = v.substring(1);
            if (v.length() != 6) return fallback;
            try {
                int rgb = Integer.parseInt(v, 16);
                return new Color(rgb);
            } catch (Exception e) {
                return fallback;
            }
        }

        private static Color mix(Color a, Color b, float t) {
            t = Math.max(0f, Math.min(1f, t));
            int r = (int) (a.getRed() * (1 - t) + b.getRed() * t);
            int g = (int) (a.getGreen() * (1 - t) + b.getGreen() * t);
            int bl = (int) (a.getBlue() * (1 - t) + b.getBlue() * t);
            return new Color(r, g, bl);
        }
    }
}
