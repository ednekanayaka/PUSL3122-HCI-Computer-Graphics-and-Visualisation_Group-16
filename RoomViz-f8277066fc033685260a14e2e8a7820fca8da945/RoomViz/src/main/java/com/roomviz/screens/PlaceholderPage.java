package com.roomviz.screens;

import com.roomviz.app.Router;
import com.roomviz.ui.UiKit;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * Placeholder screen used for routes that aren't implemented yet.
 * - Figma-ish centered card
 * - Optional Back button (either via Runnable OR Router+screenKey)
 */
public class PlaceholderPage extends JPanel {

    private static final Color BG = new Color(0xF6F7FB);
    private static final Color TEXT = new Color(0x111827);
    private static final Color MUTED = new Color(0x6B7280);
    private static final Color BORDER = new Color(0xE5E7EB);
    private static final Color WHITE = Color.WHITE;

    // --- simplest: no back ---
    public PlaceholderPage(String title) {
        this(title, (Runnable) null);
    }

    // --- best option: pass what should happen when Back is clicked ---
    public PlaceholderPage(String title, Runnable onBack) {
        buildUI(title, onBack);
    }

    // --- convenience: Router + screen key (uses router.show, not router.back) ---
    public PlaceholderPage(String title, Router router, String backScreenKey) {
        buildUI(title, (router == null || backScreenKey == null) ? null : () -> router.show(backScreenKey));
    }

    private void buildUI(String title, Runnable onBack) {
        setLayout(new BorderLayout());
        setOpaque(true);
        setBackground(BG);

        // Top bar (optional back)
        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.setBorder(new EmptyBorder(18, 20, 10, 20));

        if (onBack != null) {
            JButton back = UiKit.ghostButton("← Back");
            back.addActionListener(e -> onBack.run());
            top.add(back, BorderLayout.WEST);
        }

        add(top, BorderLayout.NORTH);

        // Center card
        JPanel centerWrap = new JPanel(new GridBagLayout());
        centerWrap.setOpaque(false);
        centerWrap.setBorder(new EmptyBorder(10, 20, 30, 20));

        UiKit.RoundedPanel card = new UiKit.RoundedPanel(16, WHITE);
        card.setBorderPaint(BORDER);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(new EmptyBorder(22, 22, 22, 22));
        card.setPreferredSize(new Dimension(560, 220));

        JLabel h1 = new JLabel(title);
        h1.setAlignmentX(Component.CENTER_ALIGNMENT);
        h1.setForeground(TEXT);
        h1.setFont(h1.getFont().deriveFont(Font.BOLD, 22f));

        JLabel sub = new JLabel("This page is under construction. We'll wire it up next.");
        sub.setAlignmentX(Component.CENTER_ALIGNMENT);
        sub.setForeground(MUTED);
        sub.setFont(sub.getFont().deriveFont(Font.PLAIN, 12.8f));
        sub.setBorder(new EmptyBorder(8, 0, 0, 0));

        JLabel hint = new JLabel("Tip: You can keep building from the 2D Planner and Design Library.");
        hint.setAlignmentX(Component.CENTER_ALIGNMENT);
        hint.setForeground(new Color(0x9CA3AF));
        hint.setFont(hint.getFont().deriveFont(Font.PLAIN, 12.2f));
        hint.setBorder(new EmptyBorder(10, 0, 0, 0));

        card.add(h1);
        card.add(sub);
        card.add(hint);

        centerWrap.add(card);
        add(centerWrap, BorderLayout.CENTER);
    }
}
