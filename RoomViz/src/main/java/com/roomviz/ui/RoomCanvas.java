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
import java.util.*;
import java.util.List;

public class RoomCanvas extends JPanel {

    private List<FurnitureItem> items = new ArrayList<>();
    private FurnitureItem selected = null;
    private RoomSpec roomSpec;

    private Point dragStartMouse = null;

    // drag start in RELATIVE coords (0..1 inside room bounds)
    private double dragStartRelX = 0;
    private double dragStartRelY = 0;

    // Room boundary (computed each paint from roomSpec + viewport scaling)
    private Shape cachedRoomShape = null;
    private Rectangle cachedRoomBoundsPx = null;

    private Runnable onSelectionChanged = null;

    // edit hooks (for undo snapshots + autosave)
    private Runnable onEditStart = null;
    private Runnable onEditCommit = null;

    // delete hook (keyboard delete)
    private Runnable onDeleteRequested = null;

    // ✅ Room cache safety
    private static final int MIN_ROOM_PX = 80;
    private static final int PAD = 46;

    // ✅ Canvas minimum size (prevents “window becomes a point”)
    private static final int MIN_CANVAS_W = 900;
    private static final int MIN_CANVAS_H = 600;

    /**
     * Stable storage of item geometry relative to the room bounds:
     * x,y,w,h are all 0..1 relative to room width/height.
     * This prevents drift + repeated scaling bugs.
     */
    private static class RelRect {
        double x, y, w, h;
        RelRect(double x, double y, double w, double h) {
            this.x = x; this.y = y; this.w = w; this.h = h;
        }
    }

    private final Map<String, RelRect> relById = new HashMap<>();

    public RoomCanvas() {
        this(null);
    }

    public RoomCanvas(RoomSpec spec) {
        this.roomSpec = spec;

        setOpaque(false);
        setFocusable(true);
        setLayout(null);
        setBorder(new EmptyBorder(10, 10, 10, 10));

        setMinimumSize(new Dimension(MIN_CANVAS_W, MIN_CANVAS_H));

        MouseAdapter ma = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                requestFocusInWindow();

                ensureRoomCache();
                syncRelFromPixelsIfMissing(); // make sure rel exists before hit test

                FurnitureItem hit = hitTest(e.getPoint());
                setSelected(hit);

                if (hit != null) {
                    if (onEditStart != null) onEditStart.run();

                    dragStartMouse = e.getPoint();
                    RelRect rr = relById.get(safeId(hit));
                    if (rr != null) {
                        dragStartRelX = rr.x;
                        dragStartRelY = rr.y;
                    }
                } else {
                    dragStartMouse = null;
                }

                repaint();
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                if (selected == null || dragStartMouse == null) return;
                ensureRoomCache();
                if (cachedRoomBoundsPx == null) return;

                RelRect rr = relById.get(safeId(selected));
                if (rr == null) return;

                int dx = e.getX() - dragStartMouse.x;
                int dy = e.getY() - dragStartMouse.y;

                double roomW = Math.max(1, cachedRoomBoundsPx.getWidth());
                double roomH = Math.max(1, cachedRoomBoundsPx.getHeight());

                // convert pixel delta -> relative delta
                double dRelX = dx / roomW;
                double dRelY = dy / roomH;

                rr.x = dragStartRelX + dRelX;
                rr.y = dragStartRelY + dRelY;

                clampRelIntoRoomShape(selected, rr);

                // update item pixel fields so property panel stays in sync
                syncOneItemPixelsFromRel(selected, rr);

                repaint();
                fireSelectionChanged();
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                ensureRoomCache();

                if (selected != null) {
                    RelRect rr = relById.get(safeId(selected));
                    if (rr != null) {
                        clampRelIntoRoomShape(selected, rr);
                        syncOneItemPixelsFromRel(selected, rr);
                    }
                    repaint();
                    fireSelectionChanged();

                    if (onEditCommit != null) onEditCommit.run();
                }

                dragStartMouse = null;
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

        // ✅ On resize: do NOT scale the model.
        // Just rebuild cache and repaint. Rel coords stay stable.
        addComponentListener(new ComponentAdapter() {
            @Override public void componentResized(ComponentEvent e) {
                cachedRoomBoundsPx = null;
                cachedRoomShape = null;
                ensureRoomCache();
                syncAllPixelsFromRel(); // update displayed pixel numbers
                repaint();
            }
        });
    }

