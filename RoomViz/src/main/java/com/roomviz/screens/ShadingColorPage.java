package com.roomviz.screens;

import com.roomviz.app.AppFrame;
import com.roomviz.ui.UiKit;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class ShadingColorPage extends JPanel {

    private final AppFrame frame;

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

    private final JButton tabGlobal = UiKit.primaryButton("Global Design");
    private final JButton tabSelected = UiKit.ghostButton("Selected Items");

    private final JButton matteBtn = UiKit.ghostButton("Matte");
    private final JButton satinBtn = UiKit.ghostButton("Satin");
    private final JButton glossBtn = UiKit.ghostButton("Gloss");

    private final JButton daylightBtn = UiKit.ghostButton("☀  Daylight");
    private final JButton warmBtn = UiKit.ghostButton("🔥  Warm");
    private final JButton coolBtn = UiKit.ghostButton("❄  Cool");
    private final JButton neutralBtn = UiKit.ghostButton("◻  Neutral");

    private final JToggleButton beforeBtn = new JToggleButton("Before");
    private final JToggleButton afterBtn = new JToggleButton("After");

    private final PreviewPanel previewPanel = new PreviewPanel();

    public ShadingColorPage(AppFrame frame) {
        this.frame = frame;

        setLayout(new BorderLayout());
        setBackground(UiKit.BG);
        setBorder(new EmptyBorder(14, 14, 14, 14));

        add(buildCardShell(), BorderLayout.CENTER);

        syncUI();
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
        close.addActionListener(e -> JOptionPane.showMessageDialog(
                this,
                "Close tools (demo).\nYou can route back or hide this panel later.",
                "Close",
                JOptionPane.INFORMATION_MESSAGE
        ));

        header.add(title, BorderLayout.WEST);
        header.add(close, BorderLayout.EAST);

        // Tabs row
        JPanel tabs = new JPanel(new GridLayout(1, 2, 10, 0));
        tabs.setOpaque(false);
        tabs.setBorder(new EmptyBorder(0, 16, 10, 16));

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

        // Scrollable content inside left panel
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

        wrap.add(header, BorderLayout.NORTH);
        wrap.add(tabs, BorderLayout.CENTER);

        JPanel topStack = new JPanel();
        topStack.setOpaque(false);
        topStack.setLayout(new BoxLayout(topStack, BoxLayout.Y_AXIS));
        topStack.add(header);
        topStack.add(tabs);
        topStack.add(hint);

        wrap.removeAll();
        wrap.add(topStack, BorderLayout.NORTH);
        wrap.add(sc, BorderLayout.CENTER);

        return wrap;
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

        beforeBtn.setFocusPainted(false);
        afterBtn.setFocusPainted(false);

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

        // Big preview body
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

        picker.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                selectedColor = picker.colorAt(e.getX(), e.getY());
                hexField.setText(toHex(selectedColor));
                syncUI();
            }
        });

        return picker;
    }

    private JComponent hexRow() {
        JPanel row = new JPanel(new BorderLayout(10, 0));
        row.setOpaque(false);

        swatch.setPreferredSize(new Dimension(34, 34));
        swatch.setBackground(selectedColor);
        swatch.setBorder(new LineBorder(UiKit.BORDER, 1, true));

        styleTextField(hexField);
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

    private JComponent quickPalettes() {
        JPanel wrap = new JPanel();
        wrap.setOpaque(false);
        wrap.setLayout(new BoxLayout(wrap, BoxLayout.Y_AXIS));

        JLabel t = new JLabel("Quick Palettes");
        t.setForeground(UiKit.MUTED);
        t.setFont(t.getFont().deriveFont(11.3f));

        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
        row.setOpaque(false);

        row.add(paletteButton(new Color(0x2563EB), new Color(0x60A5FA), new Color(0x93C5FD), new Color(0xDBEAFE))); // blue
        row.add(paletteButton(new Color(0x16A34A), new Color(0x4ADE80), new Color(0x86EFAC), new Color(0xDCFCE7))); // green
        row.add(paletteButton(new Color(0x7C3AED), new Color(0xA78BFA), new Color(0xC4B5FD), new Color(0xEDE9FE))); // purple
        row.add(paletteButton(new Color(0x111827), new Color(0x374151), new Color(0x6B7280), new Color(0xE5E7EB))); // neutral

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
        btn.setFont(btn.getFont().deriveFont(Font.BOLD, 11.5f));
        btn.setBorder(new EmptyBorder(10, 10, 10, 10));
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
        b.setBorder(new EmptyBorder(10, 12, 10, 12));
        b.setFont(b.getFont().deriveFont(Font.BOLD, 11.6f));
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

        apply.addActionListener(e -> JOptionPane.showMessageDialog(
                this,
                "Applied:\n- Target: " + (globalTab ? "Global Design" : "Selected Items") +
                        "\n- Color: " + toHex(selectedColor) +
                        "\n- Material: " + material +
                        "\n- Shading: " + shading + "%" +
                        "\n- Lighting: " + lighting,
                "Apply Changes",
                JOptionPane.INFORMATION_MESSAGE
        ));

        revert.addActionListener(e -> {
            // simple "revert to defaults"
            selectedColor = new Color(0x3B82F6);
            shading = 50;
            material = "Matte";
            lighting = "Daylight";
            afterMode = true;
            globalTab = true;
            hexField.setText("#3B82F6");
            syncUI();
        });

        row.add(apply);
        row.add(revert);

        JLabel reset = new JLabel("Reset to Default");
        reset.setForeground(new Color(0x6B7280));
        reset.setFont(reset.getFont().deriveFont(Font.PLAIN, 11.3f));
        reset.setHorizontalAlignment(SwingConstants.CENTER);
        reset.setBorder(new EmptyBorder(10, 0, 0, 0));
        reset.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        reset.addMouseListener(new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) {
                selectedColor = new Color(0x3B82F6);
                shading = 50;
                material = "Matte";
                lighting = "Daylight";
                afterMode = true;
                globalTab = true;
                hexField.setText("#3B82F6");
                syncUI();
            }
        });

        wrap.add(row);
        wrap.add(reset);
        return wrap;
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
        // tabs
        if (globalTab) {
            tabGlobal.setEnabled(false);
            tabSelected.setEnabled(true);
        } else {
            tabGlobal.setEnabled(true);
            tabSelected.setEnabled(false);
        }

        // swatch
        swatch.setBackground(selectedColor);

        // material highlight
        highlightMaterial(matteBtn, "Matte".equals(material));
        highlightMaterial(satinBtn, "Satin".equals(material));
        highlightMaterial(glossBtn, "Gloss".equals(material));

        // lighting highlight
        highlightPreset(daylightBtn, "Daylight".equals(lighting));
        highlightPreset(warmBtn, "Warm".equals(lighting));
        highlightPreset(coolBtn, "Cool".equals(lighting));
        highlightPreset(neutralBtn, "Neutral".equals(lighting));

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

    private void highlightMaterial(JButton b, boolean active) {
        if (active) {
            b.setBackground(new Color(0xEEF2FF));
            b.setForeground(new Color(0x1D4ED8));
            b.setBorder(new LineBorder(new Color(0xC7D2FE), 1, true));
            b.setOpaque(true);
        } else {
            b.setOpaque(false);
            b.setBackground(null);
            b.setForeground(new Color(0x111827));
            b.setBorder(new LineBorder(UiKit.BORDER, 1, true));
        }
    }

    private void highlightPreset(JButton b, boolean active) {
        if (active) {
            b.setBackground(new Color(0xEEF2FF));
            b.setForeground(new Color(0x1D4ED8));
            b.setBorder(new LineBorder(new Color(0xC7D2FE), 1, true));
            b.setOpaque(true);
        } else {
            b.setOpaque(false);
            b.setBackground(null);
            b.setForeground(new Color(0x111827));
            b.setBorder(new LineBorder(UiKit.BORDER, 1, true));
        }
    }

    private void styleToggle(JToggleButton b, boolean active) {
        b.setFocusPainted(false);
        b.setBorder(new EmptyBorder(6, 10, 6, 10));
        b.setFont(b.getFont().deriveFont(Font.BOLD, 11.4f));
        if (active) {
            b.setBackground(new Color(0x2563EB));
            b.setForeground(Color.WHITE);
            b.setOpaque(true);
        } else {
            b.setBackground(new Color(0xF3F4F6));
            b.setForeground(new Color(0x111827));
            b.setOpaque(true);
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

    /* ========================= Gradient Picker ========================= */

    private static class GradientPicker extends JPanel {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();

            // Horizontal hue-like gradient
            float[] stops = {0f, 0.2f, 0.4f, 0.6f, 0.8f, 1f};
            Color[] colors = {
                    new Color(0xEF4444), // red
                    new Color(0xF59E0B), // orange
                    new Color(0xEAB308), // yellow
                    new Color(0x22C55E), // green
                    new Color(0x3B82F6), // blue
                    new Color(0xA855F7)  // purple
            };
            LinearGradientPaint lg = new LinearGradientPaint(
                    0, 0, w, 0, stops, colors
            );
            g2.setPaint(lg);
            g2.fillRoundRect(0, 0, w, h, 12, 12);

            // Subtle white overlay from top to bottom (like saturation/value)
            GradientPaint vp = new GradientPaint(0, 0, new Color(255, 255, 255, 0), 0, h, new Color(255, 255, 255, 90));
            g2.setPaint(vp);
            g2.fillRoundRect(0, 0, w, h, 12, 12);

            g2.dispose();
        }

        Color colorAt(int x, int y) {
            int w = Math.max(1, getWidth());
            float t = Math.min(1f, Math.max(0f, x / (float) w));

            // Map t across the same 6-stops gradient
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

            // add slight lightening based on y
            int h = Math.max(1, getHeight());
            float vy = Math.min(1f, Math.max(0f, y / (float) h));
            float white = vy * 0.25f;

            r = (int) (r + (255 - r) * white);
            g = (int) (g + (255 - g) * white);
            bl = (int) (bl + (255 - bl) * white);

            return new Color(clamp(r), clamp(g), clamp(bl));
        }

        private int clamp(int v) { return Math.max(0, Math.min(255, v)); }
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

            // Large empty preview like screenshot
            g2.setColor(new Color(0xF3F4F6));
            g2.fillRect(0, 0, w, h);

            // subtle top divider line
            g2.setColor(new Color(0xE5E7EB));
            g2.drawLine(0, 0, w, 0);

            // draw a "room block" in center (demo visual)
            int boxW = Math.min(520, w - 120);
            int boxH = Math.min(320, h - 140);
            int x = (w - boxW) / 2;
            int y = (h - boxH) / 2;

            Color base = afterMode ? selectedColor : new Color(0xE5E7EB);

            // material affects highlight strength
            float glossBoost = "Gloss".equals(material) ? 0.22f : ("Satin".equals(material) ? 0.12f : 0.06f);

            // shading affects darkness/lightness
            float shade = shading / 100f; // 0..1
            // map 0(darker) -> -0.25, 1(lighter) -> +0.20
            float adj = (shade - 0.5f) * 0.9f;

            Color adjusted = adjust(base, adj);

            // lighting adds extra tint
            if ("Warm".equals(lighting)) adjusted = blend(adjusted, new Color(255, 170, 120), 0.18f);
            if ("Cool".equals(lighting)) adjusted = blend(adjusted, new Color(120, 180, 255), 0.18f);
            if ("Neutral".equals(lighting)) adjusted = blend(adjusted, new Color(240, 240, 240), 0.10f);

            UiKit.RoundedPanel rp = new UiKit.RoundedPanel(18, adjusted);
            rp.setBorderPaint(new Color(0xD1D5DB));
            rp.setBounds(x, y, boxW, boxH);

            // draw it manually
            g2.setColor(adjusted);
            g2.fillRoundRect(x, y, boxW, boxH, 22, 22);
            g2.setColor(new Color(0xD1D5DB));
            g2.drawRoundRect(x, y, boxW, boxH, 22, 22);

            // glossy highlight strip
            g2.setColor(new Color(255, 255, 255, (int) (255 * glossBoost)));
            g2.fillRoundRect(x + 14, y + 14, boxW - 28, 38, 18, 18);

            // label
            g2.setColor(new Color(0, 0, 0, 110));
            g2.setFont(g2.getFont().deriveFont(Font.BOLD, 13f));
            g2.drawString(afterMode ? "After Preview" : "Before Preview", x + 18, y + 70);

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
            return new Color(clamp(r), clamp(g), clamp(b));
        }

        private Color blend(Color a, Color b, float t) {
            int r = (int) (a.getRed() + (b.getRed() - a.getRed()) * t);
            int g = (int) (a.getGreen() + (b.getGreen() - a.getGreen()) * t);
            int bl = (int) (a.getBlue() + (b.getBlue() - a.getBlue()) * t);
            return new Color(clamp(r), clamp(g), clamp(bl));
        }

        private int clamp(int v) { return Math.max(0, Math.min(255, v)); }
    }
}
