package com.roomviz.ui;

import com.roomviz.model.FurnitureItem;
import com.roomviz.model.FurnitureKind;
import com.roomviz.model.FurnitureTemplate;
import com.roomviz.model.RoomSpec;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Path2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class RoomCanvas extends JPanel {

    private List<FurnitureItem> items = new ArrayList<>();
    private FurnitureItem selected = null;
    private RoomSpec roomSpec;

    private Point dragStartMouse = null;
    private Point dragStartItem = null;

    // Room boundary (computed each paint from roomSpec + viewport scaling)
    private Shape cachedRoomShape = null;
    private Rectangle cachedRoomBoundsPx = null;
    private double ppi = 1.0; // Pixels per inch

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
            @Override
            public void mousePressed(MouseEvent e) {
                requestFocusInWindow();

                FurnitureItem hit = hitTest(e.getPoint());
                setSelected(hit);

                if (hit != null) {
                    if (onEditStart != null) onEditStart.run();

                    dragStartMouse = e.getPoint();
                    dragStartItem = new Point(safeInt(hit.getX()), safeInt(hit.getY()));
                } else {
                    dragStartMouse = null;
                    dragStartItem = null;
                }
                repaint();
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                if (selected == null || dragStartMouse == null || dragStartItem == null) return;
                ensureRoomCache();

                int dx = e.getX() - dragStartMouse.x;
                int dy = e.getY() - dragStartMouse.y;

                // Move in inches
                selected.setX(dragStartItem.x + (int)(dx / ppi));
                selected.setY(dragStartItem.y + (int)(dy / ppi));

                repaint();
                fireSelectionChanged();
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                ensureRoomCache(); 

                if (selected != null) {
                    clampItemIntoRoom(selected);
                    repaint();
                    fireSelectionChanged();
                }

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

        // ✅ Keep cache updated on resize too
        addComponentListener(new ComponentAdapter() {
            @Override public void componentResized(ComponentEvent e) {
                cachedRoomBoundsPx = null;
                cachedRoomShape = null;
                repaint();
            }
        });
    }

    public void setRoomSpec(RoomSpec spec) {
        this.roomSpec = spec;
        cachedRoomBoundsPx = null;
        cachedRoomShape = null;
        ensureRoomCache();
        repaint();
    }

    public Rectangle getRoomBounds() {
        return cachedRoomBoundsPx;
    }

    public void setOnSelectionChanged(Runnable r) { this.onSelectionChanged = r; }
    public void setOnEditStart(Runnable r) { this.onEditStart = r; }
    public void setOnEditCommit(Runnable r) { this.onEditCommit = r; }
    public void setOnDeleteRequested(Runnable r) { this.onDeleteRequested = r; }

    public List<FurnitureItem> getItems() { return items; }

    public void setItems(List<FurnitureItem> items) {
        this.items = (items == null) ? new ArrayList<>() : items;
        if (selected != null && !this.items.contains(selected)) selected = null;
        repaint();
        fireSelectionChanged();
    }

    public FurnitureItem getSelected() { return selected; }

    public void addItemFromTemplate(FurnitureTemplate t) {
        if (t == null) return;

        ensureRoomCache();

        int defW = Math.max(10, t.defaultW);
        int defH = Math.max(10, t.defaultH);

        // Center item in room (inches)
        double rw = 120;
        double rl = 120;
        if (roomSpec != null) {
            rw = roomSpec.getWidth();
            rl = roomSpec.getLength();
            String unit = roomSpec.getUnit();
            if (unit == null || "ft".equalsIgnoreCase(unit.trim())) {
                rw *= 12.0;
                rl *= 12.0;
            } else if ("m".equalsIgnoreCase(unit.trim())) {
                rw *= 39.3701;
                rl *= 39.3701;
            }
        }

        int cx = (int)(rw / 2.0 - defW / 2.0);
        int cy = (int)(rl / 2.0 - defH / 2.0);

        FurnitureItem it = new FurnitureItem(
                UUID.randomUUID().toString(),
                t.name,
                (t.kind == null ? "CHAIR" : t.kind.name()),
                cx, cy, defW, defH
        );

        items.add(it);
        setSelected(it);

        clampItemIntoRoom(it);

        repaint();
        if (onEditCommit != null) onEditCommit.run();
    }

    public void setSelected(FurnitureItem it) {
        selected = it;
        fireSelectionChanged();
        repaint();
    }

    public void fireSelectionChanged() {
        if (onSelectionChanged != null) onSelectionChanged.run();
    }

    public FurnitureItem hitTest(Point p) {
        if (p == null) return null;
        ensureRoomCache();
        if (cachedRoomBoundsPx == null) return null;

        for (int i = items.size() - 1; i >= 0; i--) {
            FurnitureItem it = items.get(i);
            if (it == null) continue;

            // Transform item to screen rectangles
            int ix = cachedRoomBoundsPx.x + (int)(it.getX() * ppi);
            int iy = cachedRoomBoundsPx.y + (int)(it.getY() * ppi);
            int iw = (int)(it.getW() * ppi);
            int ih = (int)(it.getH() * ppi);

            Rectangle r = new Rectangle(ix, iy, Math.max(4, iw), Math.max(4, ih));
            if (r.contains(p)) return it;
        }
        return null;
    }

    public void setSelectedPosition(int x, int y) {
        if (selected == null) return;
        ensureRoomCache();

        selected.setX(x);
        selected.setY(y);

        clampItemIntoRoom(selected);

        repaint();
        fireSelectionChanged();
        if (onEditCommit != null) onEditCommit.run();
    }

    public void setSelectedSize(int w, int h) {
        if (selected == null) return;
        ensureRoomCache();

        selected.setW(Math.max(1, w));
        selected.setH(Math.max(1, h));

        clampItemIntoRoom(selected);

        repaint();
        fireSelectionChanged();
        if (onEditCommit != null) onEditCommit.run();
    }

    public void setSelectedRotation(int deg) {
        if (selected == null) return;
        int v = ((deg % 360) + 360) % 360;
        selected.setRotation(v);
        repaint();
        fireSelectionChanged();
        if (onEditCommit != null) onEditCommit.run();
    }

    public void nudgeRotation(int delta) {
        if (selected == null) return;
        setSelectedRotation(selected.getRotation() + delta);
    }

    public void setSelectedShading(int v) {
        if (selected == null) return;
        try {
            selected.setShadingPercent(Math.max(0, Math.min(100, v)));
        } catch (Throwable ignored) { }
        repaint();
        fireSelectionChanged();
        if (onEditCommit != null) onEditCommit.run();
    }

    public void deleteSelected() {
        if (selected == null) return;
        items.remove(selected);
        selected = null;
        repaint();
        fireSelectionChanged();
        if (onEditCommit != null) onEditCommit.run();
    }

    public void layerForward() {
        if (selected == null) return;
        int idx = items.indexOf(selected);
        if (idx < 0 || idx == items.size() - 1) return;
        items.remove(idx);
        items.add(idx + 1, selected);
        repaint();
        if (onEditCommit != null) onEditCommit.run();
    }

    public void layerBackward() {
        if (selected == null) return;
        int idx = items.indexOf(selected);
        if (idx <= 0) return;
        items.remove(idx);
        items.add(idx - 1, selected);
        repaint();
        if (onEditCommit != null) onEditCommit.run();
    }

    public void layerToFront() {
        if (selected == null) return;
        items.remove(selected);
        items.add(selected);
        repaint();
        if (onEditCommit != null) onEditCommit.run();
    }

    public void layerToBack() {
        if (selected == null) return;
        items.remove(selected);
        items.add(0, selected);
        repaint();
        if (onEditCommit != null) onEditCommit.run();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();

        paintGrid(g2, w, h);

        ensureRoomCache(); // ✅ ensure cachedRoomBoundsPx/cachedRoomShape exists

        if (cachedRoomBoundsPx != null) {
            int rx = cachedRoomBoundsPx.x;
            int ry = cachedRoomBoundsPx.y;
            int roomW = cachedRoomBoundsPx.width;
            int roomH = cachedRoomBoundsPx.height;

            Color roomFill = schemeRoomFill(roomSpec);
            Color roomBorder = schemeRoomBorder(roomSpec);

            // Shadow
            g2.setColor(new Color(0, 0, 0, 18));
            if (isLShape(roomSpec)) {
                Shape shadow = buildLShape(rx + 2, ry + 3, roomW, roomH, roomSpec);
                g2.fill(shadow);
            } else {
                g2.fillRoundRect(rx + 2, ry + 3, roomW, roomH, 22, 22);
            }

            // Fill + border
            g2.setColor(roomFill);
            if (isLShape(roomSpec)) {
                Shape s = buildLShape(rx, ry, roomW, roomH, roomSpec);
                g2.fill(s);

                g2.setColor(roomBorder);
                g2.setStroke(new BasicStroke(1.3f));
                g2.draw(s);
            } else {
                g2.fillRoundRect(rx, ry, roomW, roomH, 22, 22);

                g2.setColor(roomBorder);
                g2.setStroke(new BasicStroke(1.2f));
                g2.drawRoundRect(rx, ry, roomW, roomH, 22, 22);
            }
        }

        paintRulers(g2, w, h);

        for (FurnitureItem it : items) paintItem(g2, it);

        g2.dispose();
    }

    private void paintGrid(Graphics2D g2, int w, int h) {
        g2.setColor(new Color(0xE5E7EB));
        ensureRoomCache();
        double stepInches = 12.0; // 1ft grid
        double stepPx = stepInches * ppi;
        if (stepPx < 10) stepPx = 60.0 * ppi; // 5ft grid if too small
        
        if (stepPx > 0) {
            for (double x = cachedRoomBoundsPx.x; x < cachedRoomBoundsPx.x + cachedRoomBoundsPx.width; x += stepPx) {
                for (double y = cachedRoomBoundsPx.y; y < cachedRoomBoundsPx.y + cachedRoomBoundsPx.height; y += stepPx) {
                    g2.fillRect((int)x, (int)y, 1, 1);
                }
            }
        }
    }

    private void paintRulers(Graphics2D g2, int w, int h) {
        // Subtle background for rulers
        g2.setColor(new Color(0xF9FAFB));
        g2.fillRect(0, 0, w, 22);
        g2.fillRect(0, 0, 22, h);

        g2.setColor(new Color(0x9CA3AF));
        g2.setFont(g2.getFont().deriveFont(Font.PLAIN, 10f));

        ensureRoomCache();
        double stepInches = 60.0; // 5ft marks
        double stepPx = stepInches * ppi;
        
        if (stepPx > 0) {
            for (double d = 0; d < 1000; d += stepInches) {
                int px = cachedRoomBoundsPx.x + (int)(d * ppi);
                if (px > w) break;
                g2.drawLine(px, 22, px, 18);
                g2.drawString((int)d + "\"", px + 2, 14);
            }
            for (double d = 0; d < 1000; d += stepInches) {
                int py = cachedRoomBoundsPx.y + (int)(d * ppi);
                if (py > h) break;
                g2.drawLine(22, py, 18, py);
                g2.drawString((int)d + "\"", 2, py + 4);
            }
        }

        g2.setColor(UiKit.BORDER);
        g2.drawLine(0, 21, w, 21);
        g2.drawLine(21, 0, 21, h);
    }

    private void paintItem(Graphics2D parentG, FurnitureItem it) {
        if (it == null) return;
        ensureRoomCache();
        if (cachedRoomBoundsPx == null) return;

        int ix = cachedRoomBoundsPx.x + (int)(it.getX() * ppi);
        int iy = cachedRoomBoundsPx.y + (int)(it.getY() * ppi);
        int iw = (int)(it.getW() * ppi);
        int ih = (int)(it.getH() * ppi);

        Graphics2D g2 = (Graphics2D) parentG.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        Rectangle r = new Rectangle(ix, iy, Math.max(1, iw), Math.max(1, ih));

        boolean isSel = (it == selected);

        int rot = safeInt(it.getRotation());
        if (rot != 0) {
            double cx = r.getX() + r.getWidth() / 2.0;
            double cy = r.getY() + r.getHeight() / 2.0;
            g2.rotate(Math.toRadians(rot), cx, cy);
        }

        Color base = parseHexOrDefault(safeColorHex(it), new Color(0x3B82F6));

        String kind = safeKind(it);
        boolean round = "TABLE_ROUND".equals(kind);
        boolean chair = "CHAIR".equals(kind);

        // Fill shapes
        g2.setColor(base);

        if (round) {
            g2.fillOval(r.x, r.y, r.width, r.height);
            g2.setColor(new Color(0, 0, 0, 40));
            g2.setStroke(new BasicStroke(1f));
            g2.drawOval(r.x, r.y, r.width, r.height);

        } else if (chair) {
            int backThick = Math.max(4, r.height / 6);
            int gap = 2;

            int bx = r.x;
            int by = r.y;
            int bw = r.width;
            int bh = backThick;

            int sx = r.x;
            int sy = r.y + backThick + gap;
            int sw = r.width;
            int sh = r.height - (backThick + gap);

            g2.setColor(base);
            g2.fillRoundRect(bx, by, bw, bh, 4, 4);
            g2.fillRoundRect(sx, sy, sw, sh, 4, 4);

            g2.setColor(new Color(0, 0, 0, 40));
            g2.drawRoundRect(bx, by, bw, bh, 4, 4);
            g2.drawRoundRect(sx, sy, sw, sh, 4, 4);

        } else {
            g2.fillRoundRect(r.x, r.y, r.width, r.height, 8, 8);
            g2.setColor(new Color(0, 0, 0, 40));
            g2.drawRoundRect(r.x, r.y, r.width, r.height, 8, 8);
        }

        // Shading overlay (uses shadingPercent if present)
        int shade = safeShadingPercent(it);
        if (shade > 0) {
            int alpha = Math.max(0, Math.min(180, (int) Math.round(180.0 * (shade / 100.0))));
            g2.setColor(new Color(0, 0, 0, alpha));
            if (round) g2.fillOval(r.x, r.y, r.width, r.height);
            else if (chair) g2.fillRoundRect(r.x, r.y, r.width, r.height, 6, 6);
            else g2.fillRoundRect(r.x, r.y, r.width, r.height, 8, 8);
        }

        // 2.5D Depth Highlight (Subtle top-left shine)
        g2.setColor(new Color(255, 255, 255, 40));
        g2.setStroke(new BasicStroke(1.5f));
        if (round) {
            g2.drawArc(r.x + 2, r.y + 2, r.width - 4, r.height - 4, 45, 180);
        } else {
            g2.drawLine(r.x + 4, r.y + 2, r.x + r.width - 4, r.y + 2);
            g2.drawLine(r.x + 2, r.y + 4, r.x + 2, r.y + r.height - 4);
        }

        // Icon
        String icon = pickIcon(kind, round, chair);
        int minDim = Math.min(r.width, r.height);
        float fontSize = Math.max(12f, Math.min(40f, minDim * 0.5f));
        g2.setFont(g2.getFont().deriveFont(Font.BOLD, fontSize));

        FontMetrics fm = g2.getFontMetrics();
        int tx = r.x + (r.width - fm.stringWidth(icon)) / 2;
        int ty = r.y + (r.height + fm.getAscent()) / 2 - (int) (fm.getDescent() * 0.5);

        g2.setColor(new Color(0, 0, 0, 80));
        g2.drawString(icon, tx + 1, ty + 1);

        g2.setColor(Color.WHITE);
        g2.drawString(icon, tx, ty);

        // Selection highlight
        // Selection highlight with handles
        if (isSel) {
            g2.setColor(UiKit.PRIMARY);
            g2.setStroke(new BasicStroke(2.0f));
            int pad = 4;
            Rectangle sr = new Rectangle(r.x - pad / 2, r.y - pad / 2, r.width + pad, r.height + pad);

            if (round) g2.drawOval(sr.x, sr.y, sr.width, sr.height);
            else g2.drawRoundRect(sr.x, sr.y, sr.width, sr.height, 10, 10);

            // Corner handles
            g2.setColor(Color.WHITE);
            int hSize = 6;
            paintHandle(g2, sr.x, sr.y, hSize);
            paintHandle(g2, sr.x + sr.width, sr.y, hSize);
            paintHandle(g2, sr.x, sr.y + sr.height, hSize);
            paintHandle(g2, sr.x + sr.width, sr.y + sr.height, hSize);
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

    /* ====================== Room boundary helpers ====================== */

    private boolean isLShape(RoomSpec spec) {
        if (spec == null) return false;
        String s = spec.getShape();
        if (s == null) return false;
        return "L-Shape".equalsIgnoreCase(s.trim()) && spec.getLCutWidth() > 0 && spec.getLCutLength() > 0;
    }

    private Shape buildLShape(int rx, int ry, int roomW, int roomH, RoomSpec spec) {
        double outerW = Math.max(0.0001, spec.getWidth());
        double outerL = Math.max(0.0001, spec.getLength());

        int cutW = (int) Math.round(roomW * (spec.getLCutWidth() / outerW));
        int cutH = (int) Math.round(roomH * (spec.getLCutLength() / outerL));

        cutW = Math.max(1, Math.min(roomW - 1, cutW));
        cutH = Math.max(1, Math.min(roomH - 1, cutH));

        Path2D path = new Path2D.Double();
        path.moveTo(rx, ry);
        path.lineTo(rx + roomW - cutW, ry);
        path.lineTo(rx + roomW - cutW, ry + cutH);
        path.lineTo(rx + roomW, ry + cutH);
        path.lineTo(rx + roomW, ry + roomH);
        path.lineTo(rx, ry + roomH);
        path.closePath();
        return path;
    }

    private Shape buildRoomShapePx(int rx, int ry, int roomW, int roomH, RoomSpec spec) {
        if (spec == null) return new Rectangle(rx, ry, roomW, roomH);
        if (!isLShape(spec)) return new Rectangle(rx, ry, roomW, roomH);
        return buildLShape(rx, ry, roomW, roomH, spec);
    }

    private boolean rectInsideRoom(Rectangle rPx) {
        if (cachedRoomShape == null) return true;
        return cachedRoomShape.contains(rPx.x, rPx.y)
                && cachedRoomShape.contains(rPx.x + rPx.width, rPx.y)
                && cachedRoomShape.contains(rPx.x, rPx.y + rPx.height)
                && cachedRoomShape.contains(rPx.x + rPx.width, rPx.y + rPx.height);
    }

    private void clampItemIntoRoom(FurnitureItem it) {
        if (roomSpec == null || it == null) return;

        double rw = roomSpec.getWidth();
        double rl = roomSpec.getLength();
        
        String unit = roomSpec.getUnit();
        if (unit == null || "ft".equalsIgnoreCase(unit.trim())) {
            rw *= 12.0;
            rl *= 12.0;
        } else if ("m".equalsIgnoreCase(unit.trim())) {
            rw *= 39.3701;
            rl *= 39.3701;
        }

        // Clamp in inches
        it.setX(clampInt(it.getX(), 0, (int)rw - it.getW()));
        it.setY(clampInt(it.getY(), 0, (int)rl - it.getH()));
    }

    private int clampInt(int v, int lo, int hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    private int safeInt(Integer v) { return v == null ? 0 : v; }

    /* ====================== Scheme helpers ====================== */

    private Color schemeRoomFill(RoomSpec spec) {
        String scheme = (spec == null) ? null : spec.getColorScheme();
        if (scheme == null) return new Color(0xF3F4F6);

        scheme = scheme.trim().toLowerCase(Locale.ENGLISH);
        if (scheme.contains("warm")) return new Color(0xFFF7ED);
        if (scheme.contains("cool")) return new Color(0xEFF6FF);
        if (scheme.contains("mono")) return new Color(0xF5F5F5);
        if (scheme.contains("pastel")) return new Color(0xFDF2F8);
        return new Color(0xF3F4F6);
    }

    private Color schemeRoomBorder(RoomSpec spec) {
        String scheme = (spec == null) ? null : spec.getColorScheme();
        if (scheme == null) return new Color(0xD1D5DB);

        scheme = scheme.trim().toLowerCase(Locale.ENGLISH);
        if (scheme.contains("warm")) return new Color(0xFDBA74);
        if (scheme.contains("cool")) return new Color(0x93C5FD);
        if (scheme.contains("mono")) return new Color(0x9CA3AF);
        if (scheme.contains("pastel")) return new Color(0xF9A8D4);
        return new Color(0xD1D5DB);
    }

    /* ====================== Safe getters / icon helper ====================== */

    private String safeKind(FurnitureItem it) {
        try {
            Object k = it.getKind();
            if (k == null) return "CHAIR";
            String s = String.valueOf(k).trim();
            if (s.isBlank()) return "CHAIR";
            return s.toUpperCase(Locale.ENGLISH); // ✅ normalize
        } catch (Throwable ignored) {
            return "CHAIR";
        }
    }

    private String safeColorHex(FurnitureItem it) {
        try { return it.getColorHex(); } catch (Throwable ignored) { return null; }
    }

    private int safeShadingPercent(FurnitureItem it) {
        try {
            Integer v = it.getShadingPercent();
            if (v == null) return 0;
            return Math.max(0, Math.min(100, v));
        } catch (Throwable ignored) {
            return 0;
        }
    }

    private String pickIcon(String kind, boolean round, boolean chair) {
        // Prefer your enum icons if the kind matches
        try {
            FurnitureKind k = FurnitureKind.valueOf(kind);
            if (k != null && k.iconText != null && !k.iconText.isBlank()) return k.iconText;
        } catch (Throwable ignored) { }

        if (round) return "●";
        if ("TABLE_RECT".equals(kind)) return "▭";
        if (chair) return "🪑";
        return "⬚";
    }

    /* ====================== Cache builder (so clamp works before paint) ====================== */

    private void ensureRoomCache() {
        if (cachedRoomBoundsPx != null && cachedRoomShape != null) return;
        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0) return;

        int pad = 46;

        int roomW, roomH;

        int rx, ry;
        if (roomSpec != null && roomSpec.getWidth() > 0 && roomSpec.getLength() > 0) {
            int availW = w - pad * 2;
            int availH = h - pad * 2;

            // Normalize room dimensions to inches
            double rw = roomSpec.getWidth();
            double rl = roomSpec.getLength();
            
            // Assume "ft" if not specified or "ft"
            String unit = roomSpec.getUnit();
            if (unit == null || "ft".equalsIgnoreCase(unit.trim())) {
                rw *= 12.0;
                rl *= 12.0;
            } else if ("m".equalsIgnoreCase(unit.trim())) {
                rw *= 39.3701;
                rl *= 39.3701;
            }

            double roomRatio = rw / Math.max(0.0001, rl);
            double screenRatio = (double) availW / Math.max(1, availH);

            if (roomRatio > screenRatio) {
                roomW = availW;
                roomH = (int) (availW / roomRatio);
            } else {
                roomH = availH;
                roomW = (int) (availH * roomRatio);
            }
            
            rx = (w - roomW) / 2;
            ry = (h - roomH) / 2;
            
            cachedRoomBoundsPx = new Rectangle(rx, ry, roomW, roomH);
            cachedRoomShape = buildRoomShapePx(rx, ry, roomW, roomH, roomSpec);
            ppi = (double) roomW / rw;
        } else {
            roomW = Math.max(200, w - pad * 2);
            roomH = Math.max(200, h - pad * 2);
            rx = (w - roomW) / 2;
            ry = (h - roomH) / 2;
            cachedRoomBoundsPx = new Rectangle(rx, ry, roomW, roomH);
            cachedRoomShape = buildRoomShapePx(rx, ry, roomW, roomH, roomSpec);
            ppi = 1.0;
        }
    }

    private void paintHandle(Graphics2D g2, int x, int y, int size) {
        g2.setColor(Color.WHITE);
        g2.fillOval(x - size / 2, y - size / 2, size, size);
        g2.setColor(UiKit.PRIMARY);
        g2.setStroke(new BasicStroke(1.2f));
        g2.drawOval(x - size / 2, y - size / 2, size, size);
    }
}
