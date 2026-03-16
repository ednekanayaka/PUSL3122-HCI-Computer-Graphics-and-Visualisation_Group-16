package com.roomviz.ui;

import com.roomviz.model.Design;
import com.roomviz.model.FurnitureItem;
import com.roomviz.model.FurnitureKind;
import com.roomviz.model.RoomSpec;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class Mini2DPreviewPanel extends JPanel {

    private Design design;

    public Mini2DPreviewPanel() {
        setOpaque(true);
        setBackground(new Color(0xF9FAFB));
    }

    public void setDesign(Design d) {
        this.design = d;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int W = getWidth();
        int H = getHeight();

        // background
        g2.setColor(getBackground());
        g2.fillRect(0, 0, W, H);

        if (design == null) {
            g2.setColor(new Color(0, 0, 0, 120));
            g2.setFont(g2.getFont().deriveFont(Font.PLAIN, 12f));
            g2.drawString("No design selected", 16, 22);
            g2.dispose();
            return;
        }

        // ---- Preview room rect (where the room is drawn inside this panel) ----
        int pad = 18;
        int headerSpace = 0;
        int availW = Math.max(1, W - pad * 2);
        int availH = Math.max(1, H - pad * 2 - headerSpace);

        // Use room aspect ratio from RoomSpec if available, else fallback square
        double rw = 520, rh = 520;
        try {
            if (design.getRoomSpec() != null
                    && design.getRoomSpec().getWidth() > 0
                    && design.getRoomSpec().getLength() > 0) {
                rw = design.getRoomSpec().getWidth();
                rh = design.getRoomSpec().getLength();
            }
        } catch (Exception ignored) {}

        double scale = Math.min(availW / rw, availH / rh);
        scale = Math.max(0.1, Math.min(10, scale));

        int roomW = (int) Math.round(rw * scale);
        int roomH = (int) Math.round(rh * scale);

        int roomX = pad + (availW - roomW) / 2;
        int roomY = pad + headerSpace + (availH - roomH) / 2;

        // ---- Draw grid ----
        g2.setColor(new Color(0, 0, 0, 10));
        int step = (int) Math.max(12, Math.round(30 * scale));
        for (int x = roomX; x <= roomX + roomW; x += step) g2.drawLine(x, roomY, x, roomY + roomH);
        for (int y = roomY; y <= roomY + roomH; y += step) g2.drawLine(roomX, y, roomX + roomW, y);

        // ---- Draw room (supports L-Shape like RoomCanvas) ----
        RoomSpec spec = null;
        try { spec = design.getRoomSpec(); } catch (Throwable ignored) {}

        Shape roomShape = buildRoomShapePx(roomX, roomY, roomW, roomH, spec);

        g2.setColor(Color.WHITE);
        g2.fill(roomShape);

        g2.setColor(new Color(0xCBD5E1));
        g2.setStroke(new BasicStroke(1.2f));
        g2.draw(roomShape);

        // ---- Need layout bounds saved from planner ----
        double lx = safeLayoutX(design, 0);
        double ly = safeLayoutY(design, 0);
        double lw = safeLayoutW(design, 520);
        double lh = safeLayoutH(design, 520);

        // FurnitureItem has NO layer, so sort by Y (natural overlap)
        List<FurnitureItem> items = (design.getItems() == null) ? List.of() : design.getItems();
        List<FurnitureItem> sorted = new ArrayList<>(items);
        sorted.sort(Comparator.comparingInt(FurnitureItem::getY));

        // ---- Draw furniture using normalized mapping ----
        for (FurnitureItem it : sorted) {
            if (it == null) continue;

            double px = it.getX();
            double py = it.getY();
            double pw = Math.max(10, it.getW());
            double ph = Math.max(10, it.getH());

            double nx = ((px + pw / 2.0) - lx) / Math.max(1, lw);
            double ny = ((py + ph / 2.0) - ly) / Math.max(1, lh);
            double nw = pw / Math.max(1, lw);
            double nh = ph / Math.max(1, lh);

            double cx = roomX + nx * roomW;
            double cy = roomY + ny * roomH;
            double w = nw * roomW;
            double h = nh * roomH;

            double drawX = cx - w / 2.0;
            double drawY = cy - h / 2.0;

            Color base = parseHex(it.getColorHex(), new Color(0x3B82F6));
            int shade = clamp(it.getShadingPercent(), 0, 100);
            double mul = 0.75 + (shade / 100.0) * 0.4;
            base = applyBrightness(base, mul);

            paintItem(g2, it, drawX, drawY, w, h, base);
        }

        g2.dispose();
    }

    /* ===== furniture renderer ===== */

    private void paintItem(Graphics2D parentG, FurnitureItem it,
                           double x, double y, double w, double h,
                           Color base) {
        Graphics2D g2 = (Graphics2D) parentG.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        Rectangle r = new Rectangle(
                (int) Math.round(x),
                (int) Math.round(y),
                Math.max(1, (int) Math.round(w)),
                Math.max(1, (int) Math.round(h))
        );

        // rotation
        int rot = 0;
        try { rot = it.getRotation(); } catch (Throwable ignored) {}
        rot = ((rot % 360) + 360) % 360;
        if (rot != 0) {
            double cx = r.getX() + r.getWidth() / 2.0;
            double cy = r.getY() + r.getHeight() / 2.0;
            // Flip rotation direction in 2D so it matches 3D convention
            g2.rotate(Math.toRadians(-rot), cx, cy);
        }

        String kind = safeKind(it);
        boolean round = "TABLE_ROUND".equals(kind);
        boolean chair = "CHAIR".equals(kind);

        g2.setColor(base);
        if (round) {
            g2.fillOval(r.x, r.y, r.width, r.height);
            g2.setColor(new Color(0, 0, 0, 55));
            g2.setStroke(new BasicStroke(1f));
            g2.drawOval(r.x, r.y, r.width, r.height);
        } else if (chair) {
            int backThick = Math.max(3, r.height / 6);
            int gap = 2;

            int bx = r.x, by = r.y, bw = r.width, bh = backThick;
            int sx = r.x, sy = r.y + backThick + gap, sw = r.width, sh = r.height - (backThick + gap);

            g2.setColor(base);
            g2.fillRoundRect(bx, by, bw, bh, 4, 4);
            g2.fillRoundRect(sx, sy, sw, sh, 4, 4);

            g2.setColor(new Color(0, 0, 0, 55));
            g2.drawRoundRect(bx, by, bw, bh, 4, 4);
            g2.drawRoundRect(sx, sy, sw, sh, 4, 4);
        } else {
            g2.fillRoundRect(r.x, r.y, r.width, r.height, 8, 8);
            g2.setColor(new Color(0, 0, 0, 55));
            g2.setStroke(new BasicStroke(1f));
            g2.drawRoundRect(r.x, r.y, r.width, r.height, 8, 8);
        }

        // icon text
        String icon = pickIcon(kind);
        int minDim = Math.min(r.width, r.height);
        float fontSize = Math.max(10f, Math.min(22f, minDim * 0.55f));
        g2.setFont(g2.getFont().deriveFont(Font.BOLD, fontSize));
        FontMetrics fm = g2.getFontMetrics();

        int tx = r.x + (r.width - fm.stringWidth(icon)) / 2;
        int ty = r.y + (r.height + fm.getAscent()) / 2 - (int) (fm.getDescent() * 0.45);

        g2.setColor(new Color(0, 0, 0, 70));
        g2.drawString(icon, tx + 1, ty + 1);
        g2.setColor(Color.WHITE);
        g2.drawString(icon, tx, ty);

        g2.dispose();
    }

    private String safeKind(FurnitureItem it) {
        try {
            Object k = it.getKind();
            if (k == null) return "CHAIR";
            String s = String.valueOf(k).trim();
            if (s.isBlank()) return "CHAIR";
            return s.toUpperCase(java.util.Locale.ENGLISH);
        } catch (Throwable ignored) {
            return "CHAIR";
        }
    }

    private String pickIcon(String kind) {
        try {
            FurnitureKind k = FurnitureKind.valueOf(kind);
            if (k != null && k.iconText != null && !k.iconText.isBlank()) return k.iconText;
        } catch (Throwable ignored) {}

        if ("TABLE_ROUND".equals(kind)) return "●";
        if ("TABLE_RECT".equals(kind)) return "▭";
        if ("CHAIR".equals(kind)) return "🪑";
        return "⬚";
    }

    /* ===== room shape helpers ===== */

    private boolean isLShape(RoomSpec spec) {
        if (spec == null) return false;
        String s = spec.getShape();
        if (s == null) return false;
        return "L-Shape".equalsIgnoreCase(s.trim()) && spec.getLCutWidth() > 0 && spec.getLCutLength() > 0;
    }

    private Shape buildRoomShapePx(int rx, int ry, int roomW, int roomH, RoomSpec spec) {
        if (!isLShape(spec)) {
            return new RoundRectangle2D.Double(rx, ry, roomW, roomH, 12, 12);
        }
        return buildLShape(rx, ry, roomW, roomH, spec);
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

    /* ===== helpers ===== */

    private int clamp(int v, int a, int b) { return Math.max(a, Math.min(b, v)); }

    private Color parseHex(String hex, Color fallback) {
        try {
            String h = (hex == null) ? "" : hex.trim();
            if (h.isEmpty()) return fallback;
            if (!h.startsWith("#")) h = "#" + h;
            return Color.decode(h);
        } catch (Exception e) {
            return fallback;
        }
    }

    private Color applyBrightness(Color c, double mul) {
        int r = clamp((int) Math.round(c.getRed() * mul), 0, 255);
        int g = clamp((int) Math.round(c.getGreen() * mul), 0, 255);
        int b = clamp((int) Math.round(c.getBlue() * mul), 0, 255);
        return new Color(r, g, b, c.getAlpha());
    }

    private double safeLayoutX(Design d, double fallback) {
        try { Integer v = d.getLayoutX(); return (v == null) ? fallback : v.doubleValue(); }
        catch (Throwable ignored) {}
        return fallback;
    }

    private double safeLayoutY(Design d, double fallback) {
        try { Integer v = d.getLayoutY(); return (v == null) ? fallback : v.doubleValue(); }
        catch (Throwable ignored) {}
        return fallback;
    }

    private double safeLayoutW(Design d, double fallback) {
        try { Integer v = d.getLayoutWidth(); return (v == null || v <= 0) ? fallback : v.doubleValue(); }
        catch (Throwable ignored) {}
        return fallback;
    }

    private double safeLayoutH(Design d, double fallback) {
        try { Integer v = d.getLayoutHeight(); return (v == null || v <= 0) ? fallback : v.doubleValue(); }
        catch (Throwable ignored) {}
        return fallback;
    }
}