package com.roomviz.screens;

import com.roomviz.app.AppFrame;
import com.roomviz.app.Router;
import com.roomviz.app.ScreenKeys;
import com.roomviz.data.AppState;

import com.roomviz.model.Design;
import com.roomviz.model.FurnitureItem;
import com.roomviz.model.RoomSpec;
import com.roomviz.ui.UiKit;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;

import java.awt.event.*;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 3D Visual Page – REAL simple raster renderer (Java2D).
 *
 * - Reads current Design from AppState
 * - 2D rectangles -> 3D cuboids
 * - Perspective projection + raster fillPolygon
 * - Painter's algorithm (depth sort)
 * - Orbit (drag) + zoom (wheel)
 * - Uses FurnitureItem colorHex + shadingPercent
 */
public class Visual3DPage extends JPanel {

    @SuppressWarnings("unused")
    private final AppFrame frame; // kept for consistency with other pages
    private final Router router;
    private final AppState appState;

=======
import java.awt.image.BufferedImage;

/**
 * 3D Visual Page (UI scaffold) – matches screenshot layout.
 */
public class Visual3DPage extends JPanel {

    private final AppFrame frame;
    private final Router router;

    // ✅ Step 1: shared state reference (for later: read current design + items)
    private final AppState appState;

    private BufferedImage bg;

    private final JLabel toast = new JLabel("3D view updated");
    private final JPopupMenu lightingMenu = new JPopupMenu();

    private String lightingPreset = "Day";

    private final RendererPanel renderer;

    // ✅ Step 1: updated constructor signature

    public Visual3DPage(AppFrame frame, Router router, AppState appState) {
        this.frame = frame;
        this.router = router;
        this.appState = appState;

        setLayout(new BorderLayout());
        setBackground(UiKit.BG);
        setBorder(new EmptyBorder(14, 14, 14, 14));

        renderer = new RendererPanel();

        add(buildTopBar(), BorderLayout.NORTH);
        add(buildMain(), BorderLayout.CENTER);

        buildLightingMenu();

        tryLoadBackground();

        add(buildTopBar(), BorderLayout.NORTH);
        add(buildMain(), BorderLayout.CENTER);
    }

    private void tryLoadBackground() {
        try {
            var in = getClass().getResourceAsStream("/assets/3d-room.jpg");
            if (in != null)
                bg = ImageIO.read(in);
        } catch (Exception ignored) {
            bg = null;
        }

    }

    /* ========================== TOP BAR ========================== */

    private JComponent buildTopBar() {
        UiKit.RoundedPanel bar = new UiKit.RoundedPanel(16, new Color(0x111827));
        bar.setBorderPaint(new Color(0x0B1220));
        bar.setLayout(new BorderLayout(10, 0));
        bar.setBorder(new EmptyBorder(10, 12, 10, 12));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        left.setOpaque(false);

        JButton back = darkButton("←  Back to 2D");

        back.addActionListener(e -> router.show(ScreenKeys.PLANNER_2D));

        back.addActionListener(e -> {
            // ✅ Correct navigation for your project
            router.show(ScreenKeys.PLANNER_2D);
        });

        JLabel saved = pill("✓  Saved", new Color(0x064E3B), new Color(0x34D399));

        left.add(back);
        left.add(saved);

        JLabel title = new JLabel(getDesignTitleForHeader());

        JLabel title = new JLabel("Living Room Design – 3D View");

        title.setForeground(new Color(0xE5E7EB));
        title.setFont(title.getFont().deriveFont(Font.BOLD, 12.8f));
        title.setHorizontalAlignment(SwingConstants.CENTER);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        right.setOpaque(false);

        JButton snapshot = primaryDarkButton("📷  Snapshot");

        snapshot.addActionListener(e -> renderer.exportSnapshotDialog());

        snapshot.addActionListener(e -> JOptionPane.showMessageDialog(
                this,
                "Snapshot saved (demo).\nHook this to export a rendered image later.",
                "Snapshot",
                JOptionPane.INFORMATION_MESSAGE));

        JButton present = darkButton("▣  Presentation Mode");
        present.addActionListener(e -> JOptionPane.showMessageDialog(
                this,

                "Presentation Mode (demo).\nTip: extend by hiding side panels + going fullscreen.",

                "Presentation Mode (demo).\nYou can later toggle fullscreen + hide panels.",

                "Presentation Mode",
                JOptionPane.INFORMATION_MESSAGE));

        right.add(snapshot);
        right.add(present);

        bar.add(left, BorderLayout.WEST);
        bar.add(title, BorderLayout.CENTER);
        bar.add(right, BorderLayout.EAST);

        return bar;
    }

