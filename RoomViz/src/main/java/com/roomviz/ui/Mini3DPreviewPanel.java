package com.roomviz.ui;

import com.roomviz.model.Design;
import com.roomviz.model.FurnitureItem;
import com.roomviz.model.FurnitureKind;
import com.roomviz.model.RoomSpec;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Path2D;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class Mini3DPreviewPanel extends JPanel {

    private Design design;

    public Mini3DPreviewPanel() {
        setOpaque(true);
        setBackground(new Color(0x0B1220));
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

        paintBackground(g2, W, H);

        if (design == null) {
            g2.setColor(new Color(255, 255, 255, 180));
            g2.setFont(g2.getFont().deriveFont(Font.PLAIN, 12.0f));
            g2.drawString("No design selected", 16, 22);
            g2.dispose();
            return;
        }

        RoomSpec spec = safeRoomSpec(design);

        // Determine logical room plane size (prefer real room spec if available)
        double roomW = safeRoomW(spec, safeLayoutW(design, 520));
        double roomD = safeRoomD(spec, safeLayoutH(design, 520));

        // Isometric constants
        double cos = Math.cos(Math.toRadians(45));
        double sin = Math.sin(Math.toRadians(45));

        // Fit floor into panel
        double isoSpanX = (roomW + roomD) * cos;
        double isoSpanY = (roomW + roomD) * sin;

        double margin = 26;
        double scale = Math.min((W - margin * 2) / Math.max(1e-6, isoSpanX),
                (H - margin * 2) / Math.max(1e-6, isoSpanY));
        scale = Math.max(0.25, Math.min(3.0, scale));

        double cx = W / 2.0;
        double cy = H / 2.0 + 26; // push down a bit so floor feels grounded

        // ---- Floor (supports L-Shape) ----
        Shape floor = buildFloorShape(spec, roomW, roomD, cos, sin, scale, cx, cy);

        g2.setColor(new Color(0x111827));
        g2.fill(floor);

        g2.setColor(new Color(255, 255, 255, 32));
        g2.setStroke(new BasicStroke(1.2f));
        g2.draw(floor);

        // Furniture blocks (sorted back-to-front by (x+z))
        List<FurnitureItem> items = (design.getItems() == null) ? List.of() : design.getItems();
        List<FurnitureItem> sorted = new ArrayList<>(items);
        sorted.sort(Comparator.comparingDouble(it -> {
            RoomItem r = mapItemToRoom(it, design, roomW, roomD);
            return r.cx + r.cz;
        }));

        for (FurnitureItem it : sorted) {
            RoomItem r = mapItemToRoom(it, design, roomW, roomD);
            drawFurniture(g2, r, it, cos, sin, scale, cx, cy);
        }

        // Tiny label
        g2.setColor(new Color(255, 255, 255, 160));
        g2.setFont(g2.getFont().deriveFont(Font.PLAIN, 11.0f));
        g2.drawString("3D Preview", 14, 18);

        g2.dispose();
    }

    /* ===================== Floor (L-shape support) ===================== */

    private Shape buildFloorShape(RoomSpec spec, double roomW, double roomD,
                                  double cos, double sin, double scale, double cx, double cy) {

        // Default: simple rectangle floor
        if (!isLShape(spec)) {
            Vec2 p0 = projectIso(-roomW / 2, 0, -roomD / 2, cos, sin, scale, cx, cy);
            Vec2 p1 = projectIso( roomW / 2, 0, -roomD / 2, cos, sin, scale, cx, cy);
            Vec2 p2 = projectIso( roomW / 2, 0,  roomD / 2, cos, sin, scale, cx, cy);
            Vec2 p3 = projectIso(-roomW / 2, 0,  roomD / 2, cos, sin, scale, cx, cy);

            Path2D floor = new Path2D.Double();
            floor.moveTo(p0.x, p0.y);
            floor.lineTo(p1.x, p1.y);
            floor.lineTo(p2.x, p2.y);
            floor.lineTo(p3.x, p3.y);
            floor.closePath();
            return floor;
        }

        // L-shape: cut a rectangle from the outer floor (default TOP-RIGHT cut)
        double cutW = safeLCutW(spec);
        double cutD = safeLCutD(spec);

        // Clamp cut so it doesn't exceed room size
        cutW = Math.max(1, Math.min(roomW - 1, cutW));
        cutD = Math.max(1, Math.min(roomD - 1, cutD));

        double xL = -roomW / 2.0;
        double xR =  roomW / 2.0;
        double zT = -roomD / 2.0;
        double zB =  roomD / 2.0;

        double xCut = xR - cutW;
        double zCut = zT + cutD;

        Vec2 a = projectIso(xL, 0, zT, cos, sin, scale, cx, cy);
        Vec2 b = projectIso(xCut, 0, zT, cos, sin, scale, cx, cy);
        Vec2 c = projectIso(xCut, 0, zCut, cos, sin, scale, cx, cy);
        Vec2 d = projectIso(xR, 0, zCut, cos, sin, scale, cx, cy);
        Vec2 e = projectIso(xR, 0, zB, cos, sin, scale, cx, cy);
        Vec2 f = projectIso(xL, 0, zB, cos, sin, scale, cx, cy);

        Path2D floor = new Path2D.Double();
        floor.moveTo(a.x, a.y);
        floor.lineTo(b.x, b.y);
        floor.lineTo(c.x, c.y);
        floor.lineTo(d.x, d.y);
        floor.lineTo(e.x, e.y);
        floor.lineTo(f.x, f.y);
        floor.closePath();
        return floor;
    }

    private boolean isLShape(RoomSpec spec) {
        if (spec == null) return false;
        try {
            String s = spec.getShape();
            if (s == null) return false;
            return "L-Shape".equalsIgnoreCase(s.trim())
                    && safeLCutW(spec) > 0
                    && safeLCutD(spec) > 0;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private double safeLCutW(RoomSpec spec) {
        try { return Math.max(0, spec.getLCutWidth()); } catch (Throwable ignored) { return 0; }
    }

    private double safeLCutD(RoomSpec spec) {
        try { return Math.max(0, spec.getLCutLength()); } catch (Throwable ignored) { return 0; }
    }

    private double safeRoomW(RoomSpec spec, double fallback) {
        try {
            if (spec != null && spec.getWidth() > 0) return spec.getWidth();
        } catch (Throwable ignored) {}
        return fallback;
    }

    private double safeRoomD(RoomSpec spec, double fallback) {
        try {
            if (spec != null && spec.getLength() > 0) return spec.getLength();
        } catch (Throwable ignored) {}
        return fallback;
    }

    private RoomSpec safeRoomSpec(Design d) {
        try { return d.getRoomSpec(); } catch (Throwable ignored) { return null; }
    }

    /* ===================== Rendering helpers ===================== */

    private void paintBackground(Graphics2D g2, int w, int h) {
        Color top = new Color(0x0B1220);
        Color bot = new Color(0x05070D);
        GradientPaint gp = new GradientPaint(0, 0, top, 0, h, bot);
        g2.setPaint(gp);
        g2.fillRect(0, 0, w, h);

        // subtle stars
        g2.setColor(new Color(255, 255, 255, 28));
        for (int i = 0; i < 70; i++) {
            int x = (i * 73) % Math.max(1, w);
            int y = (i * 91) % Math.max(1, Math.max(1, h / 2));
            g2.fillRect(x, y, 1, 1);
        }
    }

    private void drawFurniture(Graphics2D g2, RoomItem r, FurnitureItem it,
                               double cos, double sin, double scale, double cx, double cy) {

        String kind = safeKind(it);

        Color base = parseHex(safe(it == null ? null : it.getColorHex()), new Color(0x3B82F6));
        int shade = clamp(it == null ? 50 : it.getShadingPercent(), 0, 100);
        double mul = 0.65 + (shade / 100.0) * 0.65;
        base = applyBrightness(base, mul);

        if (kind.contains("TABLE_ROUND")) {
            drawRoundTable(g2, r, it, base, cos, sin, scale, cx, cy);
            return;
        }

        if (kind.contains("TABLE_RECT")) {
            drawRectTable(g2, r, it, base, cos, sin, scale, cx, cy);
            return;
        }

        if (kind.contains("CHAIR")) {
            drawChairDetailed(g2, r, it, base, cos, sin, scale, cx, cy);
            return;
        }

        // default: block
        drawPrism(g2, r.cx, r.cz, r.w, r.d, r.rotDeg, 0, guessHeight(it, r.w, r.d),
                base, cos, sin, scale, cx, cy);

        drawIconOnTop(g2, it, r.cx, guessHeight(it, r.w, r.d), r.cz, cos, sin, scale, cx, cy, r.w, r.d);
    }

    /* ===================== Furniture models (better shapes) ===================== */

    // Rectangular table: 4 legs + tabletop slab
    private void drawRectTable(Graphics2D g2, RoomItem r, FurnitureItem it, Color base,
                               double cos, double sin, double scale, double cx, double cy) {

        double fullH = guessHeight(it, r.w, r.d);

        double topT = clampD(Math.min(r.w, r.d) * 0.14, 4, fullH * 0.35);
        double legH = Math.max(2, fullH - topT);

        double legW = clampD(Math.min(r.w, r.d) * 0.14, 3, Math.min(r.w, r.d) * 0.22);
        double legD = legW;

        // legs positions (local, then rotate into world)
        double offX = (r.w / 2.0) - (legW / 2.0);
        double offZ = (r.d / 2.0) - (legD / 2.0);

        Vec2[] legsLocal = new Vec2[]{
                new Vec2(-offX, -offZ),
                new Vec2( offX, -offZ),
                new Vec2( offX,  offZ),
                new Vec2(-offX,  offZ),
        };

        Color legCol = applyBrightness(base, 0.78);
        for (Vec2 lp : legsLocal) {
            Vec2 wp = rotate2D(lp.x, lp.y, r.rotDeg);
            drawPrism(g2,
                    r.cx + wp.x, r.cz + wp.y,
                    legW, legD, r.rotDeg,
                    0, legH,
                    legCol, cos, sin, scale, cx, cy);
        }

        // tabletop slab
        Color topCol = applyBrightness(base, 1.05);
        drawPrism(g2,
                r.cx, r.cz,
                r.w, r.d, r.rotDeg,
                legH, topT,
                topCol, cos, sin, scale, cx, cy);

        drawIconOnTop(g2, it, r.cx, fullH, r.cz, cos, sin, scale, cx, cy, r.w, r.d);
    }

    // Round table: pedestal + round top (still “round” but not just a cylinder block)
    private void drawRoundTable(Graphics2D g2, RoomItem r, FurnitureItem it, Color base,
                                double cos, double sin, double scale, double cx, double cy) {

        double fullH = guessHeight(it, r.w, r.d);

        double topT = clampD(Math.min(r.w, r.d) * 0.14, 4, fullH * 0.35);
        double legH = Math.max(2, fullH - topT);

        // pedestal radius
        double pedW = Math.min(r.w, r.d) * 0.24;
        double pedD = pedW;

        // pedestal (a prism is fine for preview; looks like a column)
        Color pedCol = applyBrightness(base, 0.78);
        drawPrism(g2,
                r.cx, r.cz,
                pedW, pedD, r.rotDeg,
                0, legH,
                pedCol, cos, sin, scale, cx, cy);

        // round top (use cylinder slab)
        Color topCol = applyBrightness(base, 1.05);
        drawCylinderAtY(g2, r, it, topCol, legH, topT, cos, sin, scale, cx, cy);

        drawIconOnTop(g2, it, r.cx, fullH, r.cz, cos, sin, scale, cx, cy, r.w, r.d);
    }

    // Chair: 4 legs + seat slab + backrest slab (looks like a chair, not a cube)
    private void drawChairDetailed(Graphics2D g2, RoomItem r, FurnitureItem it, Color base,
                                   double cos, double sin, double scale, double cx, double cy) {

        double fullH = guessHeight(it, r.w, r.d);

        double legH = fullH * 0.55;
        double seatT = clampD(fullH * 0.18, 3, fullH * 0.35);

        double legW = clampD(Math.min(r.w, r.d) * 0.14, 2.5, Math.min(r.w, r.d) * 0.22);
        double legD = legW;

        double offX = (r.w / 2.0) - (legW / 2.0);
        double offZ = (r.d / 2.0) - (legD / 2.0);

        Vec2[] legsLocal = new Vec2[]{
                new Vec2(-offX, -offZ),
                new Vec2( offX, -offZ),
                new Vec2( offX,  offZ),
                new Vec2(-offX,  offZ),
        };

        Color legCol = applyBrightness(base, 0.75);
        for (Vec2 lp : legsLocal) {
            Vec2 wp = rotate2D(lp.x, lp.y, r.rotDeg);
            drawPrism(g2,
                    r.cx + wp.x, r.cz + wp.y,
                    legW, legD, r.rotDeg,
                    0, legH,
                    legCol, cos, sin, scale, cx, cy);
        }

        // seat slab
        Color seatCol = applyBrightness(base, 0.95);
        drawPrism(g2,
                r.cx, r.cz,
                r.w, r.d, r.rotDeg,
                legH, seatT,
                seatCol, cos, sin, scale, cx, cy);

        // backrest slab: thin depth along “back” edge (negative local Z)
        double backDepth = clampD(r.d * 0.22, 3, r.d * 0.35);
        double backH = Math.max(2, fullH - (legH + seatT));

        // offset to back edge in local space, then rotate
        Vec2 localOffset = rotate2D(0, -(r.d / 2.0 - backDepth / 2.0), r.rotDeg);

        Color backCol = applyBrightness(base, 0.88);
        drawPrism(g2,
                r.cx + localOffset.x, r.cz + localOffset.y,
                r.w, backDepth, r.rotDeg,
                legH + seatT, backH,
                backCol, cos, sin, scale, cx, cy);

        drawIconOnTop(g2, it, r.cx, fullH, r.cz, cos, sin, scale, cx, cy, r.w, r.d);
    }

    /* ===================== Core 3D primitives ===================== */

    // Generic prism at yBase..yBase+height
    private void drawPrism(Graphics2D g2,
                           double cx3, double cz3,
                           double w3, double d3,
                           int rotDeg,
                           double yBase, double height,
                           Color base,
                           double cos, double sin, double scale, double cx, double cy) {

        height = Math.max(0.1, height);

        Vec3[] basePts = rotatedRectPts(cx3, cz3, w3, d3, rotDeg, yBase);
        Vec3[] topPts  = rotatedRectPts(cx3, cz3, w3, d3, rotDeg, yBase + height);

        Vec2[] b = projectAll(basePts, cos, sin, scale, cx, cy);
        Vec2[] t = projectAll(topPts,  cos, sin, scale, cx, cy);

        Color side1 = applyBrightness(base, 0.78);
        Color side2 = applyBrightness(base, 0.62);
        Color top   = applyBrightness(base, 1.05);

        // sides
        Path2D s1 = quad(b[1], b[2], t[2], t[1]);
        Path2D s2 = quad(b[2], b[3], t[3], t[2]);

        g2.setColor(side2);
        g2.fill(s1);
        g2.setColor(side1);
        g2.fill(s2);

        // top
        Path2D topFace = quad(t[0], t[1], t[2], t[3]);
        g2.setColor(top);
        g2.fill(topFace);

        // outline
        g2.setColor(new Color(0, 0, 0, 50));
        g2.setStroke(new BasicStroke(1.0f));
        g2.draw(topFace);
    }

    // Cylinder slab at yBase..yBase+height (for round table tops)
    private void drawCylinderAtY(Graphics2D g2, RoomItem r, FurnitureItem it, Color base,
                                 double yBase, double height,
                                 double cos, double sin, double scale, double cx, double cy) {

        height = Math.max(0.1, height);

        int seg = 14;
        double radX = r.w / 2.0;
        double radZ = r.d / 2.0;

        Vec3[] baseRing = new Vec3[seg];
        Vec3[] topRing  = new Vec3[seg];

        for (int i = 0; i < seg; i++) {
            double a = (Math.PI * 2.0 * i) / seg;
            double lx = Math.cos(a) * radX;
            double lz = Math.sin(a) * radZ;

            Vec2 rr = rotate2D(lx, lz, r.rotDeg);

            baseRing[i] = new Vec3(r.cx + rr.x, yBase,          r.cz + rr.y);
            topRing[i]  = new Vec3(r.cx + rr.x, yBase + height, r.cz + rr.y);
        }

        Color side = applyBrightness(base, 0.72);
        Color top  = applyBrightness(base, 1.05);

        // side band (half)
        Path2D sideA = new Path2D.Double();
        Vec2 s0 = projectIso(baseRing[0].x, baseRing[0].y, baseRing[0].z, cos, sin, scale, cx, cy);
        sideA.moveTo(s0.x, s0.y);
        for (int i = 1; i <= seg / 2; i++) {
            Vec2 p = projectIso(baseRing[i].x, baseRing[i].y, baseRing[i].z, cos, sin, scale, cx, cy);
            sideA.lineTo(p.x, p.y);
        }
        for (int i = seg / 2; i >= 0; i--) {
            Vec2 p = projectIso(topRing[i].x, topRing[i].y, topRing[i].z, cos, sin, scale, cx, cy);
            sideA.lineTo(p.x, p.y);
        }
        sideA.closePath();

        g2.setColor(side);
        g2.fill(sideA);

        // top
        Path2D topPoly = new Path2D.Double();
        Vec2 p0 = projectIso(topRing[0].x, topRing[0].y, topRing[0].z, cos, sin, scale, cx, cy);
        topPoly.moveTo(p0.x, p0.y);
        for (int i = 1; i < seg; i++) {
            Vec2 p = projectIso(topRing[i].x, topRing[i].y, topRing[i].z, cos, sin, scale, cx, cy);
            topPoly.lineTo(p.x, p.y);
        }
        topPoly.closePath();

        g2.setColor(top);
        g2.fill(topPoly);

        g2.setColor(new Color(0, 0, 0, 55));
        g2.setStroke(new BasicStroke(1.0f));
        g2.draw(topPoly);
    }

    private void drawIconOnTop(Graphics2D g2, FurnitureItem it,
                               double x, double y, double z,
                               double cos, double sin, double scale, double cx, double cy,
                               double w, double d) {

        String icon = pickIcon(it);

        Vec2 p = projectIso(x, y + 2, z, cos, sin, scale, cx, cy);

        float fontSize = (float) clamp((int) Math.round(Math.min(w, d) * 0.30), 10, 18);
        Font f = g2.getFont().deriveFont(Font.BOLD, fontSize);
        g2.setFont(f);

        FontMetrics fm = g2.getFontMetrics();
        int tx = (int) Math.round(p.x - fm.stringWidth(icon) / 2.0);
        int ty = (int) Math.round(p.y + fm.getAscent() / 2.0);

        g2.setColor(new Color(0, 0, 0, 120));
        g2.drawString(icon, tx + 1, ty + 1);

        g2.setColor(new Color(255, 255, 255, 235));
        g2.drawString(icon, tx, ty);
    }

    private String pickIcon(FurnitureItem it) {
        String kind = safeKind(it);
        try {
            FurnitureKind k = FurnitureKind.valueOf(kind);
            if (k != null && k.iconText != null && !k.iconText.isBlank()) return k.iconText;
        } catch (Throwable ignored) {}

        if (kind.contains("CHAIR")) return "C";
        if (kind.contains("TABLE_ROUND")) return "●";
        if (kind.contains("TABLE")) return "T";
        if (kind.contains("SOFA")) return "S";
        return "□";
    }

    private String safeKind(FurnitureItem it) {
        try {
            Object k = (it == null) ? null : it.getKind();
            if (k == null) return "";
            String s = String.valueOf(k).trim();
            if (s.isBlank()) return "";
            return s.toUpperCase(Locale.ENGLISH);
        } catch (Throwable ignored) {
            return "";
        }
    }

    private Vec2[] projectAll(Vec3[] pts, double cos, double sin, double scale, double cx, double cy) {
        Vec2[] out = new Vec2[pts.length];
        for (int i = 0; i < pts.length; i++) {
            out[i] = projectIso(pts[i].x, pts[i].y, pts[i].z, cos, sin, scale, cx, cy);
        }
        return out;
    }

    private Path2D quad(Vec2 a, Vec2 b, Vec2 c, Vec2 d) {
        Path2D p = new Path2D.Double();
        p.moveTo(a.x, a.y);
        p.lineTo(b.x, b.y);
        p.lineTo(c.x, c.y);
        p.lineTo(d.x, d.y);
        p.closePath();
        return p;
    }

    private Vec2 projectIso(double x, double y, double z,
                            double cos, double sin, double scale,
                            double cx, double cy) {
        double ix = (x - z) * cos;
        double iy = (x + z) * sin - y;
        return new Vec2(cx + ix * scale, cy + iy * scale);
    }

    /* ===================== Mapping ===================== */

    private RoomItem mapItemToRoom(FurnitureItem it, Design d, double roomW, double roomD) {
        double lx = safeLayoutX(d, 0);
        double ly = safeLayoutY(d, 0);
        double lw = safeLayoutW(d, 520);
        double lh = safeLayoutH(d, 520);

        double px = (it == null) ? 0 : it.getX();
        double py = (it == null) ? 0 : it.getY();
        double pw = (it == null) ? 40 : Math.max(10, it.getW());
        double ph = (it == null) ? 40 : Math.max(10, it.getH());

        double nx = ((px + pw / 2.0) - lx) / Math.max(1, lw);
        double nz = ((py + ph / 2.0) - ly) / Math.max(1, lh);
        double nw = pw / Math.max(1, lw);
        double nd = ph / Math.max(1, lh);

        double cx = (nx * roomW) - roomW / 2.0;
        double cz = (nz * roomD) - roomD / 2.0;
        double w = nw * roomW;
        double dd = nd * roomD;

        int rot = 0;
        try { rot = (it == null) ? 0 : it.getRotation(); } catch (Throwable ignored) {}
        rot = ((rot % 360) + 360) % 360;

        return new RoomItem(cx, cz, w, dd, rot);
    }

    private Vec3[] rotatedRectPts(double cx, double cz, double w, double d, double deg, double y) {
        double hw = w / 2.0;
        double hd = d / 2.0;

        Vec2 p0 = rotate2D(-hw, -hd, deg);
        Vec2 p1 = rotate2D( hw, -hd, deg);
        Vec2 p2 = rotate2D( hw,  hd, deg);
        Vec2 p3 = rotate2D(-hw,  hd, deg);

        return new Vec3[]{
                new Vec3(cx + p0.x, y, cz + p0.y),
                new Vec3(cx + p1.x, y, cz + p1.y),
                new Vec3(cx + p2.x, y, cz + p2.y),
                new Vec3(cx + p3.x, y, cz + p3.y),
        };
    }

    private Vec2 rotate2D(double x, double z, double deg) {
        double rad = Math.toRadians(deg);
        double c = Math.cos(rad), s = Math.sin(rad);
        double rx = x * c - z * s;
        double rz = x * s + z * c;
        return new Vec2(rx, rz);
    }

    private double guessHeight(FurnitureItem it, double w, double d) {
        String kind = (it == null || it.getKind() == null) ? "" : it.getKind().toUpperCase();

        // height proportional to footprint for "room units"
        double base = Math.max(w, d) * 0.45;

        if (kind.contains("CHAIR")) return base * 0.95;
        if (kind.contains("TABLE")) return base * 0.70;
        if (kind.contains("SOFA"))  return base * 0.85;

        return base;
    }

    /* ===================== Safe helpers ===================== */

    private String safe(String s) { return (s == null) ? "" : s; }

    private int clamp(int v, int a, int b) { return Math.max(a, Math.min(b, v)); }

    private double clampD(double v, double a, double b) { return Math.max(a, Math.min(b, v)); }

    private Color parseHex(String hex, Color fallback) {
        try {
            String h = hex == null ? "" : hex.trim();
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

    // ✅ FIX: only use getters that exist in your Design class + handle null Integer
    private double safeLayoutX(Design d, double fallback) {
        try {
            Integer v = d.getLayoutX();
            return (v == null) ? fallback : v.doubleValue();
        } catch (Throwable ignored) { }
        return fallback;
    }

    private double safeLayoutY(Design d, double fallback) {
        try {
            Integer v = d.getLayoutY();
            return (v == null) ? fallback : v.doubleValue();
        } catch (Throwable ignored) { }
        return fallback;
    }

    private double safeLayoutW(Design d, double fallback) {
        try {
            Integer v = d.getLayoutWidth();
            return (v == null || v <= 0) ? fallback : v.doubleValue();
        } catch (Throwable ignored) { }
        return fallback;
    }

    private double safeLayoutH(Design d, double fallback) {
        try {
            Integer v = d.getLayoutHeight();
            return (v == null || v <= 0) ? fallback : v.doubleValue();
        } catch (Throwable ignored) { }
        return fallback;
    }

    /* ===================== Small structs ===================== */

    private static class Vec2 {
        final double x, y;
        Vec2(double x, double y) { this.x = x; this.y = y; }
    }

    private static class Vec3 {
        final double x, y, z;
        Vec3(double x, double y, double z) { this.x = x; this.y = y; this.z = z; }
    }

    private static class RoomItem {
        final double cx, cz;
        final double w, d;
        final int rotDeg;
        RoomItem(double cx, double cz, double w, double d, int rotDeg) {
            this.cx = cx;
            this.cz = cz;
            this.w = w;
            this.d = d;
            this.rotDeg = rotDeg;
        }
    }
}
