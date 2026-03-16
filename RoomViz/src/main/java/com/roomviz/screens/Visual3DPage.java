// (FULL FILE) — paste this entire file exactly as-is:
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
import java.util.Locale;

/**
 * 3D Visual Page – simple Java2D raster renderer.
 *
 * ✅ Step 1 enforcement:
 * - NO silent auto-create designs.
 * - If no design is selected -> show empty state:
 *   "Select or create a design first" + buttons Library / New Design.
 *
 * ✅ Other (existing) fixes preserved:
 * - Safe getters for Design.layoutX/layoutY/layoutWidth/layoutHeight (handles primitive double OR Double)
 * - Safe getters for Furniture rotation (getRotation / getRotationDeg / etc)
 * - Converts furniture (canvas coords) -> room-local coords for correct 3D placement
 * - Room size prefers layout bounds (or fallback)
 *
 * ✅ FIX (L-Shape support):
 * - 3D room respects L-Shape cut-out
 * - 3D floor becomes 2 quads for L-Shape, and walls follow L perimeter
 * - Rectangle rooms get full perimeter walls (4 sides)
 *
 * ✅ FIX (Resize stability):
 * - REMOVED auto zoomToFit() spam during resize (causes drift/jump)
 * - Uses a constant lens FOV angle; projection uses derived focal length
 * - Optional: debounced fit can be triggered manually with Fit button / key
 */
public class Visual3DPage extends JPanel {

    @SuppressWarnings("unused")
    private final AppFrame frame;
    private final Router router;
    private final AppState appState;

    private final JLabel toast = new JLabel("3D view updated");
    private final JPopupMenu lightingMenu = new JPopupMenu();

    private String lightingPreset = "Day";
    private JButton lightingDropdownBtn;

    private final RendererPanel renderer;

    public Visual3DPage(AppFrame frame, Router router, AppState appState) {
        this.frame = frame;
        this.router = router;
        this.appState = appState;

        setLayout(new BorderLayout());
        setBackground(UiKit.BG);
        setBorder(new EmptyBorder(14, 14, 14, 14));

        renderer = new RendererPanel();

        // ✅ Build menu once (safe even if design is not selected yet)
        buildLightingMenu();

        // ✅ Build the correct UI for current state (design selected or not)
        rebuildUI();

        // ✅ Refresh on navigation (CRITICAL - this page instance is created once in ShellScreen)
        try {
            if (router != null) {
                router.addListener(key -> {
                    if (ScreenKeys.VIEW_3D.equals(key)) {
                        rebuildUI();
                    }
                });
            }
        } catch (Throwable ignored) { }
    }

    /** ✅ Rebuild screen depending on whether a design is selected */
    private void rebuildUI() {
        removeAll();

        if (getCurrentDesign() == null) {
            add(buildNoDesignState(), BorderLayout.CENTER);
            revalidate();
            repaint();
            return;
        }

        add(buildTopBar(), BorderLayout.NORTH);
        add(buildMain(), BorderLayout.CENTER);

        revalidate();
        repaint();

        // ✅ Ensure camera fits when returning to the 3D screen (ONE-TIME, not on every resize)
        SwingUtilities.invokeLater(() -> {
            try { renderer.zoomToFit(); } catch (Throwable ignored) { }
        });
    }

    /* ========================== EMPTY STATE ========================== */