    private String getDesignTitleForHeader() {
        Design d = appState.getCurrentDesignOrNull();
        String name = (d == null || d.getName() == null || d.getName().isBlank()) ? "Untitled Design" : d.getName();
        return name + " – 3D View";
    }

    private JButton darkButton(String text) {
        JButton b = new JButton(text);
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setForeground(new Color(0xE5E7EB));

        b.setBackground(new Color(0x1F2937));
        b.setBorder(new EmptyBorder(8, 12, 8, 12));

        b.setBackground(new Color(0x0B1220));
        b.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(255, 255, 255, 18), 1, true),
                new EmptyBorder(8, 12, 8, 12)));

        return b;
    }

    private JButton primaryDarkButton(String text) {
        JButton b = new JButton(text);
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setForeground(Color.WHITE);
        b.setBackground(new Color(0x2563EB));

        b.setBorder(new EmptyBorder(8, 12, 8, 12));

        b.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(255, 255, 255, 18), 1, true),
                new EmptyBorder(8, 12, 8, 12)));

        return b;
    }

    private JLabel pill(String text, Color bg, Color fg) {
        JLabel l = new JLabel(text);
        l.setOpaque(true);
        l.setBackground(bg);
        l.setForeground(fg);

        l.setFont(l.getFont().deriveFont(Font.BOLD, 11.5f));
        l.setBorder(new EmptyBorder(6, 10, 6, 10));
        return l;
    }

    /* ========================== MAIN LAYOUT ========================== */

    private JComponent buildMain() {
        JPanel wrap = new JPanel(new BorderLayout(12, 12));
        wrap.setOpaque(false);

        wrap.add(buildViewDock(), BorderLayout.WEST);

        UiKit.RoundedPanel canvasCard = new UiKit.RoundedPanel(18, Color.BLACK);
        canvasCard.setBorderPaint(new Color(255, 255, 255, 18));
        canvasCard.setLayout(new BorderLayout());
        canvasCard.add(renderer, BorderLayout.CENTER);
        wrap.add(canvasCard, BorderLayout.CENTER);

        wrap.add(buildRightPanel(), BorderLayout.EAST);

        return wrap;
    }

    private JComponent buildRightPanel() {
        JPanel right = new JPanel();
        right.setOpaque(false);
        right.setLayout(new BoxLayout(right, BoxLayout.Y_AXIS));
        right.setPreferredSize(new Dimension(260, 0));

        UiKit.RoundedPanel lightingCard = new UiKit.RoundedPanel(16, new Color(0x111827));
        lightingCard.setBorderPaint(new Color(255, 255, 255, 22));
        lightingCard.setLayout(new BorderLayout());
        lightingCard.setBorder(new EmptyBorder(12, 12, 12, 12));

        JLabel t = new JLabel("Lighting Preset");
        t.setForeground(new Color(0xD1D5DB));
        t.setFont(t.getFont().deriveFont(Font.BOLD, 11.2f));

        JButton dropdown = darkButton(" " + lightingPreset + "  ▾");
        dropdown.setHorizontalAlignment(SwingConstants.LEFT);
        dropdown.addActionListener(e -> showLightingMenu(dropdown));

        lightingCard.add(t, BorderLayout.NORTH);
        lightingCard.add(dropdown, BorderLayout.CENTER);

        toast.setOpaque(true);
        toast.setBackground(new Color(0x111827));
        toast.setForeground(new Color(0xE5E7EB));
        toast.setBorder(new EmptyBorder(10, 12, 10, 12));
        toast.setVisible(false);
        toast.setAlignmentX(Component.LEFT_ALIGNMENT);

        right.add(lightingCard);
        right.add(Box.createVerticalStrut(12));
        right.add(toast);
        right.add(Box.createVerticalGlue());

        return right;
    }

    private JComponent buildViewDock() {
        UiKit.RoundedPanel dock = new UiKit.RoundedPanel(16, new Color(0x111827));
        dock.setBorderPaint(new Color(255, 255, 255, 22));
        dock.setLayout(new BorderLayout());
        dock.setBorder(new EmptyBorder(10, 10, 10, 10));
        dock.setPreferredSize(new Dimension(96, 0));

        JLabel title = new JLabel("View");
        title.setForeground(new Color(0xD1D5DB));
        title.setFont(title.getFont().deriveFont(Font.BOLD, 11.0f));

        l.setBorder(new EmptyBorder(6, 10, 6, 10));
        l.setFont(l.getFont().deriveFont(Font.BOLD, 11.5f));
        return l;
    }

    /* ========================== MAIN AREA ========================== */

    private JComponent buildMain() {
        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setOpaque(false);
        wrap.setBorder(new EmptyBorder(12, 0, 0, 0));

        VisualPanel visual = new VisualPanel();
        visual.setLayout(null);

        JComponent controlsDock = buildViewControlsDock();
        controlsDock.setBounds(18, 180, 76, 220);
        visual.add(controlsDock);

        JLabel tag1 = tag("4.2m", new Color(0x2563EB));
        tag1.setBounds(220, 110, 56, 28);
        visual.add(tag1);

        JLabel tag2 = tag("Sofa Set", new Color(0x16A34A));
        tag2.setBounds(585, 295, 70, 28);
        visual.add(tag2);

        JButton lightingBtn = darkOverlayButton("⚡  " + lightingPreset);
        lightingBtn.setBounds(700, 26, 150, 38);
        lightingBtn.addActionListener(e -> showLightingMenu(lightingBtn));
        visual.add(lightingBtn);

        toast.setOpaque(true);
        toast.setBackground(new Color(0x1D4ED8));
        toast.setForeground(Color.WHITE);
        toast.setFont(toast.getFont().deriveFont(Font.BOLD, 11.6f));
        toast.setBorder(new EmptyBorder(8, 12, 8, 12));
        toast.setVisible(false);
        toast.setBounds(735, 72, 150, 34);
        visual.add(toast);

        buildLightingMenu();

        wrap.add(visual, BorderLayout.CENTER);
        return wrap;
    }

    private JLabel tag(String text, Color bg) {
        JLabel l = new JLabel(text);
        l.setOpaque(true);
        l.setBackground(bg);
        l.setForeground(Color.WHITE);
        l.setBorder(new EmptyBorder(5, 10, 5, 10));
        l.setFont(l.getFont().deriveFont(Font.BOLD, 11.5f));
        l.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return l;
    }

    private JButton darkOverlayButton(String text) {
        JButton b = new JButton(text);
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setForeground(new Color(0xE5E7EB));
        b.setBackground(new Color(17, 24, 39, 220));
        b.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(255, 255, 255, 22), 1, true),
                new EmptyBorder(8, 12, 8, 12)));
        return b;
    }

    /* ========================== Left Dock ========================== */

    private JComponent buildViewControlsDock() {
        UiKit.RoundedPanel dock = new UiKit.RoundedPanel(16, new Color(17, 24, 39, 220));
        dock.setBorderPaint(new Color(255, 255, 255, 22));
        dock.setLayout(new BorderLayout());
        dock.setBorder(new EmptyBorder(10, 10, 10, 10));

        JLabel title = new JLabel("View Controls");
        title.setForeground(new Color(0xD1D5DB));
        title.setFont(title.getFont().deriveFont(Font.BOLD, 10.5f));

        title.setBorder(new EmptyBorder(0, 2, 8, 0));

        JPanel btns = new JPanel(new GridLayout(4, 1, 0, 10));
        btns.setOpaque(false);

        JButton reset = iconDockButton("⟳", "Reset camera");
        reset.addActionListener(e -> {
            renderer.resetCamera();
            showToast();
        });

        JButton center = iconDockButton("✥", "Center scene");
        center.addActionListener(e -> {
            renderer.centerScene();
            showToast();
        });

        JButton zoom = iconDockButton("🔍", "Zoom to fit");
        zoom.addActionListener(e -> {
            renderer.zoomToFit();
            showToast();
        });

        JButton home = iconDockButton("⌂", "Default view");
        home.addActionListener(e -> {
            renderer.resetCamera();
            renderer.zoomToFit();
            showToast();
        });

        btns.add(reset);
        btns.add(center);
        btns.add(zoom);
        btns.add(home);

        btns.add(iconDockButton("⟳"));
        btns.add(iconDockButton("✥"));
        btns.add(iconDockButton("🔍"));
        btns.add(iconDockButton("⌂"));

        dock.add(title, BorderLayout.NORTH);
        dock.add(btns, BorderLayout.CENTER);
        return dock;
    }

    private JButton iconDockButton(String icon, String tooltip) {
        JButton b = new JButton(icon);
        b.setToolTipText(tooltip);
=======

    private JButton iconDockButton(String icon) {
        JButton b = new JButton(icon);

        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setForeground(Color.WHITE);
        b.setBackground(new Color(0x2563EB));
        b.setBorder(new EmptyBorder(10, 0, 10, 0));
        return b;
    }

    /* ========================== Lighting Dropdown ========================== */

    private void buildLightingMenu() {
        lightingMenu.setBorder(new LineBorder(new Color(0, 0, 0, 30), 1, true));
        lightingMenu.setBackground(new Color(17, 24, 39, 245));

        lightingMenu.add(menuItem("Day", "☀", () -> setLighting("Day")));
        lightingMenu.add(menuItem("Night", "☾", () -> setLighting("Night")));
        lightingMenu.add(menuItem("Sunset", "☀", () -> setLighting("Sunset")));
    }

    private JMenuItem menuItem(String label, String icon, Runnable action) {
        JMenuItem item = new JMenuItem(icon + "  " + label);
        item.setForeground(new Color(0xE5E7EB));
        item.setBackground(new Color(17, 24, 39, 245));
        item.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        item.setBorder(new EmptyBorder(10, 12, 10, 12));
        item.addActionListener(e -> action.run());
        return item;
    }

    private void showLightingMenu(Component anchor) {
        lightingMenu.show(anchor, 0, anchor.getHeight() + 6);
    }

    private void setLighting(String preset) {
        lightingPreset = preset;

        renderer.setLightingPreset(preset);

        showToast();
        repaint();
    }

    private void showToast() {
        toast.setVisible(true);

        Timer t = new Timer(1800, e -> toast.setVisible(false));

        Timer t = new Timer(2000, e -> toast.setVisible(false));

        t.setRepeats(false);
        t.start();
    }

    /* ========================== RENDERER PANEL ========================== */

    private class RendererPanel extends JPanel {

        // Orbit camera params
        private double yaw = Math.toRadians(35);
        private double pitch = Math.toRadians(-22);
        private double dist = 820;

        private Vec3 sceneCenter = new Vec3(0, 0, 0);

        private Point lastMouse = null;

        private String preset = "Day";

        RendererPanel() {
            setOpaque(true);
            setBackground(Color.BLACK);
            setFocusable(true);

            MouseAdapter ma = new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    requestFocusInWindow();
                    lastMouse = e.getPoint();
                }

                @Override
                public void mouseDragged(MouseEvent e) {
                    if (lastMouse == null)
                        return;
                    int dx = e.getX() - lastMouse.x;
                    int dy = e.getY() - lastMouse.y;

                    yaw += dx * 0.0085;
                    pitch += dy * 0.0085;
                    pitch = clamp(pitch, Math.toRadians(-80), Math.toRadians(10));

                    lastMouse = e.getPoint();
                    repaint();
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    lastMouse = null;
                }
            };
            addMouseListener(ma);
            addMouseMotionListener(ma);

            addMouseWheelListener(e -> {
                double delta = e.getPreciseWheelRotation();
                dist *= (1.0 + delta * 0.10);
                dist = clamp(dist, 220, 2600);
                repaint();
            });

            getInputMap(WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_R, 0), "resetCam");
            getActionMap().put("resetCam", new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    resetCamera();
                    showToast();
                }
            });

            getInputMap(WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_F, 0), "fitCam");
            getActionMap().put("fitCam", new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    zoomToFit();
                    showToast();
                }
            });
        }

        void setLightingPreset(String p) {
            preset = (p == null) ? "Day" : p;
            repaint();
        }

        void resetCamera() {
            yaw = Math.toRadians(35);
            pitch = Math.toRadians(-22);
            dist = 820;
            repaint();
        }

        void centerScene() {
            computeSceneCenter();
            repaint();
        }

        void zoomToFit() {
            Bounds b = computeSceneBounds();
            double maxSpan = Math.max(b.w, b.d);
            if (maxSpan <= 1)
                maxSpan = 600;
            dist = clamp(maxSpan * 1.25, 260, 2600);
            repaint();
        }

        void exportSnapshotDialog() {
            try {
                JFileChooser fc = new JFileChooser();
                fc.setDialogTitle("Export Snapshot (PNG)");
                int res = fc.showSaveDialog(this);
                if (res != JFileChooser.APPROVE_OPTION) return;

                java.io.File f = fc.getSelectedFile();
                String name = f.getName().toLowerCase();
                if (!name.endsWith(".png")) f = new java.io.File(f.getParentFile(), f.getName() + ".png");

                BufferedImage img = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_ARGB);
                Graphics2D g2 = img.createGraphics();
                paint(g2);
                g2.dispose();

                ImageIO.write(img, "png", f);

                JOptionPane.showMessageDialog(this,
                        "Snapshot saved:\n" + f.getAbsolutePath(),
                        "Saved",
                        JOptionPane.INFORMATION_MESSAGE
                );
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this,
                        "Snapshot failed:\n" + ex.getMessage(),
                        "Error",
                        JOptionPane.ERROR_MESSAGE
                );
            }

    /* ========================== Visual Panel ========================== */

    private class VisualPanel extends JPanel {
        VisualPanel() {
            setOpaque(true);
            setBackground(Color.BLACK);
            setBorder(BorderFactory.createCompoundBorder(
                    new LineBorder(new Color(0xD1D5DB), 1, true),
                    new EmptyBorder(0, 0, 0, 0)
            ));

        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);



            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();


            paintBackground(g2, w, h);

            List<Face> faces = buildSceneFaces();
            drawFaces(g2, faces, w, h);

            paintHud(g2, w, h);

            g2.dispose();
        }

        private void paintBackground(Graphics2D g2, int w, int h) {
            Color top = new Color(0x0B1220);
            Color bot = new Color(0x05070D);

            if ("Night".equalsIgnoreCase(preset)) {
                top = new Color(0x050A14);
                bot = new Color(0x02040A);
            } else if ("Sunset".equalsIgnoreCase(preset)) {
                top = new Color(0x1A0F14);
                bot = new Color(0x05070D);
            }

            GradientPaint gp = new GradientPaint(0, 0, top, 0, h, bot);
            g2.setPaint(gp);
            g2.fillRect(0, 0, w, h);

            if ("Night".equalsIgnoreCase(preset)) {
                g2.setColor(new Color(255, 255, 255, 40));
                for (int i = 0; i < 60; i++) {
                    int x = (i * 73) % Math.max(1, w);
                    int y = (i * 91) % Math.max(1, h / 2);
                    g2.fillRect(x, y, 1, 1);
                }
            }
        }

        private void paintHud(Graphics2D g2, int w, int h) {
            g2.setColor(new Color(255, 255, 255, 160));
            g2.setFont(g2.getFont().deriveFont(Font.PLAIN, 11.5f));
            g2.drawString("Drag: orbit   Wheel: zoom   R: reset   F: fit", 16, h - 16);
        }

        private List<Face> buildSceneFaces() {
            computeSceneCenter();
            List<Face> faces = new ArrayList<>();

            Bounds b = computeSceneBounds();

            Design d = appState.getCurrentDesignOrNull();
            RoomSpec spec = (d == null) ? null : d.getRoomSpec();

            double roomW = b.w;
            double roomD = b.d;

            if (spec != null && spec.getWidth() > 0 && spec.getLength() > 0) {
                double ratio = spec.getWidth() / Math.max(0.0001, spec.getLength());
                if (ratio > 0.2 && ratio < 5) {
                    if (roomW >= roomD) roomD = roomW / ratio;
                    else roomW = roomD * ratio;
                }
            }

            double floorY = 0;
            double wallH = Math.max(260, Math.min(520, Math.max(roomW, roomD) * 0.45));

            double x0 = sceneCenter.x - roomW / 2;
            double x1 = sceneCenter.x + roomW / 2;
            double z0 = sceneCenter.z - roomD / 2;
            double z1 = sceneCenter.z + roomD / 2;

            Color floorCol = new Color(40, 48, 70, 220);
            Color wallCol = new Color(24, 30, 46, 210);

            if ("Sunset".equalsIgnoreCase(preset)) {
                floorCol = new Color(55, 42, 46, 220);
                wallCol = new Color(36, 26, 36, 210);
            } else if ("Night".equalsIgnoreCase(preset)) {
                floorCol = new Color(18, 22, 34, 220);
                wallCol = new Color(12, 16, 26, 210);
            }

            faces.add(faceQuad(
                    new Vec3(x0, floorY, z0),
                    new Vec3(x1, floorY, z0),
                    new Vec3(x1, floorY, z1),
                    new Vec3(x0, floorY, z1),
                    floorCol
            ));

            faces.add(faceQuad(
                    new Vec3(x0, floorY, z0),
                    new Vec3(x1, floorY, z0),
                    new Vec3(x1, floorY + wallH, z0),
                    new Vec3(x0, floorY + wallH, z0),
                    wallCol
            ));

            faces.add(faceQuad(
                    new Vec3(x0, floorY, z0),
                    new Vec3(x0, floorY, z1),
                    new Vec3(x0, floorY + wallH, z1),
                    new Vec3(x0, floorY + wallH, z0),
                    wallCol.darker()
            ));

            List<FurnitureItem> items = (d == null) ? List.of() : safeItems(d.getItems());
            for (FurnitureItem it : items) {
                faces.addAll(buildFurnitureFaces(it));
            }

            return faces;
        }

        private List<FurnitureItem> safeItems(List<FurnitureItem> items) {
            return (items == null) ? List.of() : items;
        }

        private List<Face> buildFurnitureFaces(FurnitureItem it) {
            List<Face> faces = new ArrayList<>();

            double x = it.getX();
            double z = it.getY();
            double w = Math.max(10, it.getW());
            double d = Math.max(10, it.getH());

            double h = 60;
            String kind = (it.getKind() == null) ? "" : it.getKind().toUpperCase();
            if (kind.contains("TABLE")) h = 52;
            if (kind.contains("CHAIR")) h = 72;

            if (it.getId() != null && it.getId().equals(appState.getSelectedItemId())) {
                h *= 1.08;
            }

            Color base = parseHex(it.getColorHex(), new Color(0x3B82F6));
            double shadeMul = 0.60 + (clamp(it.getShadingPercent(), 0, 100) / 100.0) * 0.70;
            base = applyBrightness(base, shadeMul);

            String material = (it.getMaterial() == null) ? "" : it.getMaterial().toLowerCase();
            double gloss = 0.0;
            if (material.contains("gloss")) gloss = 0.18;
            else if (material.contains("satin")) gloss = 0.10;

            Vec3 c = new Vec3(x + w / 2, 0, z + d / 2);

            double rx = w / 2;
            double rz = d / 2;
            double rot = Math.toRadians(it.getRotation());

            Vec3 p000 = rotY(new Vec3(-rx, 0, -rz), rot).add(c);
            Vec3 p100 = rotY(new Vec3(+rx, 0, -rz), rot).add(c);
            Vec3 p110 = rotY(new Vec3(+rx, 0, +rz), rot).add(c);
            Vec3 p010 = rotY(new Vec3(-rx, 0, +rz), rot).add(c);

            Vec3 p001 = rotY(new Vec3(-rx, h, -rz), rot).add(c);
            Vec3 p101 = rotY(new Vec3(+rx, h, -rz), rot).add(c);
            Vec3 p111 = rotY(new Vec3(+rx, h, +rz), rot).add(c);
            Vec3 p011 = rotY(new Vec3(-rx, h, +rz), rot).add(c);

            Vec3 lightDir = new Vec3(-0.35, 0.85, -0.35);
            if ("Night".equalsIgnoreCase(preset)) lightDir = new Vec3(-0.25, 0.60, -0.20);
            if ("Sunset".equalsIgnoreCase(preset)) lightDir = new Vec3(0.55, 0.75, -0.15);

            faces.add(faceQuadWithLighting(p001, p101, p111, p011, base, lightDir, 1.00 + gloss));
            faces.add(faceQuadWithLighting(p000, p010, p011, p001, base, lightDir, 0.72));
            faces.add(faceQuadWithLighting(p100, p101, p111, p110, base, lightDir, 0.80));
            faces.add(faceQuadWithLighting(p010, p110, p111, p011, base, lightDir, 0.86));
            faces.add(faceQuadWithLighting(p000, p100, p101, p001, base, lightDir, 0.76));

            return faces;
        }

        private Face faceQuad(Vec3 a, Vec3 b, Vec3 c, Vec3 d, Color col) {
            return new Face(List.of(a, b, c, d), col, 0);
        }

        private Face faceQuadWithLighting(Vec3 a, Vec3 b, Vec3 c, Vec3 d, Color base, Vec3 lightDir, double baseMul) {
            Vec3 u = new Vec3(b.x - a.x, b.y - a.y, b.z - a.z);
            Vec3 v = new Vec3(d.x - a.x, d.y - a.y, d.z - a.z);
            Vec3 n = cross(u, v);

            double nLen = Math.sqrt(n.x * n.x + n.y * n.y + n.z * n.z);
            if (nLen > 0) n = new Vec3(n.x / nLen, n.y / nLen, n.z / nLen);

            double lLen = Math.sqrt(lightDir.x * lightDir.x + lightDir.y * lightDir.y + lightDir.z * lightDir.z);
            Vec3 ld = lightDir;
            if (lLen > 0) ld = new Vec3(lightDir.x / lLen, lightDir.y / lLen, lightDir.z / lLen);

            double dot = clamp(n.x * ld.x + n.y * ld.y + n.z * ld.z, -1, 1);
            double lit = 0.65 + Math.max(0, dot) * 0.55;

            if ("Night".equalsIgnoreCase(preset)) lit *= 0.82;
            if ("Sunset".equalsIgnoreCase(preset)) lit *= 0.94;

            Color col = applyBrightness(base, lit * baseMul);
            return new Face(List.of(a, b, c, d), col, 0);
        }

        private void drawFaces(Graphics2D g2, List<Face> faces, int w, int h) {
            List<Face> projected = new ArrayList<>(faces.size());

            for (Face f : faces) {
                List<Vec3> camPts = new ArrayList<>(f.pts.size());
                double depthSum = 0;
                boolean anyVisible = false;

                for (Vec3 p : f.pts) {
                    Vec3 cam = worldToCamera(p);
                    camPts.add(cam);
                    depthSum += cam.z;
                    if (cam.z > 5) anyVisible = true;
                }

                if (!anyVisible) continue;

                projected.add(new Face(camPts, f.color, depthSum / f.pts.size()));
            }

            projected.sort(Comparator.comparingDouble(a -> -a.avgDepth));

            double fov = 520;
            double cx = w * 0.5;
            double cy = h * 0.54;

            for (Face f : projected) {
                Path2D path = new Path2D.Double();
                boolean started = false;
                boolean clipped = false;

                for (Vec3 p : f.pts) {
                    if (p.z <= 5) { clipped = true; break; }
                    double sx = cx + (p.x / p.z) * fov;
                    double sy = cy - (p.y / p.z) * fov;

                    if (!started) { path.moveTo(sx, sy); started = true; }
                    else { path.lineTo(sx, sy); }
                }

                if (clipped || !started) continue;
                path.closePath();

                g2.setColor(f.color);
                g2.fill(path);

                g2.setColor(new Color(0, 0, 0, 80));
                g2.setStroke(new BasicStroke(1f));
                g2.draw(path);
            }
        }

        private Vec3 worldToCamera(Vec3 world) {
            Vec3 p = world.sub(sceneCenter);
            p = rotateY(p, yaw);
            p = rotateX(p, pitch);
            return new Vec3(p.x, p.y, p.z + dist);
        }

        private void computeSceneCenter() {
            Bounds b = computeSceneBounds();
            sceneCenter = new Vec3(b.cx, 0, b.cz);
        }

        private Bounds computeSceneBounds() {
            Design d = appState.getCurrentDesignOrNull();
            List<FurnitureItem> items = (d == null) ? List.of() : safeItems(d.getItems());

            if (items.isEmpty()) {
                return new Bounds(-300, -260, 600, 520);
            }

            double minX = Double.POSITIVE_INFINITY;
            double minZ = Double.POSITIVE_INFINITY;
            double maxX = Double.NEGATIVE_INFINITY;
            double maxZ = Double.NEGATIVE_INFINITY;

            for (FurnitureItem it : items) {
                double x0 = it.getX();
                double z0 = it.getY();
                double x1 = x0 + Math.max(10, it.getW());
                double z1 = z0 + Math.max(10, it.getH());

                minX = Math.min(minX, Math.min(x0, x1));
                maxX = Math.max(maxX, Math.max(x0, x1));
                minZ = Math.min(minZ, Math.min(z0, z1));
                maxZ = Math.max(maxZ, Math.max(z0, z1));
            }

            double pad = 180;
            minX -= pad; maxX += pad;
            minZ -= pad; maxZ += pad;

            double w = Math.max(520, maxX - minX);
            double dpth = Math.max(520, maxZ - minZ);

            double cx = (minX + maxX) / 2.0;
            double cz = (minZ + maxZ) / 2.0;

            minX = cx - w / 2.0;
            minZ = cz - dpth / 2.0;

            return new Bounds(minX, minZ, w, dpth);
        }

        /* ===== Math helpers ===== */

        private Vec3 cross(Vec3 a, Vec3 b) {
            return new Vec3(
                    a.y * b.z - a.z * b.y,
                    a.z * b.x - a.x * b.z,
                    a.x * b.y - a.y * b.x
            );
        }

        private Vec3 rotY(Vec3 v, double rad) {
            double c = Math.cos(rad);
            double s = Math.sin(rad);
            return new Vec3(v.x * c + v.z * s, v.y, -v.x * s + v.z * c);
        }

        private Vec3 rotateY(Vec3 v, double rad) {
            double c = Math.cos(rad);
            double s = Math.sin(rad);
            return new Vec3(v.x * c + v.z * s, v.y, -v.x * s + v.z * c);
        }

        private Vec3 rotateX(Vec3 v, double rad) {
            double c = Math.cos(rad);
            double s = Math.sin(rad);
            return new Vec3(v.x, v.y * c - v.z * s, v.y * s + v.z * c);
        }

        private Color parseHex(String hex, Color fallback) {
            try {
                if (hex == null) return fallback;
                String h = hex.trim();
                if (!h.startsWith("#")) h = "#" + h;
                return Color.decode(h);
            } catch (Exception e) {
                return fallback;
            }
        }

        private Color applyBrightness(Color c, double mul) {
            int r = clampInt(Math.round((float) (c.getRed() * mul)), 0, 255);
            int g = clampInt(Math.round((float) (c.getGreen() * mul)), 0, 255);
            int b = clampInt(Math.round((float) (c.getBlue() * mul)), 0, 255);
            return new Color(r, g, b, c.getAlpha());
        }

        private double clamp(double v, double lo, double hi) {
            return Math.max(lo, Math.min(hi, v));
        }

        private double clamp(double v, int lo, int hi) {
            return Math.max(lo, Math.min(hi, v));
        }

        private int clampInt(long v, long lo, long hi) {
            long clamped = Math.max(lo, Math.min(hi, v));
            return (int) clamped;
        }

        private static class Bounds {
            final double x, z, w, d, cx, cz;
            Bounds(double x, double z, double w, double d) {
                this.x = x;
                this.z = z;
                this.w = w;
                this.d = d;
                this.cx = x + w / 2.0;
                this.cz = z + d / 2.0;
            }
        }
    }

    /* ========================== Tiny 3D types ========================== */

    private static class Vec3 {
        final double x, y, z;
        Vec3(double x, double y, double z) { this.x = x; this.y = y; this.z = z; }
        Vec3 add(Vec3 o) { return new Vec3(x + o.x, y + o.y, z + o.z); }
        Vec3 sub(Vec3 o) { return new Vec3(x - o.x, y - o.y, z - o.z); }
    }

    private static class Face {
        final List<Vec3> pts;
        final Color color;
        final double avgDepth;
        Face(List<Vec3> pts, Color color, double avgDepth) {
            this.pts = pts;
            this.color = color;
            this.avgDepth = avgDepth;

            if (bg != null) {
                double sx = w / (double) bg.getWidth();
                double sy = h / (double) bg.getHeight();
                double s = Math.max(sx, sy);
                int dw = (int) (bg.getWidth() * s);
                int dh = (int) (bg.getHeight() * s);
                int x = (w - dw) / 2;
                int y = (h - dh) / 2;
                g2.drawImage(bg, x, y, dw, dh, null);
            } else {
                GradientPaint gp = new GradientPaint(0, 0, new Color(0x111827), 0, h, new Color(0x0B1220));
                g2.setPaint(gp);
                g2.fillRect(0, 0, w, h);

                g2.setColor(new Color(255, 255, 255, 160));
                g2.setFont(g2.getFont().deriveFont(Font.BOLD, 18f));
                g2.drawString("3D View (UI Scaffold)", 30, 48);

                g2.setColor(new Color(255, 255, 255, 110));
                g2.setFont(g2.getFont().deriveFont(Font.PLAIN, 12.5f));
                g2.drawString("Later: connect this to a real renderer.", 30, 72);
            }

            applyLightingTint(g2, w, h);
            g2.dispose();
        }

        private void applyLightingTint(Graphics2D g2, int w, int h) {
            if ("Day".equalsIgnoreCase(lightingPreset)) return;

            if ("Night".equalsIgnoreCase(lightingPreset)) {
                g2.setColor(new Color(20, 30, 70, 85));
                g2.fillRect(0, 0, w, h);
            } else if ("Sunset".equalsIgnoreCase(lightingPreset)) {
                g2.setColor(new Color(255, 120, 60, 70));
                g2.fillRect(0, 0, w, h);
            }

        }
    }
}