    @Override
    public Dimension getMinimumSize() {
        return new Dimension(MIN_CANVAS_W, MIN_CANVAS_H);
    }

    public void setRoomSpec(RoomSpec spec) {
        this.roomSpec = spec;
        cachedRoomBoundsPx = null;
        cachedRoomShape = null;

        ensureRoomCache();
        syncRelFromPixelsIfMissing();
        syncAllPixelsFromRel();

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

        ensureRoomCache();
        syncRelFromPixelsIfMissing();
        syncAllPixelsFromRel();

        repaint();
        fireSelectionChanged();
    }

    public FurnitureItem getSelected() { return selected; }

    public void addItemFromTemplate(FurnitureTemplate t) {
        if (t == null) return;

        ensureRoomCache();
        if (cachedRoomBoundsPx == null) return;

        int defW = Math.max(20, t.defaultW);
        int defH = Math.max(20, t.defaultH);

        // center in room (pixel), then convert to rel
        int cx = cachedRoomBoundsPx.x + cachedRoomBoundsPx.width / 2 - defW / 2;
        int cy = cachedRoomBoundsPx.y + cachedRoomBoundsPx.height / 2 - defH / 2;

        FurnitureItem it = new FurnitureItem(
                UUID.randomUUID().toString(),
                t.name,
                (t.kind == null ? "CHAIR" : t.kind.name()),
                cx, cy, defW, defH
        );

        items.add(it);

        // create relative rect
        RelRect rr = pixelRectToRel(it);
        relById.put(safeId(it), rr);

        // clamp to room shape, sync pixels back
        clampRelIntoRoomShape(it, rr);
        syncOneItemPixelsFromRel(it, rr);

        setSelected(it);

        repaint();
        if (onEditCommit != null) onEditCommit.run();
    }

    public void setSelected(FurnitureItem it) {
        selected = it;

        ensureRoomCache();
        syncRelFromPixelsIfMissing();
        if (selected != null) {
            RelRect rr = relById.get(safeId(selected));
            if (rr != null) syncOneItemPixelsFromRel(selected, rr);
        }

        fireSelectionChanged();
        repaint();
    }

    public void fireSelectionChanged() {
        if (onSelectionChanged != null) onSelectionChanged.run();
    }

    public FurnitureItem hitTest(Point p) {
        if (p == null) return null;

        ensureRoomCache();
        syncRelFromPixelsIfMissing();

        for (int i = items.size() - 1; i >= 0; i--) {
            FurnitureItem it = items.get(i);
            if (it == null) continue;

            Rectangle r = getItemPixelRect(it);
            if (r.contains(p)) return it;
        }
        return null;
    }

    public void setSelectedPosition(int x, int y) {
        if (selected == null) return;

        ensureRoomCache();
        if (cachedRoomBoundsPx == null) return;

        RelRect rr = relById.get(safeId(selected));
        if (rr == null) {
            rr = pixelRectToRel(selected);
            relById.put(safeId(selected), rr);
        }

        // convert requested pixel pos -> rel pos
        rr.x = (x - cachedRoomBoundsPx.getX()) / Math.max(1, cachedRoomBoundsPx.getWidth());
        rr.y = (y - cachedRoomBoundsPx.getY()) / Math.max(1, cachedRoomBoundsPx.getHeight());

        clampRelIntoRoomShape(selected, rr);
        syncOneItemPixelsFromRel(selected, rr);

        repaint();
        fireSelectionChanged();
        if (onEditCommit != null) onEditCommit.run();
    }

