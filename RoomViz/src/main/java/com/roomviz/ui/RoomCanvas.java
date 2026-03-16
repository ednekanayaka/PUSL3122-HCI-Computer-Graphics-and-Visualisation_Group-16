package com.roomviz.ui;

import com.roomviz.model.FurnitureItem;
import com.roomviz.model.FurnitureTemplate;
import com.roomviz.model.RoomSpec;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Path2D;
import java.util.*;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

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

    //  Saved layout bounds from the Design (used to convert old pixel coords reliably on load)
    // If a design was saved in fullscreen, and opened in a smaller window, this prevents "push out" drift.
    private Rectangle legacyLayoutBoundsPx = null;

    private Runnable onSelectionChanged = null;

    // edit hooks (for undo snapshots + autosave)
    private Runnable onEditStart = null;
    private Runnable onEditCommit = null;

    // delete hook (keyboard delete)
    private Runnable onDeleteRequested = null;

    //  Room cache safety
    private static final int MIN_ROOM_PX = 80;
    private static final int PAD = 46;

    //  Canvas minimum size (prevents “window becomes a point”)
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

        //  On resize: do NOT scale the model.
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

    // called by Planner2DPage before loading items
    public void setLegacyLayoutBounds(Rectangle legacyBounds) {
        this.legacyLayoutBoundsPx = legacyBounds;
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

        // When replacing the whole list (load / undo / redo), the rel map must match the restored pixels.
        // Otherwise, syncAllPixelsFromRel() will overwrite restored x/y with stale rel values.
        relById.clear();

        if (selected != null && !this.items.contains(selected)) selected = null;

        ensureRoomCache();
        syncRelFromPixelsIfMissing();   // repopulates relById from current pixel x/y/w/h
        syncAllPixelsFromRel();         // keeps pixel fields consistent with rel + current room bounds

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

        // create relative rect (uses current bounds, not legacy)
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

        // convert requested pixel pos -> rel pos (always current)
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

    // Used by the new "Placed Items" side list
    public void deleteItem(FurnitureItem it) {
        if (it == null) return;
        relById.remove(safeId(it));
        items.remove(it);
        if (selected == it) selected = null;
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
        g2.setColor(UiKit.isDarkBlueMode() ? new Color(0x1F2937) : new Color(0xEEF2F7));
        int step = 24;
        for (int x = 0; x < w; x += step) g2.drawLine(x, 0, x, h);
        for (int y = 0; y < h; y += step) g2.drawLine(0, y, w, y);
    }

    private void paintRulers(Graphics2D g2, int w, int h) {
        g2.setColor(UiKit.isDarkBlueMode() ? new Color(0x0F172A) : new Color(0xE5E7EB));
        g2.fillRect(0, 0, w, 28);
        g2.fillRect(0, 0, 28, h);

        g2.setColor(UiKit.isDarkBlueMode() ? new Color(0x94A3B8) : new Color(0x9CA3AF));
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

        g2.setColor(UiKit.isDarkBlueMode() ? new Color(0x334155) : new Color(0xD1D5DB));
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
            //  FIX: Flip rotation direction in 2D so it matches 3D convention
            g2.rotate(Math.toRadians(-rot), cx, cy);
        }

        Color base = parseHexOrDefault(safeColorHex(it), new Color(0x3B82F6));
        String kind = safeKind(it);

        // Draw top-down “actual” 2D shapes (instead of generic boxes + emoji)
        if ("TABLE_ROUND".equals(kind)) {
            drawRoundTableTopDown(g2, r, base);
        } else if ("TABLE_RECT".equals(kind)) {
            drawRectTableTopDown(g2, r, base);
        } else if ("CHAIR".equals(kind)) {
            drawChairTopDown(g2, r, base);
        } else {
            // fallback
            g2.setColor(base);
            g2.fillRoundRect(r.x, r.y, r.width, r.height, 12, 12);
            g2.setColor(new Color(0, 0, 0, 55));
            g2.setStroke(new BasicStroke(1f));
            g2.drawRoundRect(r.x, r.y, r.width, r.height, 12, 12);
        }

        // shading overlay (darken)
        int shade = safeShadingPercent(it);
        if (shade > 0) {
            int alpha = Math.max(0, Math.min(180, (int) Math.round(180.0 * (shade / 100.0))));
            g2.setColor(new Color(0, 0, 0, alpha));
            // overlay the bounding rect (keeps it simple + consistent)
            g2.fillRoundRect(r.x, r.y, r.width, r.height, 12, 12);
        }

        if (isSel) {
            g2.setColor(UiKit.PRIMARY);
            g2.setStroke(new BasicStroke(2.0f));
            int pad = 4;
            if ("TABLE_ROUND".equals(kind)) {
                g2.drawOval(r.x - pad / 2, r.y - pad / 2, r.width + pad, r.height + pad);
            } else {
                g2.drawRoundRect(r.x - pad / 2, r.y - pad / 2, r.width + pad, r.height + pad, 12, 12);
            }
        }

        g2.dispose();
    }

    // ======= Top-down furniture drawings (2D planner) =======

    private void drawChairTopDown(Graphics2D g2, Rectangle r, Color base) {
        // seat
        int inset = Math.max(3, Math.min(r.width, r.height) / 8);
        Rectangle seat = new Rectangle(
                r.x + inset,
                r.y + inset * 2,
                Math.max(1, r.width - inset * 2),
                Math.max(1, r.height - inset * 3)
        );

        // backrest (top side)
        int backH = Math.max(4, r.height / 5);
        Rectangle back = new Rectangle(
                r.x + inset,
                r.y + inset,
                Math.max(1, r.width - inset * 2),
                backH
        );

        g2.setColor(base);
        g2.fillRoundRect(seat.x, seat.y, seat.width, seat.height, 10, 10);
        g2.fillRoundRect(back.x, back.y, back.width, back.height, 10, 10);

        // legs (small circles)
        int leg = Math.max(3, Math.min(r.width, r.height) / 10);
        g2.setColor(new Color(0, 0, 0, 45));
        g2.fillOval(r.x + 2, r.y + 2, leg, leg);
        g2.fillOval(r.x + r.width - leg - 2, r.y + 2, leg, leg);
        g2.fillOval(r.x + 2, r.y + r.height - leg - 2, leg, leg);
        g2.fillOval(r.x + r.width - leg - 2, r.y + r.height - leg - 2, leg, leg);

        // outline
        g2.setColor(new Color(0, 0, 0, 55));
        g2.setStroke(new BasicStroke(1f));
        g2.drawRoundRect(seat.x, seat.y, seat.width, seat.height, 10, 10);
        g2.drawRoundRect(back.x, back.y, back.width, back.height, 10, 10);
    }

    private void drawRectTableTopDown(Graphics2D g2, Rectangle r, Color base) {
        int inset = Math.max(3, Math.min(r.width, r.height) / 10);
        Rectangle top = new Rectangle(
                r.x + inset,
                r.y + inset,
                Math.max(1, r.width - inset * 2),
                Math.max(1, r.height - inset * 2)
        );

        // tabletop
        g2.setColor(base);
        g2.fillRoundRect(top.x, top.y, top.width, top.height, 16, 16);

        // legs
        int leg = Math.max(4, Math.min(r.width, r.height) / 8);
        g2.setColor(new Color(0, 0, 0, 35));
        g2.fillRoundRect(r.x + 2, r.y + 2, leg, leg, 8, 8);
        g2.fillRoundRect(r.x + r.width - leg - 2, r.y + 2, leg, leg, 8, 8);
        g2.fillRoundRect(r.x + 2, r.y + r.height - leg - 2, leg, leg, 8, 8);
        g2.fillRoundRect(r.x + r.width - leg - 2, r.y + r.height - leg - 2, leg, leg, 8, 8);

        // outline
        g2.setColor(new Color(0, 0, 0, 55));
        g2.setStroke(new BasicStroke(1f));
        g2.drawRoundRect(top.x, top.y, top.width, top.height, 16, 16);
    }

    private void drawRoundTableTopDown(Graphics2D g2, Rectangle r, Color base) {
        g2.setColor(base);
        g2.fillOval(r.x, r.y, r.width, r.height);

        // inner ring
        g2.setColor(new Color(255, 255, 255, 35));
        int ringInset = Math.max(4, Math.min(r.width, r.height) / 8);
        g2.setStroke(new BasicStroke(1.2f));
        g2.drawOval(r.x + ringInset, r.y + ringInset, r.width - ringInset * 2, r.height - ringInset * 2);

        // outline
        g2.setColor(new Color(0, 0, 0, 55));
        g2.setStroke(new BasicStroke(1f));
        g2.drawOval(r.x, r.y, r.width, r.height);
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

    private Rectangle baseBoundsForPixelToRel() {
        if (legacyLayoutBoundsPx != null && legacyLayoutBoundsPx.width > 0 && legacyLayoutBoundsPx.height > 0) {
            return legacyLayoutBoundsPx;
        }
        return cachedRoomBoundsPx;
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

        // IMPORTANT: legacy bounds are only meant for the initial load conversion
        // Once rel coords exist, we can clear it so future conversions use current room bounds.
        legacyLayoutBoundsPx = null;
    }

    private RelRect pixelRectToRel(FurnitureItem it) {
        ensureRoomCache();

        Rectangle base = baseBoundsForPixelToRel();
        if (base == null) return new RelRect(0.1, 0.1, 0.2, 0.2);

        double roomW = Math.max(1, base.getWidth());
        double roomH = Math.max(1, base.getHeight());

        double px = safeInt(it.getX());
        double py = safeInt(it.getY());
        double pw = Math.max(1, safeInt(it.getW()));
        double ph = Math.max(1, safeInt(it.getH()));

        double rx = (px - base.getX()) / roomW;
        double ry = (py - base.getY()) / roomH;
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
        if (UiKit.isDarkBlueMode()) {
            if (scheme == null) return new Color(0x1E293B);
            scheme = scheme.trim().toLowerCase(Locale.ENGLISH);
            if (scheme.contains("warm")) return new Color(0x3A2E27);
            if (scheme.contains("cool")) return new Color(0x1E3A5F);
            if (scheme.contains("mono")) return new Color(0x2F3645);
            if (scheme.contains("pastel")) return new Color(0x3D2E45);
            return new Color(0x1E293B);
        }
        if (scheme == null) return UiKit.META_PILL_BG;

        scheme = scheme.trim().toLowerCase(Locale.ENGLISH);
        if (scheme.contains("warm")) return new Color(0xFFF7ED);
        if (scheme.contains("cool")) return new Color(0xEFF6FF);
        if (scheme.contains("mono")) return new Color(0xF5F5F5);
        if (scheme.contains("pastel")) return new Color(0xFDF2F8);
        return UiKit.META_PILL_BG;
    }

    private Color schemeRoomBorder(RoomSpec spec) {
        String scheme = (spec == null) ? null : spec.getColorScheme();
        if (UiKit.isDarkBlueMode()) {
            if (scheme == null) return new Color(0x475569);
            scheme = scheme.trim().toLowerCase(Locale.ENGLISH);
            if (scheme.contains("warm")) return new Color(0xB45309);
            if (scheme.contains("cool")) return new Color(0x3B82F6);
            if (scheme.contains("mono")) return new Color(0x64748B);
            if (scheme.contains("pastel")) return new Color(0xA855F7);
            return new Color(0x475569);
        }
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
            return Math.max(0, Math.min(100, v));
        } catch (Throwable ignored) {
            return 0;
        }
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
