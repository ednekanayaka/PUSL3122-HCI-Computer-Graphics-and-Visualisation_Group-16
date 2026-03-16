package com.roomviz.screens;

import com.roomviz.app.AppFrame;
import com.roomviz.app.Router;
import com.roomviz.ui.UiKit;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;

/**
 * New Design Wizard page (Figma-like)
 */
public class NewDesignWizardPage extends JPanel {

    private static final int STEPS = 4;
    private int currentStep = 1;

    private final JLabel stepTitle = new JLabel("Design Information");
    private final JLabel stepSubtitle = new JLabel("Let's start with the basic details of your design project");

    private final JPanel formHost = new JPanel(new CardLayout());
    private final Stepper stepper = new Stepper();

    private final JButton backBtn = UiKit.ghostButton("←  Back");
    private final JButton nextBtn = UiKit.primaryButton("Next  →");

    // Step 1 fields
    private final JTextField designName = new JTextField();
    private final JTextField customerName = new JTextField();
    private final JTextArea notes = new JTextArea(5, 40);

    // Step 2 fields
    private final JTextField roomWidth = new JTextField();
    private final JTextField roomLength = new JTextField();
    private final JComboBox<String> unit = new JComboBox<>(new String[]{"ft", "cm", "m"});

    // Step 3 fields
    private final JComboBox<String> roomShape = new JComboBox<>(new String[]{"Rectangular", "Square", "L-Shape", "Custom"});

    // Step 4 fields
    private final JComboBox<String> colorScheme = new JComboBox<>(new String[]{"Neutral Tones", "Warm Tones", "Cool Tones", "Monochrome", "Pastels"});

    public NewDesignWizardPage(AppFrame frame, Router router) {
        setLayout(new BorderLayout());
        setOpaque(false);

        JPanel page = new JPanel();
        page.setOpaque(false);
        page.setLayout(new BoxLayout(page, BoxLayout.Y_AXIS));
        page.setBorder(new EmptyBorder(18, 24, 18, 24));

        page.add(topHeader(frame));
        page.add(Box.createVerticalStrut(14));

        page.add(stepperWrap());
        page.add(Box.createVerticalStrut(18));

        page.add(centerCard());
        page.add(Box.createVerticalStrut(18));

        page.add(bottomNav());

        JScrollPane scroller = new JScrollPane(page);
        scroller.setBorder(BorderFactory.createEmptyBorder());
        scroller.getViewport().setOpaque(true);
        scroller.getViewport().setBackground(UiKit.BG);
        scroller.setOpaque(false);
        scroller.getVerticalScrollBar().setUnitIncrement(18);

        add(scroller, BorderLayout.CENTER);

        setStep(1);
    }

    /* ========================= Header ========================= */