    private JComponent buildNoDesignState() {
        JPanel wrap = new JPanel(new GridBagLayout());
        wrap.setOpaque(false);

        UiKit.RoundedPanel card = new UiKit.RoundedPanel(18, UiKit.WHITE);
        card.setBorderPaint(UiKit.BORDER);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(new EmptyBorder(18, 18, 18, 18));
        card.setPreferredSize(new Dimension(560, 240));

        JLabel title = new JLabel("Select or create a design first");
        title.setForeground(UiKit.TEXT);
        title.setFont(UiKit.scaled(title, Font.BOLD, 1.10f));
        title.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel sub = new JLabel("<html>The 3D view renders the <b>currently selected design</b>.<br/>Go to the Design Library to pick one, or create a new design.</html>");
        sub.setForeground(UiKit.MUTED);
        sub.setFont(UiKit.scaled(sub, Font.PLAIN, 0.95f));
        sub.setBorder(new EmptyBorder(8, 0, 0, 0));
        sub.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        btnRow.setOpaque(false);
        btnRow.setBorder(new EmptyBorder(16, 0, 0, 0));
        btnRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        JButton goLibrary = UiKit.primaryButton("Go to Design Library");
        goLibrary.addActionListener(e -> {
            if (router != null) router.show(ScreenKeys.DESIGN_LIBRARY);
        });

        JButton createNew = UiKit.ghostButton("Create New Design");
        createNew.addActionListener(e -> {
            if (router != null) router.show(ScreenKeys.NEW_DESIGN);
        });

        JButton backPlanner = UiKit.ghostButton("Back to Planner");
        backPlanner.addActionListener(e -> {
            if (router != null) router.show(ScreenKeys.PLANNER_2D);
        });

        btnRow.add(goLibrary);
        btnRow.add(createNew);
        btnRow.add(backPlanner);

        card.add(title);
        card.add(sub);
        card.add(btnRow);

        wrap.add(card);
        return wrap;
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
        back.addActionListener(e -> {
            if (router != null) router.show(ScreenKeys.PLANNER_2D);
        });

        JLabel saved = pill("✓  Saved", new Color(0x064E3B), new Color(0x34D399));

        left.add(back);
        left.add(saved);

        JLabel title = new JLabel(getDesignTitleForHeader());
        title.setForeground(new Color(0xE5E7EB));
        title.setFont(title.getFont().deriveFont(Font.BOLD, 12.8f));
        title.setHorizontalAlignment(SwingConstants.CENTER);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        right.setOpaque(false);

        JButton snapshot = primaryDarkButton("📷  Snapshot");
        snapshot.addActionListener(e -> renderer.exportSnapshotDialog());

        JButton present = darkButton("▣  Presentation Mode");
        present.addActionListener(e -> JOptionPane.showMessageDialog(
                this,
                "Presentation Mode (demo).\nTip: extend by hiding side panels + going fullscreen.",
                "Presentation Mode",
                JOptionPane.INFORMATION_MESSAGE
        ));

        right.add(snapshot);
        right.add(present);

        bar.add(left, BorderLayout.WEST);
        bar.add(title, BorderLayout.CENTER);
        bar.add(right, BorderLayout.EAST);

        return bar;
    }

    private String getDesignTitleForHeader() {
        Design d = getCurrentDesign();
        String name = "No design selected";
        if (d != null) {
            try {
                String n = d.getName();
                if (n != null && !n.isBlank()) name = n;
            } catch (Throwable ignored) { }
        }
        return name + " – 3D View";
    }

    private JButton darkButton(String text) {
        JButton b = new JButton(text);
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setForeground(new Color(0xE5E7EB));
        b.setBackground(new Color(0x1F2937));
        b.setBorder(new EmptyBorder(8, 12, 8, 12));
        return b;
    }

    private JButton primaryDarkButton(String text) {
        JButton b = new JButton(text);
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setForeground(Color.WHITE);
        b.setBackground(new Color(0x2563EB));
        b.setBorder(new EmptyBorder(8, 12, 8, 12));
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

        lightingDropdownBtn = darkButton(" " + lightingPreset + "  ▾");
        lightingDropdownBtn.setHorizontalAlignment(SwingConstants.LEFT);
        lightingDropdownBtn.addActionListener(e -> showLightingMenu(lightingDropdownBtn));

        lightingCard.add(t, BorderLayout.NORTH);
        lightingCard.add(lightingDropdownBtn, BorderLayout.CENTER);

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
        title.setBorder(new EmptyBorder(0, 2, 8, 0));

        JPanel btns = new JPanel(new GridLayout(4, 1, 0, 10));
        btns.setOpaque(false);

        JButton reset = iconDockButton("⟳", "Reset camera");
        reset.addActionListener(e -> { renderer.resetCamera(); showToast(); });

        JButton center = iconDockButton("✥", "Center scene");
        center.addActionListener(e -> { renderer.centerScene(); showToast(); });

        JButton zoom = iconDockButton("🔍", "Zoom to fit");
        zoom.addActionListener(e -> { renderer.zoomToFit(); showToast(); });

        JButton home = iconDockButton("⌂", "Default view");
        home.addActionListener(e -> { renderer.resetCamera(); renderer.zoomToFit(); showToast(); });

        btns.add(reset);
        btns.add(center);
        btns.add(zoom);
        btns.add(home);

        dock.add(title, BorderLayout.NORTH);
        dock.add(btns, BorderLayout.CENTER);
        return dock;
    }

