package com.roomviz.screens;

import com.roomviz.app.AppFrame;
import com.roomviz.ui.UiKit;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.*;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * 2D Planner page (Swing UI) – matches the provided screenshot layout:
 * Left: Furniture Library
 * Center: Canvas (grid + room) with draggable items
 * Right: Properties panel (position/rotation/scale/color/shading/layer/delete)
 *
 * NOTE: This is an interface + basic interaction scaffold (HCI-focused).
 * Persistence/export can be added later (coursework iteration).
 */
public class Planner2DPage extends JPanel {

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

    private final JSlider shadingSlider = new JSlider(0, 100, 60);

    private final JButton deleteBtn = new JButton("Delete Item");

    private boolean programmaticUpdate = false;

    public Planner2DPage(AppFrame frame) {
        setLayout(new BorderLayout());
        setBackground(UiKit.BG);
        setBorder(new EmptyBorder(16, 16, 16, 16));

        // ===== Main layout: left | center | right =====
        JPanel left = buildFurnitureLibrary();
        JPanel right = buildPropertiesPanel();
        JPanel center = new JPanel(new BorderLayout(12, 12));
        center.setOpaque(false);

        // header row inside page (like screenshot: RoomPlan | Living Room Design + controls)
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

        wireRightPanel();
        updatePropertiesFromSelection();
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

        // chips row
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

        // list of furniture (simple JList)
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

        // double click to add
        list.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    FurnitureTemplate t = list.getSelectedValue();
                    if (t != null) {
                        canvas.addItemFromTemplate(t);
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

        // Left: "RoomPlan | Living Room Design"
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        left.setOpaque(false);

        JLabel appName = new JLabel("RoomPlan");
        appName.setFont(appName.getFont().deriveFont(Font.BOLD, 12.8f));
        appName.setForeground(UiKit.TEXT);

        JLabel divider = new JLabel("|");
        divider.setForeground(UiKit.BORDER);

        JLabel designName = new JLabel("Living Room Design");
        designName.setFont(designName.getFont().deriveFont(Font.PLAIN, 12.6f));
        designName.setForeground(new Color(0x374151));

        left.add(appName);
        left.add(divider);
        left.add(designName);

        // Right: Save + status + 2D/3D + Export
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        right.setOpaque(false);

        JButton undo = UiKit.iconButton("↶");
        JButton redo = UiKit.iconButton("↷");
        JButton save = UiKit.ghostButton("Save");

        JLabel autosaved = new JLabel("● Auto-saved 2m ago");
        autosaved.setForeground(new Color(0x10B981));
        autosaved.setFont(autosaved.getFont().deriveFont(Font.PLAIN, 11.6f));

        JToggleButton toggle2d = new JToggleButton("2D");
        JToggleButton toggle3d = new JToggleButton("3D");
        ButtonGroup g = new ButtonGroup();
        g.add(toggle2d);
        g.add(toggle3d);
        toggle2d.setSelected(true);

        styleMiniToggle(toggle2d, true);
        styleMiniToggle(toggle3d, false);

        toggle2d.addActionListener(e -> { styleMiniToggle(toggle2d, true); styleMiniToggle(toggle3d, false); });
        toggle3d.addActionListener(e -> { styleMiniToggle(toggle2d, false); styleMiniToggle(toggle3d, true); });

        JButton export = UiKit.ghostButton("Export");

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

        content.add(sectionLabel("Color"));
        content.add(colorPaletteRow());
        content.add(Box.createVerticalStrut(14));

        content.add(sectionLabel("Shading Intensity"));
        content.add(shadingRow());
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
        rotationSlider.setValue(0);

        rotField.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(UiKit.BORDER, 1, true),
                new EmptyBorder(8, 10, 8, 10)
        ));
        rotField.setPreferredSize(new Dimension(64, 34));

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        btns.setOpaque(false);

        JButton left90 = UiKit.ghostButton("↺ 90°");
        JButton right90 = UiKit.ghostButton("↻ 90°");

        left90.addActionListener(e -> canvas.nudgeRotation(-90));
        right90.addActionListener(e -> canvas.nudgeRotation(90));

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

    private JComponent colorPaletteRow() {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        row.setOpaque(false);

        Color[] colors = new Color[] {
                new Color(0x3B82F6), // blue
                new Color(0x6B7280), // gray
                new Color(0xF87171), // red
                new Color(0x34D399), // green
                new Color(0xFBBF24), // amber
                new Color(0xA78BFA)  // purple
        };

        for (Color c : colors) {
            JButton b = new JButton();
            b.setPreferredSize(new Dimension(26, 26));
            b.setFocusPainted(false);
            b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            b.setBackground(c);
            b.setBorder(new LineBorder(new Color(0xE5E7EB), 2, true));
            b.addActionListener(e -> canvas.setSelectedColor(c));
            row.add(b);
        }

        return row;
    }

