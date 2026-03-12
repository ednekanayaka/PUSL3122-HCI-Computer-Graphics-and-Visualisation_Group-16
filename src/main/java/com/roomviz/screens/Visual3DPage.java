package com.roomviz.screens;

import com.roomviz.app.AppFrame;
import com.roomviz.app.Router;
import com.roomviz.app.ScreenKeys;
import com.roomviz.ui.UiKit;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * 3D Visual Page (UI scaffold) – matches screenshot layout.
 */
public class Visual3DPage extends JPanel {

    private final AppFrame frame;
    private final Router router;

    private BufferedImage bg;
    private final JLabel toast = new JLabel("3D view updated");
    private final JPopupMenu lightingMenu = new JPopupMenu();

    private String lightingPreset = "Day";

    public Visual3DPage(AppFrame frame, Router router) {
        this.frame = frame;
        this.router = router;

        setLayout(new BorderLayout());
        setBackground(UiKit.BG);
        setBorder(new EmptyBorder(14, 14, 14, 14));

        tryLoadBackground();

        add(buildTopBar(), BorderLayout.NORTH);
        add(buildMain(), BorderLayout.CENTER);
    }

    private void tryLoadBackground() {
        try {
            var in = getClass().getResourceAsStream("/assets/3d-room.jpg");
            if (in != null) bg = ImageIO.read(in);
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
        back.addActionListener(e -> {
            // ✅ Correct navigation for your project
            router.show(ScreenKeys.PLANNER_2D);
        });

        JLabel saved = pill("✓  Saved", new Color(0x064E3B), new Color(0x34D399));

        left.add(back);
        left.add(saved);

        JLabel title = new JLabel("Living Room Design – 3D View");
        title.setForeground(new Color(0xE5E7EB));
        title.setFont(title.getFont().deriveFont(Font.BOLD, 12.8f));
        title.setHorizontalAlignment(SwingConstants.CENTER);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        right.setOpaque(false);

        JButton snapshot = primaryDarkButton("📷  Snapshot");
        snapshot.addActionListener(e -> JOptionPane.showMessageDialog(
                this,
                "Snapshot saved (demo).\nHook this to export a rendered image later.",
                "Snapshot",
                JOptionPane.INFORMATION_MESSAGE
        ));

        JButton present = darkButton("▣  Presentation Mode");
        present.addActionListener(e -> JOptionPane.showMessageDialog(
                this,
                "Presentation Mode (demo).\nYou can later toggle fullscreen + hide panels.",
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

    private JButton darkButton(String text) {
        JButton b = new JButton(text);
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setForeground(new Color(0xE5E7EB));
        b.setBackground(new Color(0x0B1220));
        b.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(255, 255, 255, 18), 1, true),
                new EmptyBorder(8, 12, 8, 12)
        ));
        return b;
    }

    private JButton primaryDarkButton(String text) {
        JButton b = new JButton(text);
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setForeground(Color.WHITE);
        b.setBackground(new Color(0x2563EB));
        b.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(255, 255, 255, 18), 1, true),
                new EmptyBorder(8, 12, 8, 12)
        ));
        return b;
    }

    private JLabel pill(String text, Color bg, Color fg) {
        JLabel l = new JLabel(text);
        l.setOpaque(true);
        l.setBackground(bg);
        l.setForeground(fg);
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
                new EmptyBorder(8, 12, 8, 12)
        ));
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

        btns.add(iconDockButton("⟳"));
        btns.add(iconDockButton("✥"));
        btns.add(iconDockButton("🔍"));
        btns.add(iconDockButton("⌂"));

        dock.add(title, BorderLayout.NORTH);
        dock.add(btns, BorderLayout.CENTER);
        return dock;
    }

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
        showToast();
        repaint();
    }

    private void showToast() {
        toast.setVisible(true);
        Timer t = new Timer(2000, e -> toast.setVisible(false));
        t.setRepeats(false);
        t.start();
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