    private JButton iconDockButton(String icon, String tooltip) {
        JButton b = new JButton(icon);
        b.setToolTipText(tooltip);
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
        if (lightingDropdownBtn != null) lightingDropdownBtn.setText(" " + lightingPreset + "  ▾");
        renderer.setLightingPreset(preset);
        showToast();
        repaint();
    }

    private void showToast() {
        toast.setVisible(true);
        Timer t = new Timer(1800, e -> toast.setVisible(false));
        t.setRepeats(false);
        t.start();
    }

    /* ========================== DESIGN LOOKUP (Step 1 safe) ========================== */

    private Design getCurrentDesign() {
        if (appState == null) return null;
        // ✅ Step 1: NEVER auto-create here
        try {
            return appState.getCurrentDesign();
        } catch (Throwable ignored) {
            return null;
        }
    }

    /* ========================== SAFE LAYOUT GETTERS ========================== */

    private static Double getLayoutX(Design d) { return readDouble(d, "getLayoutX"); }
    private static Double getLayoutY(Design d) { return readDouble(d, "getLayoutY"); }
    private static Double getLayoutW(Design d) { return readDouble(d, "getLayoutWidth"); }
    private static Double getLayoutH(Design d) { return readDouble(d, "getLayoutHeight"); }

    private static Double readDouble(Object obj, String method) {
        if (obj == null) return null;
        try {
            Object v = obj.getClass().getMethod(method).invoke(obj);
            if (v == null) return null;
            if (v instanceof Number) return ((Number) v).doubleValue();
            return null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    /* ========================== RENDERER PANEL ========================== */

    private class RendererPanel extends JPanel {

        private double yaw = Math.toRadians(35);
        private double pitch = Math.toRadians(-22);
        private double dist = 820;

        private Vec3 sceneCenter = new Vec3(0, 0, 0);
        private Point lastMouse = null;

        private String preset = "Day";

        // ✅ Constant camera lens (stable on resize)
        private final double fovDeg = 58.0;
        private final double fitPadding = 1.18;

        RendererPanel() {
            setOpaque(true);
            setBackground(Color.BLACK);
            setFocusable(true);

            MouseAdapter ma = new MouseAdapter() {
                @Override public void mousePressed(MouseEvent e) {
                    requestFocusInWindow();
                    lastMouse = e.getPoint();
                }
                @Override public void mouseDragged(MouseEvent e) {
                    if (lastMouse == null) return;
                    int dx = e.getX() - lastMouse.x;
                    int dy = e.getY() - lastMouse.y;

                    yaw += dx * 0.0085;
                    pitch += dy * 0.0085;
                    pitch = clamp(pitch, Math.toRadians(-80), Math.toRadians(10));

                    lastMouse = e.getPoint();
                    repaint();
                }
                @Override public void mouseReleased(MouseEvent e) {
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
                @Override public void actionPerformed(ActionEvent e) {
                    resetCamera();
                    showToast();
                }
            });

            getInputMap(WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_F, 0), "fitCam");
            getActionMap().put("fitCam", new AbstractAction() {
                @Override public void actionPerformed(ActionEvent e) {
                    zoomToFit();
                    showToast();
                }
            });

            // ✅ FIX: DO NOT change camera distance continuously during resize (this causes the drift/jump)
            addComponentListener(new ComponentAdapter() {
                @Override public void componentResized(ComponentEvent e) {
                    repaint();
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
            Design d = getCurrentDesign();
            if (d == null) return;

            double roomW = 520;
            double roomD = 520;

            Double lw = getLayoutW(d);
            Double lh = getLayoutH(d);

            if (lw != null && lh != null) {
                roomW = Math.max(520, lw);
                roomD = Math.max(520, lh);
            } else {
                Bounds b = computeSceneBounds();
                roomW = Math.max(520, b.w);
                roomD = Math.max(520, b.d);
            }

            double maxSpan = Math.max(roomW, roomD) * fitPadding;

            // ✅ Constant lens: distance needed so maxSpan fits in view
            double fovRad = Math.toRadians(fovDeg);
            double half = maxSpan * 0.5;
            double needed = half / Math.tan(fovRad * 0.5);

            dist = clamp(needed, 260, 2600);
            repaint();
        }

        void exportSnapshotDialog() {
            try {
                JFileChooser fc = new JFileChooser();
                fc.setDialogTitle("Export Snapshot (PNG)");
                int res = fc.showSaveDialog(this);
                if (res != JFileChooser.APPROVE_OPTION) return;

                java.io.File f = fc.getSelectedFile();
                String name = f.getName().toLowerCase(Locale.ENGLISH);
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
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            // ✅ If design disappears (deleted / unselected), show empty state safely
            if (getCurrentDesign() == null) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(Color.BLACK);
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setColor(new Color(255, 255, 255, 180));
                g2.setFont(g2.getFont().deriveFont(Font.PLAIN, 12f));
                g2.drawString("No design selected. Go to Design Library.", 18, 24);
                g2.dispose();
                return;
            }

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

        /* ========================== ROOM SHAPE HELPERS (3D) ========================== */

        private boolean isLShape(RoomSpec spec) {
            if (spec == null) return false;
            String s = spec.getShape();
            if (s == null) return false;
            return "L-Shape".equalsIgnoreCase(s.trim())
                    && spec.getLCutWidth() > 0
                    && spec.getLCutLength() > 0;
        }

        private int cutWpx(RoomSpec spec, double roomW) {
            double outerW = Math.max(0.0001, spec.getWidth());
            int cut = (int) Math.round(roomW * (spec.getLCutWidth() / outerW));
            return Math.max(1, Math.min((int) roomW - 1, cut));
        }

        private int cutDpx(RoomSpec spec, double roomD) {
            double outerL = Math.max(0.0001, spec.getLength());
            int cut = (int) Math.round(roomD * (spec.getLCutLength() / outerL));
            return Math.max(1, Math.min((int) roomD - 1, cut));
        }

        private List<Face> buildSceneFaces() {
            computeSceneCenter();
            List<Face> faces = new ArrayList<>();

            Design d = getCurrentDesign();
            if (d == null) return faces;

            RoomSpec spec = d.getRoomSpec();

            double roomW = 520;
            double roomD = 520;

            Double lw = getLayoutW(d);
            Double lh = getLayoutH(d);

            if (lw != null && lh != null) {
                roomW = Math.max(520, lw);
                roomD = Math.max(520, lh);
            } else {
                Bounds b = computeSceneBounds();
                roomW = Math.max(520, b.w);
                roomD = Math.max(520, b.d);
            }

            double floorY = 0;
            double wallH = Math.max(260, Math.min(520, Math.max(roomW, roomD) * 0.45));

            double x0 = sceneCenter.x - roomW / 2;
            double x1 = sceneCenter.x + roomW / 2;
            double z0 = sceneCenter.z - roomD / 2;
            double z1 = sceneCenter.z + roomD / 2;

            Color[] schemeCols = schemeColors(spec, preset);
            Color floorCol = schemeCols[0];
            Color wallCol = schemeCols[1];

            if (spec != null && isLShape(spec)) {
                int cutW = cutWpx(spec, roomW);
                int cutD = cutDpx(spec, roomD);

                double xCut = x1 - cutW;
                double zCut = z0 + cutD;

                faces.add(faceQuad(
                        new Vec3(x0, floorY, z0),
                        new Vec3(xCut, floorY, z0),
                        new Vec3(xCut, floorY, z1),
                        new Vec3(x0, floorY, z1),
                        floorCol
                ));

                faces.add(faceQuad(
                        new Vec3(xCut, floorY, zCut),
                        new Vec3(x1,   floorY, zCut),
                        new Vec3(x1,   floorY, z1),
                        new Vec3(xCut, floorY, z1),
                        floorCol
                ));

                addWallSegment(faces, x0,   z0,   xCut, z0,   floorY, wallH, wallCol);
                addWallSegment(faces, xCut, z0,   xCut, zCut, floorY, wallH, wallCol.darker());
                addWallSegment(faces, xCut, zCut, x1,   zCut, floorY, wallH, wallCol);
                addWallSegment(faces, x1,   zCut, x1,   z1,   floorY, wallH, wallCol.darker());
                addWallSegment(faces, x1,   z1,   x0,   z1,   floorY, wallH, wallCol);
                addWallSegment(faces, x0,   z1,   x0,   z0,   floorY, wallH, wallCol.darker());

            } else {
                faces.add(faceQuad(
                        new Vec3(x0, floorY, z0),
                        new Vec3(x1, floorY, z0),
                        new Vec3(x1, floorY, z1),
                        new Vec3(x0, floorY, z1),
                        floorCol
                ));

                addWallSegment(faces, x0, z0, x1, z0, floorY, wallH, wallCol);
                addWallSegment(faces, x1, z0, x1, z1, floorY, wallH, wallCol.darker());
                addWallSegment(faces, x1, z1, x0, z1, floorY, wallH, wallCol);
                addWallSegment(faces, x0, z1, x0, z0, floorY, wallH, wallCol.darker());
            }

            List<FurnitureItem> items = (d.getItems() == null) ? List.of() : d.getItems();
            for (FurnitureItem it : items) faces.addAll(buildFurnitureFaces(it));

            return faces;
        }

        private List<Face> buildFurnitureFaces(FurnitureItem it) {
            List<Face> faces = new ArrayList<>();

            double w = Math.max(10, safeW(it));
            double d = Math.max(10, safeH(it));
            double rot = Math.toRadians(safeRotationDeg(it));

            Color base = parseHex(safeColorHex(it), new Color(0x3B82F6));
            double shadeMul = 0.60 + (clamp(safeShading(it), 0, 100) / 100.0) * 0.70;
            base = applyBrightness(base, shadeMul);

            Design design = getCurrentDesign();
            if (design == null) return faces;

            double roomW = 520;
            double roomD = 520;

            Double lw = getLayoutW(design);
            Double lh = getLayoutH(design);
            if (lw != null && lh != null) {
                roomW = Math.max(520, lw);
                roomD = Math.max(520, lh);
            }

            double lx = safeX(it);
            double lz = safeY(it);

            Double lxo = getLayoutX(design);
            Double lyo = getLayoutY(design);
            if (lxo != null && lyo != null) {
                lx = safeX(it) - lxo;
                lz = safeY(it) - lyo;
            }

            Vec3 c = new Vec3(
                    (lx + w / 2.0) - roomW / 2.0 + sceneCenter.x,
                    0,
                    (lz + d / 2.0) - roomD / 2.0 + sceneCenter.z
            );

            String kind = safeKind(it);

            if ("TABLE_ROUND".equals(kind)) {
                buildRoundTable(faces, c, w, d, rot, base);
            } else if ("TABLE_RECT".equals(kind)) {
                buildRectTable(faces, c, w, d, rot, base);
            } else if ("CHAIR".equals(kind)) {
                buildChair(faces, c, w, d, rot, base);
            } else {
                addBox(faces, c, w, 60, d, rot, base);
            }

            return faces;
        }

        private void buildChair(List<Face> faces, Vec3 c, double w, double d, double rot, Color color) {
            double seatH = 34;
            double seatThick = 4;
            double backH = 38;
            double legDim = Math.min(w, d) * 0.12;
            Color legCol = color.darker();

            double lx = w / 2 - legDim / 2;
            double lz = d / 2 - legDim / 2;

            addBoxWrapper(faces, c, -lx, 0, -lz, legDim, seatH, legDim, rot, legCol);
            addBoxWrapper(faces, c, +lx, 0, -lz, legDim, seatH, legDim, rot, legCol);
            addBoxWrapper(faces, c, +lx, 0, +lz, legDim, seatH, legDim, rot, legCol);
            addBoxWrapper(faces, c, -lx, 0, +lz, legDim, seatH, legDim, rot, legCol);

            addBoxWrapper(faces, c, 0, seatH, 0, w, seatThick, d, rot, color);

            double backThick = 5;
            addBoxWrapper(faces, c, 0, seatH + seatThick, -(d / 2 - backThick / 2), w, backH, backThick, rot, color);
        }

        private void buildRectTable(List<Face> faces, Vec3 c, double w, double d, double rot, Color color) {
            double tableH = 50;
            double topThick = 4;
            double legDim = Math.min(w, d) * 0.08;
            Color legCol = color.darker();

            double lx = w / 2 - legDim;
            double lz = d / 2 - legDim;

            addBoxWrapper(faces, c, -lx, 0, -lz, legDim, tableH, legDim, rot, legCol);
            addBoxWrapper(faces, c, +lx, 0, -lz, legDim, tableH, legDim, rot, legCol);
            addBoxWrapper(faces, c, +lx, 0, +lz, legDim, tableH, legDim, rot, legCol);
            addBoxWrapper(faces, c, -lx, 0, +lz, legDim, tableH, legDim, rot, legCol);

            addBoxWrapper(faces, c, 0, tableH, 0, w, topThick, d, rot, color);
        }

        private void buildRoundTable(List<Face> faces, Vec3 c, double w, double d, double rot, Color color) {
            double tableH = 50;
            double topThick = 4;
            double radius = Math.min(w, d) / 2.0;

            double pillarR = radius * 0.15;
            addPrismWrapper(faces, c, 0, 0, 0, pillarR, tableH, rot, color.darker());
            addPrismWrapper(faces, c, 0, 0, 0, radius * 0.4, 2, rot, color.darker());
            addPrismWrapper(faces, c, 0, tableH, 0, radius, topThick, rot, color);
        }

        private void addBoxWrapper(List<Face> faces, Vec3 c, double offX, double offY, double offZ,
                                   double w, double h, double d, double rot, Color color) {
            Vec3 off = rotY(new Vec3(offX, 0, offZ), rot);
            Vec3 finalC = new Vec3(c.x + off.x, c.y + offY, c.z + off.z);
            addBox(faces, finalC, w, h, d, rot, color);
        }

        private void addPrismWrapper(List<Face> faces, Vec3 c, double offX, double offY, double offZ,
                                     double r, double h, double rot, Color color) {
            Vec3 off = rotY(new Vec3(offX, 0, offZ), rot);
            Vec3 finalC = new Vec3(c.x + off.x, c.y + offY, c.z + off.z);
            addPrism(faces, finalC, r, h, rot, color);
        }

        private void addBox(List<Face> faces, Vec3 c, double w, double h, double d, double rot, Color color) {
            double rx = w / 2;
            double rz = d / 2;

            Vec3 b0 = new Vec3(-rx, 0, -rz);
            Vec3 b1 = new Vec3(+rx, 0, -rz);
            Vec3 b2 = new Vec3(+rx, 0, +rz);
            Vec3 b3 = new Vec3(-rx, 0, +rz);

            Vec3 t0 = new Vec3(-rx, h, -rz);
            Vec3 t1 = new Vec3(+rx, h, -rz);
            Vec3 t2 = new Vec3(+rx, h, +rz);
            Vec3 t3 = new Vec3(-rx, h, +rz);

            Vec3[] v = new Vec3[]{b0, b1, b2, b3, t0, t1, t2, t3};
            for (int i = 0; i < 8; i++) v[i] = rotY(v[i], rot).add(c);

            Vec3 lightDir = getLightDir();
            double gloss = 0.05;

            faces.add(faceQuadWithLighting(v[4], v[5], v[6], v[7], color, lightDir, 1.0 + gloss));
            faces.add(faceQuadWithLighting(v[0], v[1], v[5], v[4], color, lightDir, 0.72));
            faces.add(faceQuadWithLighting(v[1], v[2], v[6], v[5], color, lightDir, 0.80));
            faces.add(faceQuadWithLighting(v[2], v[3], v[7], v[6], color, lightDir, 0.86));
            faces.add(faceQuadWithLighting(v[3], v[0], v[4], v[7], color, lightDir, 0.76));
        }

        private void addPrism(List<Face> faces, Vec3 c, double r, double h, double rot, Color color) {
            int sides = 12;
            Vec3[] bot = new Vec3[sides];
            Vec3[] top = new Vec3[sides];

            for (int i = 0; i < sides; i++) {
                double ang = (2.0 * Math.PI * i) / sides;
                double lx = Math.cos(ang) * r;
                double lz = Math.sin(ang) * r;
                bot[i] = rotY(new Vec3(lx, 0, lz), rot).add(c);
                top[i] = rotY(new Vec3(lx, h, lz), rot).add(c);
            }

            Vec3 lightDir = getLightDir();

            faces.add(new Face(java.util.Arrays.asList(top), applyBrightness(color, 1.05), 0));

            for (int i = 0; i < sides; i++) {
                int next = (i + 1) % sides;
                faces.add(faceQuadWithLighting(bot[i], bot[next], top[next], top[i], color, lightDir, 0.8));
            }
        }

        private Vec3 getLightDir() {
            Vec3 lightDir = new Vec3(-0.35, 0.85, -0.35);
            if ("Night".equalsIgnoreCase(preset)) lightDir = new Vec3(-0.25, 0.60, -0.20);
            if ("Sunset".equalsIgnoreCase(preset)) lightDir = new Vec3(0.55, 0.75, -0.15);
            return lightDir;
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

            // ✅ FIX: Use constant FOV angle -> compute focal length from panel size
            double fovRad = Math.toRadians(fovDeg);
            double focal = (Math.min(w, h) * 0.5) / Math.tan(fovRad * 0.5);

            double cx = w * 0.5;
            double cy = h * 0.52;

            for (Face f : projected) {
                Path2D path = new Path2D.Double();
                boolean started = false;
                boolean clipped = false;

                for (Vec3 p : f.pts) {
                    if (p.z <= 5) { clipped = true; break; }
                    double sx = cx + (p.x / p.z) * focal;
                    double sy = cy - (p.y / p.z) * focal;

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
            Design d = getCurrentDesign();
            if (d == null) return;

            Double lx = getLayoutX(d);
            Double ly = getLayoutY(d);
            Double lw = getLayoutW(d);
            Double lh = getLayoutH(d);

            if (lx != null && ly != null && lw != null && lh != null) {
                double cx = lx + lw / 2.0;
                double cz = ly + lh / 2.0;
                sceneCenter = new Vec3(cx, 0, cz);
                return;
            }

            Bounds b = computeSceneBounds();
            sceneCenter = new Vec3(b.cx, 0, b.cz);
        }

        private Bounds computeSceneBounds() {
            Design d = getCurrentDesign();
            List<FurnitureItem> items = (d == null || d.getItems() == null) ? List.of() : d.getItems();

            double minX = Double.POSITIVE_INFINITY;
            double minZ = Double.POSITIVE_INFINITY;
            double maxX = Double.NEGATIVE_INFINITY;
            double maxZ = Double.NEGATIVE_INFINITY;

            for (FurnitureItem it : items) {
                double x0 = safeX(it);
                double z0 = safeY(it);
                double x1 = x0 + Math.max(10, safeW(it));
                double z1 = z0 + Math.max(10, safeH(it));

                minX = Math.min(minX, Math.min(x0, x1));
                maxX = Math.max(maxX, Math.max(x0, x1));
                minZ = Math.min(minZ, Math.min(z0, z1));
                maxZ = Math.max(maxZ, Math.max(z0, z1));
            }

            if (minX == Double.POSITIVE_INFINITY) {
                minX = -260; maxX = 260;
                minZ = -260; maxZ = 260;
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

        private int clampInt(long v, long lo, long hi) {
            long clamped = Math.max(lo, Math.min(hi, v));
            return (int) clamped;
        }

        private static class Bounds {
            final double w, d, cx, cz;
            Bounds(double x, double z, double w, double d) {
                this.w = w;
                this.d = d;
                this.cx = x + w / 2.0;
                this.cz = z + d / 2.0;
            }
        }

        private void addWallSegment(List<Face> faces,
                                    double ax, double az, double bx, double bz,
                                    double floorY, double wallH, Color col) {
            faces.add(faceQuad(
                    new Vec3(ax, floorY, az),
                    new Vec3(bx, floorY, bz),
                    new Vec3(bx, floorY + wallH, bz),
                    new Vec3(ax, floorY + wallH, az),
                    col
            ));
        }

        private Color[] schemeColors(RoomSpec spec, String preset) {
            String scheme = (spec == null || spec.getColorScheme() == null)
                    ? "neutral"
                    : spec.getColorScheme().toLowerCase(Locale.ENGLISH);

            Color floor = new Color(40, 48, 70, 220);
            Color wall = new Color(24, 30, 46, 210);

            if (scheme.contains("warm")) {
                floor = new Color(72, 54, 46, 220);
                wall = new Color(44, 32, 28, 210);
            } else if (scheme.contains("cool")) {
                floor = new Color(40, 58, 84, 220);
                wall = new Color(22, 32, 52, 210);
            } else if (scheme.contains("mono")) {
                floor = new Color(55, 55, 60, 220);
                wall = new Color(32, 32, 36, 210);
            } else if (scheme.contains("pastel")) {
                floor = new Color(70, 54, 72, 220);
                wall = new Color(40, 30, 48, 210);
            }

            if ("Night".equalsIgnoreCase(preset)) {
                floor = new Color(floor.getRed() / 2, floor.getGreen() / 2, floor.getBlue() / 2, floor.getAlpha());
                wall = new Color(wall.getRed() / 2, wall.getGreen() / 2, wall.getBlue() / 2, wall.getAlpha());
            } else if ("Sunset".equalsIgnoreCase(preset)) {
                floor = new Color(
                        Math.min(255, (int) (floor.getRed() * 1.10)),
                        Math.min(255, (int) (floor.getGreen() * 0.92)),
                        Math.min(255, (int) (floor.getBlue() * 0.92)),
                        floor.getAlpha()
                );
                wall = new Color(
                        Math.min(255, (int) (wall.getRed() * 1.18)),
                        Math.min(255, (int) (wall.getGreen() * 0.90)),
                        Math.min(255, (int) (wall.getBlue() * 0.85)),
                        wall.getAlpha()
                );
            }

            return new Color[]{floor, wall};
        }

        /* ========================== SAFE ITEM GETTERS ========================== */

        private double safeX(FurnitureItem it) { return readNum(it, "getX", 0); }
        private double safeY(FurnitureItem it) { return readNum(it, "getY", 0); }
        private double safeW(FurnitureItem it) { return readNum(it, "getW", 60); }
        private double safeH(FurnitureItem it) { return readNum(it, "getH", 40); }

        private int safeRotationDeg(FurnitureItem it) {
            Double v = readNumObj(it, "getRotation", null);
            if (v == null) v = readNumObj(it, "getRotationDeg", null);
            if (v == null) v = readNumObj(it, "getRotationDegrees", null);
            return (int) Math.round(v == null ? 0 : v);
        }

        private int safeShading(FurnitureItem it) {
            Double v = readNumObj(it, "getShadingPercent", null);
            if (v == null) v = readNumObj(it, "getShade", null);
            return (int) Math.round(v == null ? 50 : v);
        }

        private String safeColorHex(FurnitureItem it) {
            try {
                Object v = it.getClass().getMethod("getColorHex").invoke(it);
                return (v == null) ? null : v.toString();
            } catch (Throwable ignored) { }
            return null;
        }

        private String safeKind(FurnitureItem it) {
            try {
                Object k = it.getClass().getMethod("getKind").invoke(it);
                return (k == null) ? "" : k.toString();
            } catch (Throwable ignored) { }
            return "";
        }

        private double readNum(Object obj, String method, double fallback) {
            Double v = readNumObj(obj, method, null);
            return (v == null) ? fallback : v;
        }

        private Double readNumObj(Object obj, String method, Double fallback) {
            if (obj == null) return fallback;
            try {
                Object v = obj.getClass().getMethod(method).invoke(obj);
                if (v instanceof Number) return ((Number) v).doubleValue();
            } catch (Throwable ignored) { }
            return fallback;
        }

        /* ========================== DATA TYPES ========================== */

        private class Face {
            final List<Vec3> pts;
            final Color color;
            final double avgDepth;
            Face(List<Vec3> pts, Color color, double avgDepth) {
                this.pts = pts;
                this.color = color;
                this.avgDepth = avgDepth;
            }
        }

        private class Vec3 {
            final double x, y, z;
            Vec3(double x, double y, double z) { this.x = x; this.y = y; this.z = z; }
            Vec3 add(Vec3 o) { return new Vec3(x + o.x, y + o.y, z + o.z); }
            Vec3 sub(Vec3 o) { return new Vec3(x - o.x, y - o.y, z - o.z); }
        }
    }
}