    private JComponent shadingRow() {
        JPanel row = new JPanel(new BorderLayout(10, 0));
        row.setOpaque(false);

        JLabel light = new JLabel("Light");
        light.setForeground(UiKit.MUTED);
        light.setFont(light.getFont().deriveFont(Font.PLAIN, 11.2f));

        JLabel dark = new JLabel("Dark");
        dark.setForeground(UiKit.MUTED);
        dark.setFont(dark.getFont().deriveFont(Font.PLAIN, 11.2f));

        shadingSlider.setOpaque(false);

        row.add(light, BorderLayout.WEST);
        row.add(shadingSlider, BorderLayout.CENTER);
        row.add(dark, BorderLayout.EAST);

        return row;
    }

    private JComponent layerRow() {
        JPanel row = new JPanel(new GridLayout(2, 2, 10, 10));
        row.setOpaque(false);

        JButton forward = UiKit.ghostButton("Forward");
        JButton backward = UiKit.ghostButton("Backward");
        JButton toFront = UiKit.ghostButton("To Front");
        JButton toBack = UiKit.ghostButton("To Back");

        forward.addActionListener(e -> canvas.layerForward());
        backward.addActionListener(e -> canvas.layerBackward());
        toFront.addActionListener(e -> canvas.layerToFront());
        toBack.addActionListener(e -> canvas.layerToBack());

        row.add(forward);
        row.add(backward);
        row.add(toFront);
        row.add(toBack);

        return row;
    }

    private void wireRightPanel() {
        // numeric fields: apply on Enter or focus lost
        installApplyOnEnterOrBlur(xField, this::applyPositionFromFields);
        installApplyOnEnterOrBlur(yField, this::applyPositionFromFields);
        installApplyOnEnterOrBlur(wField, this::applyScaleFromFields);
        installApplyOnEnterOrBlur(hField, this::applyScaleFromFields);

        rotationSlider.addChangeListener(e -> {
            if (programmaticUpdate) return;
            canvas.setSelectedRotation(rotationSlider.getValue());
            rotField.setText(String.valueOf(rotationSlider.getValue()));
        });

        installApplyOnEnterOrBlur(rotField, () -> {
            try {
                int v = Integer.parseInt(rotField.getText().trim());
                v = Math.max(0, Math.min(360, v));
                canvas.setSelectedRotation(v);
                rotationSlider.setValue(v);
            } catch (Exception ignored) {}
        });

        shadingSlider.addChangeListener(e -> {
            if (programmaticUpdate) return;
            canvas.setSelectedShading(shadingSlider.getValue());
        });

        deleteBtn.addActionListener(e -> {
            canvas.deleteSelected();
            updatePropertiesFromSelection();
        });

        // listen to selection changes from canvas
        canvas.setOnSelectionChanged(this::updatePropertiesFromSelection);
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
                // maintain aspect based on whichever field was edited last is hard in Swing without more wiring,
                // so we keep aspect from current selection:
                float aspect = sel.h == 0 ? 1f : (sel.w / (float) sel.h);
                // if user changed width significantly, compute height:
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
                shadingSlider.setValue(60);
                deleteBtn.setEnabled(false);
                setRightPanelEnabled(false);
                return;
            }

            selectedTitle.setText(sel.name + " selected");
            xField.setText(String.valueOf(sel.x));
            yField.setText(String.valueOf(sel.y));
            rotField.setText(String.valueOf(sel.rotation));
            rotationSlider.setValue(sel.rotation);

            wField.setText(String.valueOf(sel.w));
            hField.setText(String.valueOf(sel.h));

            shadingSlider.setValue(sel.shading);
            deleteBtn.setEnabled(true);
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

    private static class FurnitureItem {
        String name;
        FurnitureKind kind;

        int x, y;     // canvas coords (top-left of item)
        int w, h;

        int rotation; // degrees
        int shading;  // 0..100

        Color color;

        FurnitureItem(String name, FurnitureKind kind, int x, int y, int w, int h) {
            this.name = name;
            this.kind = kind;
            this.x = x; this.y = y;
            this.w = w; this.h = h;
            this.rotation = 0;
            this.shading = 60;
            this.color = new Color(0x3B82F6);
        }

        Rectangle bounds() { return new Rectangle(x, y, w, h); }
    }

    /* ========================= Canvas ========================= */

    private static class RoomCanvas extends JPanel {

        private final List<FurnitureItem> items = new ArrayList<>();
        private FurnitureItem selected = null;

        private Point dragStartMouse = null;
        private Point dragStartItem = null;

        private Runnable onSelectionChanged = null;