    public void setSelectedSize(int w, int h) {
        if (selected == null) return;

        ensureRoomCache();
        if (cachedRoomBoundsPx == null) return;

        RelRect rr = relById.get(safeId(selected));
        if (rr == null) {
            rr = pixelRectToRel(selected);
            relById.put(safeId(selected), rr);
        }

        rr.w = w / Math.max(1, cachedRoomBoundsPx.getWidth());
        rr.h = h / Math.max(1, cachedRoomBoundsPx.getHeight());

        // keep usable
        rr.w = Math.max(10.0 / cachedRoomBoundsPx.getWidth(), rr.w);
        rr.h = Math.max(10.0 / cachedRoomBoundsPx.getHeight(), rr.h);

        clampRelIntoRoomShape(selected, rr);
        syncOneItemPixelsFromRel(selected, rr);

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
        relById.remove(safeId(selected));
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

        ensureRoomCache();
        syncRelFromPixelsIfMissing();
        syncAllPixelsFromRel(); // keep pixel fields consistent for UI panels

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
        if (it == null) return;

        Graphics2D g2 = (Graphics2D) parentG.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        Rectangle r = getItemPixelRect(it);

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

        int shade = safeShadingPercent(it);
        if (shade > 0) {
            int alpha = Math.max(0, Math.min(180, (int) Math.round(180.0 * (shade / 100.0))));
            g2.setColor(new Color(0, 0, 0, alpha));
            if (round) g2.fillOval(r.x, r.y, r.width, r.height);
            else if (chair) g2.fillRoundRect(r.x, r.y, r.width, r.height, 6, 6);
            else g2.fillRoundRect(r.x, r.y, r.width, r.height, 8, 8);
        }

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

        if (isSel) {
            g2.setColor(new Color(0x2563EB));
            g2.setStroke(new BasicStroke(2.0f));
            int pad = 4;
            if (round) g2.drawOval(r.x - pad / 2, r.y - pad / 2, r.width + pad, r.height + pad);
            else g2.drawRoundRect(r.x - pad / 2, r.y - pad / 2, r.width + pad, r.height + pad, 10, 10);
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

    /* ====================== RELATIVE <-> PIXEL SYNC ====================== */

    private String safeId(FurnitureItem it) {
        try {
            String id = it.getId();
            if (id != null && !id.isBlank()) return id;
        } catch (Throwable ignored) {}
        // fallback: stable-ish key using object identity if model has no id
        return "OBJ@" + System.identityHashCode(it);
    }

    private void syncRelFromPixelsIfMissing() {
        if (cachedRoomBoundsPx == null) return;
        for (FurnitureItem it : items) {
            if (it == null) continue;
            String id = safeId(it);
            if (!relById.containsKey(id)) {
                relById.put(id, pixelRectToRel(it));
            }
        }
    }

    private RelRect pixelRectToRel(FurnitureItem it) {
        ensureRoomCache();
        if (cachedRoomBoundsPx == null) return new RelRect(0.1, 0.1, 0.2, 0.2);

        double roomW = Math.max(1, cachedRoomBoundsPx.getWidth());
        double roomH = Math.max(1, cachedRoomBoundsPx.getHeight());

        double px = safeInt(it.getX());
        double py = safeInt(it.getY());
        double pw = Math.max(1, safeInt(it.getW()));
        double ph = Math.max(1, safeInt(it.getH()));

        double rx = (px - cachedRoomBoundsPx.getX()) / roomW;
        double ry = (py - cachedRoomBoundsPx.getY()) / roomH;
        double rw = pw / roomW;
        double rh = ph / roomH;

        return new RelRect(rx, ry, rw, rh);
    }

    private Rectangle getItemPixelRect(FurnitureItem it) {
        ensureRoomCache();
        if (cachedRoomBoundsPx == null) {
            return new Rectangle(safeInt(it.getX()), safeInt(it.getY()),
                    Math.max(1, safeInt(it.getW())), Math.max(1, safeInt(it.getH())));
        }

        RelRect rr = relById.get(safeId(it));
        if (rr == null) {
            rr = pixelRectToRel(it);
            relById.put(safeId(it), rr);
        }

        double roomW = Math.max(1, cachedRoomBoundsPx.getWidth());
        double roomH = Math.max(1, cachedRoomBoundsPx.getHeight());

        int x = (int) Math.round(cachedRoomBoundsPx.getX() + rr.x * roomW);
        int y = (int) Math.round(cachedRoomBoundsPx.getY() + rr.y * roomH);
        int w = (int) Math.round(rr.w * roomW);
        int h = (int) Math.round(rr.h * roomH);

        w = Math.max(10, w);
        h = Math.max(10, h);

        return new Rectangle(x, y, w, h);
    }

    private void syncOneItemPixelsFromRel(FurnitureItem it, RelRect rr) {
        if (it == null || rr == null) return;
        Rectangle r = getItemPixelRect(it);
        it.setX(r.x);
        it.setY(r.y);
        it.setW(r.width);
        it.setH(r.height);
    }

    private void syncAllPixelsFromRel() {
        for (FurnitureItem it : items) {
            if (it == null) continue;
            RelRect rr = relById.get(safeId(it));
            if (rr != null) syncOneItemPixelsFromRel(it, rr);
        }
    }

    private void clampRelIntoRoomShape(FurnitureItem it, RelRect rr) {
        ensureRoomCache();
        if (cachedRoomBoundsPx == null || rr == null) return;

        // first clamp to rectangular bounds (0..1)
        rr.w = Math.max(rr.w, 10.0 / Math.max(1, cachedRoomBoundsPx.getWidth()));
        rr.h = Math.max(rr.h, 10.0 / Math.max(1, cachedRoomBoundsPx.getHeight()));

        rr.x = clamp(rr.x, 0, 1 - rr.w);
        rr.y = clamp(rr.y, 0, 1 - rr.h);

        // then if L-shape: try nudging until pixel rect fits inside shape
        Rectangle px = getItemPixelRect(it);

        int tries = 0;
        while (!rectInsideRoom(px) && tries < 250) {
            // nudge left / down a bit
            rr.x = clamp(rr.x - 0.005, 0, 1 - rr.w);
            rr.y = clamp(rr.y + 0.005, 0, 1 - rr.h);
            px = getItemPixelRect(it);
            tries++;
        }
    }

    private double clamp(double v, double lo, double hi) {
        return Math.max(lo, Math.min(hi, v));
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
            return s.toUpperCase(Locale.ENGLISH);
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
        try {
            FurnitureKind k = FurnitureKind.valueOf(kind);
            if (k != null && k.iconText != null && !k.iconText.isBlank()) return k.iconText;
        } catch (Throwable ignored) { }

        if (round) return "●";
        if ("TABLE_RECT".equals(kind)) return "▭";
        if (chair) return "🪑";
        return "⬚";
    }

    /* ====================== Cache builder ====================== */

    private void ensureRoomCache() {
        if (cachedRoomBoundsPx != null && cachedRoomShape != null) return;

        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0) return;

        // keep available area sensible
        int availW = Math.max(MIN_ROOM_PX, w - PAD * 2);
        int availH = Math.max(MIN_ROOM_PX, h - PAD * 2);

        int roomW, roomH;

        if (roomSpec != null && roomSpec.getWidth() > 0 && roomSpec.getLength() > 0) {
            double rw = roomSpec.getWidth();
            double rl = roomSpec.getLength();

            double roomRatio = rw / Math.max(0.0001, rl);
            double screenRatio = (double) availW / Math.max(1, availH);

            if (roomRatio > screenRatio) {
                roomW = availW;
                roomH = (int) Math.round(availW / roomRatio);
            } else {
                roomH = availH;
                roomW = (int) Math.round(availH * roomRatio);
            }
        } else {
            roomW = availW;
            roomH = availH;
        }

        roomW = Math.max(MIN_ROOM_PX, roomW);
        roomH = Math.max(MIN_ROOM_PX, roomH);

        int rx = (w - roomW) / 2;
        int ry = (h - roomH) / 2;

        cachedRoomBoundsPx = new Rectangle(rx, ry, roomW, roomH);
        cachedRoomShape = buildRoomShapePx(rx, ry, roomW, roomH, roomSpec);
    }
}