    private JComponent topHeader(AppFrame frame) {
        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(false);

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        left.setOpaque(false);

        UiKit.RoundedPanel icon = new UiKit.RoundedPanel(10, new Color(0xEEF2FF));
        icon.setBorderPaint(new Color(0xC7D2FE));
        icon.setPreferredSize(new Dimension(32, 32));
        icon.setLayout(new GridBagLayout());

        JLabel i = new JLabel("✦");
        i.setForeground(UiKit.PRIMARY_DARK);
        i.setFont(i.getFont().deriveFont(Font.BOLD, 14f));
        icon.add(i);

        JPanel text = new JPanel();
        text.setOpaque(false);
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("New Design Wizard");
        title.setForeground(UiKit.TEXT);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 14.5f));

        JLabel sub = new JLabel("Create a custom room design");
        sub.setForeground(UiKit.MUTED);
        sub.setFont(sub.getFont().deriveFont(Font.PLAIN, 12f));
        sub.setBorder(new EmptyBorder(2, 0, 0, 0));

        text.add(title);
        text.add(sub);

        left.add(icon);
        left.add(text);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        right.setOpaque(false);

        JButton saveDraft = UiKit.ghostButton("💾  Save as Draft");
        saveDraft.addActionListener(e -> JOptionPane.showMessageDialog(this, "Draft saved (hook up later)."));

        JButton close = UiKit.iconButton("✕");
        close.setToolTipText("Close wizard");
        close.addActionListener(e -> JOptionPane.showMessageDialog(this, "Close wizard (hook up later)."));

        right.add(saveDraft);
        right.add(close);

        row.add(left, BorderLayout.WEST);
        row.add(right, BorderLayout.EAST);
        return row;
    }

    /* ========================= Stepper wrapper ========================= */

    private JComponent stepperWrap() {
        UiKit.RoundedPanel wrap = new UiKit.RoundedPanel(14, UiKit.WHITE);
        wrap.setBorderPaint(UiKit.BORDER);
        wrap.setLayout(new BorderLayout());
        wrap.setBorder(new EmptyBorder(14, 14, 14, 14));
        wrap.add(stepper, BorderLayout.CENTER);
        return wrap;
    }

    /* ========================= Center Card ========================= */

    private JComponent centerCard() {
        UiKit.RoundedPanel card = new UiKit.RoundedPanel(14, UiKit.WHITE);
        card.setBorderPaint(UiKit.BORDER);
        card.setLayout(new BorderLayout());
        card.setBorder(new EmptyBorder(18, 18, 18, 18));

        JPanel header = new JPanel();
        header.setOpaque(false);
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));

        stepTitle.setForeground(UiKit.TEXT);
        stepTitle.setFont(stepTitle.getFont().deriveFont(Font.BOLD, 16f));

        stepSubtitle.setForeground(UiKit.MUTED);
        stepSubtitle.setFont(stepSubtitle.getFont().deriveFont(Font.PLAIN, 12.2f));
        stepSubtitle.setBorder(new EmptyBorder(6, 0, 0, 0));

        header.add(stepTitle);
        header.add(stepSubtitle);

        formHost.setOpaque(false);
        formHost.add(step1Panel(), "1");
        formHost.add(step2Panel(), "2");
        formHost.add(step3Panel(), "3");
        formHost.add(step4Panel(), "4");

        JPanel body = new JPanel(new BorderLayout());
        body.setOpaque(false);
        body.setBorder(new EmptyBorder(14, 0, 0, 0));
        body.add(formHost, BorderLayout.CENTER);

        card.add(header, BorderLayout.NORTH);
        card.add(body, BorderLayout.CENTER);
        return card;
    }

    /* ========================= Bottom Nav ========================= */

    private JComponent bottomNav() {
        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(false);

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        left.setOpaque(false);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        right.setOpaque(false);

        styleNavButton(backBtn);
        styleNavButton(nextBtn);

        backBtn.addActionListener(e -> setStep(currentStep - 1));
        nextBtn.addActionListener(e -> {
            if (currentStep < STEPS) setStep(currentStep + 1);
            else JOptionPane.showMessageDialog(this, "Wizard complete ✅ (hook up create flow later)");
        });

        left.add(backBtn);
        right.add(nextBtn);

        row.add(left, BorderLayout.WEST);
        row.add(right, BorderLayout.EAST);
        return row;
    }

    /* ========================= Steps ========================= */

    private JComponent step1Panel() {
        JPanel p = formStack();

        // ✅ FIXED: hint(placeholder, field) and fieldBlock(label, component)
        p.add(fieldBlock("Design Name *", hint("e.g., Modern Living Room Redesign", designName)));
        p.add(Box.createVerticalStrut(12));

        p.add(fieldBlock("Customer Name *", hint("e.g., John Smith", customerName)));
        p.add(Box.createVerticalStrut(12));

        p.add(textAreaBlock("Project Notes (Optional)",
                "Add any additional notes or requirements for this design...",
                notes));

        return p;
    }

    private JComponent step2Panel() {
        JPanel p = formStack();

        JPanel row = new JPanel(new GridLayout(1, 3, 12, 0));
        row.setOpaque(false);

        // ✅ FIXED: pass both args to hint
        row.add(fieldBlock("Room Width *", hint("e.g., 24", roomWidth)));
        row.add(fieldBlock("Room Length *", hint("e.g., 18", roomLength)));

        JPanel unitWrap = new JPanel();
        unitWrap.setOpaque(false);
        unitWrap.setLayout(new BoxLayout(unitWrap, BoxLayout.Y_AXIS));
        unitWrap.add(label("Units *"));
        unitWrap.add(Box.createVerticalStrut(6));
        UiKit.styleDropdown(unit);
        unitWrap.add(unit);

        row.add(unitWrap);

        p.add(row);
        p.add(Box.createVerticalStrut(10));

        JLabel info = new JLabel("Tip: Use the same units you will use in the 2D Planner.");
        info.setForeground(UiKit.MUTED);
        info.setFont(info.getFont().deriveFont(11.5f));
        p.add(info);

        return p;
    }

    private JComponent step3Panel() {
        JPanel p = formStack();

        UiKit.styleDropdown(roomShape);
        p.add(dropdownBlock("Room Shape *", roomShape));
        p.add(Box.createVerticalStrut(10));

        JLabel note = new JLabel("You can refine the room shape later inside the 2D Planner.");
        note.setForeground(UiKit.MUTED);
        note.setFont(note.getFont().deriveFont(11.5f));
        p.add(note);

        return p;
    }

    private JComponent step4Panel() {
        JPanel p = formStack();

        UiKit.styleDropdown(colorScheme);
        p.add(dropdownBlock("Color Scheme *", colorScheme));
        p.add(Box.createVerticalStrut(12));

        JPanel palettes = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        palettes.setOpaque(false);
        palettes.add(colorDot(new Color(0x111827)));
        palettes.add(colorDot(new Color(0xF5F3FF)));
        palettes.add(colorDot(new Color(0xEDE9FE)));
        palettes.add(colorDot(new Color(0xF3F4F6)));
        palettes.add(colorDot(new Color(0xD1D5DB)));

        JLabel prev = new JLabel("Preview");
        prev.setForeground(UiKit.TEXT);

        p.add(prev);
        p.add(Box.createVerticalStrut(8));
        p.add(palettes);

        return p;
    }

    /* ========================= Step handling ========================= */

    private void setStep(int step) {
        if (step < 1) step = 1;
        if (step > STEPS) step = STEPS;

        currentStep = step;
        stepper.setActive(step);

        switch (step) {
            case 1 -> {
                stepTitle.setText("Design Information");
                stepSubtitle.setText("Let's start with the basic details of your design project");
                nextBtn.setText("Next  →");
            }
            case 2 -> {
                stepTitle.setText("Dimensions");
                stepSubtitle.setText("Enter the room measurements to set up your design");
                nextBtn.setText("Next  →");
            }
            case 3 -> {
                stepTitle.setText("Room Shape");
                stepSubtitle.setText("Choose a room shape that matches the customer space");
                nextBtn.setText("Next  →");
            }
            case 4 -> {
                stepTitle.setText("Color Scheme");
                stepSubtitle.setText("Pick a starting color theme for your design");
                nextBtn.setText("Finish  ✓");
            }
        }

        CardLayout cl = (CardLayout) formHost.getLayout();
        cl.show(formHost, String.valueOf(step));

        backBtn.setEnabled(step != 1);

        revalidate();
        repaint();
    }

    /* ========================= Small UI helpers ========================= */

    private JPanel formStack() {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        return p;
    }

    private JLabel label(String t) {
        JLabel l = new JLabel(t);
        l.setForeground(UiKit.TEXT);
        l.setFont(l.getFont().deriveFont(Font.PLAIN, 12f));
        return l;
    }

    private JComponent hint(String placeholder, JTextField field) {
        styleTextField(field);
        setPlaceholder(field, placeholder);
        return field;
    }

    private JComponent fieldBlock(String label, JComponent field) {
        JPanel b = new JPanel();
        b.setOpaque(false);
        b.setLayout(new BoxLayout(b, BoxLayout.Y_AXIS));
        b.add(label(label));
        b.add(Box.createVerticalStrut(6));
        b.add(field);
        return b;
    }

    private JComponent dropdownBlock(String label, JComponent dropdown) {
        JPanel b = new JPanel();
        b.setOpaque(false);
        b.setLayout(new BoxLayout(b, BoxLayout.Y_AXIS));
        b.add(label(label));
        b.add(Box.createVerticalStrut(6));
        b.add(dropdown);
        return b;
    }

    private JComponent textAreaBlock(String label, String placeholder, JTextArea area) {
        JPanel b = new JPanel();
        b.setOpaque(false);
        b.setLayout(new BoxLayout(b, BoxLayout.Y_AXIS));
        b.add(label(label));
        b.add(Box.createVerticalStrut(6));

        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setFont(area.getFont().deriveFont(13f));
        area.setBorder(new EmptyBorder(10, 12, 10, 12));
        area.setForeground(new Color(0x111827));

        JScrollPane sp = new JScrollPane(area);
        sp.setBorder(new LineBorder(UiKit.BORDER, 1, true));
        sp.setPreferredSize(new Dimension(0, 120));

        area.setText(placeholder);
        area.setForeground(new Color(0x9CA3AF));
        area.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override public void focusGained(java.awt.event.FocusEvent e) {
                if (placeholder.equals(area.getText())) {
                    area.setText("");
                    area.setForeground(new Color(0x111827));
                }
            }
            @Override public void focusLost(java.awt.event.FocusEvent e) {
                if (area.getText().trim().isEmpty()) {
                    area.setText(placeholder);
                    area.setForeground(new Color(0x9CA3AF));
                }
            }
        });

        b.add(sp);

        JLabel helper = new JLabel("Optional: Add special requirements, preferences, or important details");
        helper.setForeground(UiKit.MUTED);
        helper.setFont(helper.getFont().deriveFont(11.2f));
        helper.setBorder(new EmptyBorder(6, 0, 0, 0));
        b.add(helper);

        return b;
    }

    private void styleTextField(JTextField tf) {
        tf.setFont(tf.getFont().deriveFont(13f));
        tf.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(UiKit.BORDER, 1, true),
                new EmptyBorder(10, 12, 10, 12)
        ));
        tf.setBackground(Color.WHITE);
        tf.setForeground(new Color(0x111827));
        tf.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
    }

    private void setPlaceholder(JTextField tf, String placeholder) {
        tf.setText(placeholder);
        tf.setForeground(new Color(0x9CA3AF));

        tf.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override public void focusGained(java.awt.event.FocusEvent e) {
                if (placeholder.equals(tf.getText())) {
                    tf.setText("");
                    tf.setForeground(new Color(0x111827));
                }
            }
            @Override public void focusLost(java.awt.event.FocusEvent e) {
                if (tf.getText().trim().isEmpty()) {
                    tf.setText(placeholder);
                    tf.setForeground(new Color(0x9CA3AF));
                }
            }
        });
    }

    private void styleNavButton(JButton b) {
        b.setFont(b.getFont().deriveFont(Font.BOLD, 12.2f));
        b.setBorder(new EmptyBorder(10, 14, 10, 14));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    private JComponent colorDot(Color c) {
        UiKit.RoundedPanel dot = new UiKit.RoundedPanel(999, c);
        dot.setPreferredSize(new Dimension(18, 18));
        dot.setBorderPaint(new Color(0xE5E7EB));
        return dot;
    }

    /* ========================= Stepper ========================= */

    private class Stepper extends JPanel {
        private int active = 1;

        Stepper() {
            setOpaque(false);
            setLayout(new GridLayout(1, STEPS, 12, 0));
            rebuild();
        }

        void setActive(int active) {
            this.active = active;
            rebuild();
        }

        private void rebuild() {
            removeAll();
            add(stepItem(1, "Design Info"));
            add(stepItem(2, "Dimensions"));
            add(stepItem(3, "Room Shape"));
            add(stepItem(4, "Color Scheme"));
            revalidate();
            repaint();
        }

        private JComponent stepItem(int number, String label) {
            JPanel p = new JPanel(new BorderLayout(10, 0));
            p.setOpaque(false);

            boolean done = number < active;
            boolean isActive = number == active;

            UiKit.RoundedPanel circle = new UiKit.RoundedPanel(
                    999,
                    isActive ? UiKit.PRIMARY : new Color(0xE5E7EB)
            );
            circle.setPreferredSize(new Dimension(28, 28));
            circle.setLayout(new GridBagLayout());

            JLabel n = new JLabel(done ? "✓" : String.valueOf(number));
            n.setForeground(isActive ? Color.WHITE : new Color(0x6B7280));
            n.setFont(n.getFont().deriveFont(Font.BOLD, 12f));
            circle.add(n);

            JLabel t = new JLabel(label);
            t.setForeground(isActive ? UiKit.TEXT : UiKit.MUTED);
            t.setFont(t.getFont().deriveFont(isActive ? Font.BOLD : Font.PLAIN, 12f));

            JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
            left.setOpaque(false);
            left.add(circle);

            p.add(left, BorderLayout.WEST);
            p.add(t, BorderLayout.CENTER);

            return p;
        }
    }
}