        RoomCanvas() {
            setOpaque(false);
            setFocusable(true);
            setLayout(null);

            setBorder(new EmptyBorder(10, 10, 10, 10));

            // click/drag handling
            MouseAdapter ma = new MouseAdapter() {
                @Override public void mousePressed(MouseEvent e) {
                    requestFocusInWindow();
                    FurnitureItem hit = hitTest(e.getPoint());
                    setSelected(hit);
                    if (hit != null) {
                        dragStartMouse = e.getPoint();
                        dragStartItem = new Point(hit.x, hit.y);
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
                    selected.x = dragStartItem.x + dx;
                    selected.y = dragStartItem.y + dy;
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

            // delete key
            getInputMap(WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "deleteSelected");
            getActionMap().put("deleteSelected", new AbstractAction() {
                @Override public void actionPerformed(ActionEvent e) {
                    deleteSelected();
                }
            });
        }

        void setOnSelectionChanged(Runnable r) { this.onSelectionChanged = r; }

        FurnitureItem getSelected() { return selected; }

        void addItemFromTemplate(FurnitureTemplate t) {
            // place near center
            int cx = Math.max(40, getWidth() / 2 - t.defaultW / 2);
            int cy = Math.max(40, getHeight() / 2 - t.defaultH / 2);

            FurnitureItem it = new FurnitureItem(t.name, t.kind, cx, cy, t.defaultW, t.defaultH);
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
            // top-most item should be selected first (iterate from end)
            for (int i = items.size() - 1; i >= 0; i--) {
                FurnitureItem it = items.get(i);
                if (it.bounds().contains(p)) return it;
            }
            return null;
        }

        void setSelectedPosition(int x, int y) {
            if (selected == null) return;
            selected.x = x;
            selected.y = y;
            repaint();
            fireSelectionChanged();
        }

        void setSelectedSize(int w, int h) {
            if (selected == null) return;
            selected.w = w;
            selected.h = h;
            repaint();
            fireSelectionChanged();
        }

        void setSelectedRotation(int deg) {
            if (selected == null) return;
            selected.rotation = ((deg % 360) + 360) % 360;
            repaint();
            fireSelectionChanged();
        }

        void nudgeRotation(int delta) {
            if (selected == null) return;
            setSelectedRotation(selected.rotation + delta);
        }

        void setSelectedColor(Color c) {
            if (selected == null) return;
            selected.color = c;
            repaint();
        }

        void setSelectedShading(int v) {
            if (selected == null) return;
            selected.shading = Math.max(0, Math.min(100, v));
            repaint();
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

            // background grid (like planning canvas)
            paintGrid(g2, w, h);

            // room bounds (simple "room" rectangle)
            int pad = 46;
            int roomW = w - pad * 2;
            int roomH = h - pad * 2;

            // drop shadow
            g2.setColor(new Color(0, 0, 0, 18));
            g2.fillRoundRect(pad + 2, pad + 3, roomW, roomH, 22, 22);

            // room surface
            g2.setColor(new Color(0xF3F4F6));
            g2.fillRoundRect(pad, pad, roomW, roomH, 22, 22);

            // room border
            g2.setColor(new Color(0xD1D5DB));
            g2.setStroke(new BasicStroke(1.2f));
            g2.drawRoundRect(pad, pad, roomW, roomH, 22, 22);

            // rulers (top + left ticks)
            paintRulers(g2, w, h);

            // items
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

            // ruler borders
            g2.setColor(new Color(0xD1D5DB));
            g2.drawLine(0, 28, w, 28);
            g2.drawLine(28, 0, 28, h);
        }

        private void paintItem(Graphics2D g2, FurnitureItem it) {
            // shading applied by mixing with black depending on shading value
            float shade = it.shading / 100f;
            Color base = it.color;
            Color shaded = mix(base, Color.BLACK, 0.15f * shade);

            Rectangle r = it.bounds();

            // selection outline
            boolean isSel = (it == selected);

            // paint shape
            g2.setColor(shaded);

            if (it.kind == FurnitureKind.TABLE_ROUND) {
                g2.fillOval(r.x, r.y, r.width, r.height);
            } else {
                g2.fillRoundRect(r.x, r.y, r.width, r.height, 14, 14);
            }

            // border
            g2.setColor(new Color(0, 0, 0, 40));
            if (it.kind == FurnitureKind.TABLE_ROUND) {
                g2.drawOval(r.x, r.y, r.width, r.height);
            } else {
                g2.drawRoundRect(r.x, r.y, r.width, r.height, 14, 14);
            }

            // icon-like mark
            g2.setColor(Color.WHITE);
            g2.setFont(g2.getFont().deriveFont(Font.BOLD, 12f));
            String icon = it.kind.iconText;
            FontMetrics fm = g2.getFontMetrics();
            int tx = r.x + (r.width - fm.stringWidth(icon)) / 2;
            int ty = r.y + (r.height + fm.getAscent()) / 2 - 2;
            g2.drawString(icon, tx, ty);

            if (isSel) {
                g2.setColor(new Color(0x2563EB));
                g2.setStroke(new BasicStroke(2.0f));
                if (it.kind == FurnitureKind.TABLE_ROUND) {
                    g2.drawOval(r.x - 2, r.y - 2, r.width + 4, r.height + 4);
                } else {
                    g2.drawRoundRect(r.x - 2, r.y - 2, r.width + 4, r.height + 4, 16, 16);
                }
            }
        }

        private Color mix(Color a, Color b, float t) {
            t = Math.max(0f, Math.min(1f, t));
            int r = (int) (a.getRed() * (1 - t) + b.getRed() * t);
            int g = (int) (a.getGreen() * (1 - t) + b.getGreen() * t);
            int bl = (int) (a.getBlue() * (1 - t) + b.getBlue() * t);
            return new Color(r, g, bl);
        }
    }
}
