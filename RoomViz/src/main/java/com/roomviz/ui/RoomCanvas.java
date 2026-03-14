package com.roomviz.ui;

import com.roomviz.model.FurnitureItem;
import com.roomviz.model.FurnitureKind;
import com.roomviz.model.FurnitureTemplate;
import com.roomviz.model.RoomSpec;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.util.UUID;

public class RoomCanvas extends JPanel {

    private java.util.List<FurnitureItem> items = new java.util.ArrayList<>();
    private FurnitureItem selected = null;
    private RoomSpec roomSpec;

    private Point dragStartMouse = null;
    private Point dragStartItem = null;

    private Runnable onSelectionChanged = null;

    // edit hooks (for undo snapshots + autosave)
    private Runnable onEditStart = null;
    private Runnable onEditCommit = null;

    // delete hook (keyboard delete)
    private Runnable onDeleteRequested = null;

    public RoomCanvas() {
        this(null);
    }

    public RoomCanvas(RoomSpec spec) {
        this.roomSpec = spec;
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
                    // snapshot once when user starts dragging
                    if (onEditStart != null) onEditStart.run();

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
                if (selected != null && dragStartItem != null) {
                    boolean moved = selected.getX() != dragStartItem.x || selected.getY() != dragStartItem.y;
                    if (moved && onEditCommit != null) onEditCommit.run();
                }
                dragStartMouse = null;
                dragStartItem = null;
            }
        };

        addMouseListener(ma);
        addMouseMotionListener(ma);

        getInputMap(WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "deleteSelected");
        getActionMap().put("deleteSelected", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) {
                if (onDeleteRequested != null) onDeleteRequested.run();
                else deleteSelected();
            }
        });
    }

    public void setRoomSpec(RoomSpec spec) {
        this.roomSpec = spec;
        repaint();
    }

    public void setOnSelectionChanged(Runnable r) { this.onSelectionChanged = r; }
    public void setOnEditStart(Runnable r) { this.onEditStart = r; }
    public void setOnEditCommit(Runnable r) { this.onEditCommit = r; }
    public void setOnDeleteRequested(Runnable r) { this.onDeleteRequested = r; }

    public java.util.List<FurnitureItem> getItems() { return items; }

    public void setItems(java.util.List<FurnitureItem> items) {
        this.items = (items == null) ? new java.util.ArrayList<>() : items;
        if (!this.items.contains(selected)) selected = null;
        repaint();
        fireSelectionChanged();
    }

    public FurnitureItem getSelected() { return selected; }

    public void addItemFromTemplate(FurnitureTemplate t) {
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

    public void setSelected(FurnitureItem it) {
        selected = it;
        fireSelectionChanged();
    }

    public void fireSelectionChanged() {
        if (onSelectionChanged != null) onSelectionChanged.run();
    }

    public FurnitureItem hitTest(Point p) {
        for (int i = items.size() - 1; i >= 0; i--) {
            FurnitureItem it = items.get(i);
            Rectangle r = new Rectangle(it.getX(), it.getY(), it.getW(), it.getH());
            if (r.contains(p)) return it;
        }
        return null;
    }

    public void setSelectedPosition(int x, int y) {
        if (selected == null) return;
        selected.setX(x);
        selected.setY(y);
        repaint();
        fireSelectionChanged();
    }

    public void setSelectedSize(int w, int h) {
        if (selected == null) return;
        selected.setW(w);
        selected.setH(h);
        repaint();
        fireSelectionChanged();
    }

    public void setSelectedRotation(int deg) {
        if (selected == null) return;
        int v = ((deg % 360) + 360) % 360;
        selected.setRotation(v);
        repaint();
        fireSelectionChanged();
    }

    public void nudgeRotation(int delta) {
        if (selected == null) return;
        setSelectedRotation(selected.getRotation() + delta);
    }

    public void setSelectedShading(int v) {
        if (selected == null) return;
        selected.setShadingPercent(Math.max(0, Math.min(100, v)));
        repaint();
        fireSelectionChanged();
    }

    public void deleteSelected() {
        if (selected == null) return;
        items.remove(selected);
        selected = null;
        repaint();
        fireSelectionChanged();
    }

    public void layerForward() {
        if (selected == null) return;
        int idx = items.indexOf(selected);
        if (idx < 0 || idx == items.size() - 1) return;
        items.remove(idx);
        items.add(idx + 1, selected);
        repaint();
    }

    public void layerBackward() {
        if (selected == null) return;
        int idx = items.indexOf(selected);
        if (idx <= 0) return;
        items.remove(idx);
        items.add(idx - 1, selected);
        repaint();
    }

    public void layerToFront() {
        if (selected == null) return;
        items.remove(selected);
        items.add(selected);
        repaint();
    }

    public void layerToBack() {
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
        
        // Calculate scaled room dimensions
        int roomW, roomH;
        
        if (roomSpec != null && roomSpec.getWidth() > 0 && roomSpec.getLength() > 0) {
             // Available drawing area
             int availW = w - pad * 2;
             int availH = h - pad * 2;
             
             double rw = roomSpec.getWidth();
             double rl = roomSpec.getLength();
             
             // Aspect ratio of the room
             double roomRatio = rw / rl;
             
             // Aspect ratio of the available space
             double screenRatio = (double) availW / availH;
             
             if (roomRatio > screenRatio) {
                 // Room is wider than screen, fit to width
                 roomW = availW;
                 roomH = (int) (availW / roomRatio);
             } else {
                 // Room is taller than screen, fit to height
                 roomH = availH;
                 roomW = (int) (availH * roomRatio);
             }
        } else {
            // Fallback fixed size if no spec
            roomW = w - pad * 2;
            roomH = h - pad * 2;
        }

        // Center the room
        int rx = (w - roomW) / 2;
        int ry = (h - roomH) / 2;

        g2.setColor(new Color(0, 0, 0, 18));
        g2.fillRoundRect(rx + 2, ry + 3, roomW, roomH, 22, 22);

        g2.setColor(new Color(0xF3F4F6));
        g2.fillRoundRect(rx, ry, roomW, roomH, 22, 22);

        g2.setColor(new Color(0xD1D5DB));
        g2.setStroke(new BasicStroke(1.2f));
        g2.drawRoundRect(rx, ry, roomW, roomH, 22, 22);

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

    private void paintItem(Graphics2D parentG, FurnitureItem it) {
        Graphics2D g2 = (Graphics2D) parentG.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        Rectangle r = new Rectangle(it.getX(), it.getY(), it.getW(), it.getH());
        boolean isSel = (it == selected);

        // Rotation
        if (it.getRotation() != 0) {
            double cx = r.getX() + r.getWidth() / 2.0;
            double cy = r.getY() + r.getHeight() / 2.0;
            g2.rotate(Math.toRadians(it.getRotation()), cx, cy);
        }

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
        
        // Use FurnitureKind icon logic if possible, or fallback manually
        // We moved FurnitureKind to model so we cannot easily reverse map string to enum in paintItem 
        // without try-catch or valueOf. Let's replicate logic for robustness or try valueOf.
        String icon = "🪑";
        try {
            FurnitureKind k = FurnitureKind.valueOf(kind);
            icon = k.iconText;
        } catch (Exception e) {
            // fallback generic
            if (round) icon = "●";
            else if ("TABLE_RECT".equals(kind)) icon = "▭";
        }
        
        FontMetrics fm = g2.getFontMetrics();
        int tx = r.x + (r.width - fm.stringWidth(icon)) / 2;
        int ty = r.y + (r.height + fm.getAscent()) / 2 - 2;
        
        // Draw icon (centered, but rotated with context)
        g2.drawString(icon, tx, ty);

        if (isSel) {
            g2.setColor(new Color(0x2563EB));
            g2.setStroke(new BasicStroke(2.0f));
            if (round) g2.drawOval(r.x - 2, r.y - 2, r.width + 4, r.height + 4);
            else g2.drawRoundRect(r.x - 2, r.y - 2, r.width + 4, r.height + 4, 16, 16);
        }

        g2.dispose();
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
