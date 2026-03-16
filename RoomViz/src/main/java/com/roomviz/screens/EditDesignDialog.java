package com.roomviz.screens;

import com.roomviz.data.AppState;
import com.roomviz.model.Design;
import com.roomviz.model.RoomSpec;
import com.roomviz.ui.FontAwesome;
import com.roomviz.ui.UiKit;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;

public class EditDesignDialog extends JDialog {

    private static final int HEADER_PAD_X = 24;
    private static final int HEADER_PAD_Y = 16;
    private static final int BODY_PAD_X = 24;
    private static final int SECTION_GAP = 12;
    private static final int CARD_PAD_X = 18;
    private static final int CARD_PAD_Y = 16;
    private static final int FIELD_HEIGHT = 40;
    private static final int ROW_GAP_X = 12;
    private static final int NOTES_HEIGHT = 110;
    private static final int MAX_FORM_WIDTH = 620;
    private static final int CARD_RADIUS = 14;
    private static final int CONTROL_MIN_WIDTH = 120;
    private static final int BUTTON_MIN_WIDTH = 110;
    private static final int BUTTON_PRIMARY_MIN_WIDTH = 148;

    private final Design design;
    private final AppState appState;
    private final Runnable onSuccess;

    // Palette (dark-mode aware)
    private final Color C_BG;
    private final Color C_CARD;
    private final Color C_BORDER;
    private final Color C_TEXT;
    private final Color C_MUTED;
    private final Color C_PRIMARY;
    private final Color C_HEADER_END;
    private final Color C_DANGER;
    private final Color C_INPUT_BG;
    private final Color C_INPUT_BORDER;
    private final Color C_ACCENT_BG;

    private JTextField nameField;
    private JTextField customerField;
    private JTextField widthField;
    private JTextField lengthField;
    private JTextField lCutWidthField;
    private JTextField lCutLengthField;
    private JPanel lShapeWrap;
    private JComboBox<String> typeBox;
    private JComboBox<String> shapeBox;
    private JTextArea notesArea;
    private JLabel errorLabel;

    public EditDesignDialog(Window owner, Design design, AppState appState, Runnable onSuccess) {
        super(owner, "Edit Design", ModalityType.APPLICATION_MODAL);
        this.design = design;
        this.appState = appState;
        this.onSuccess = onSuccess;

        // Pick palette
        boolean dark = UiKit.isDarkBlueMode();
        C_BG           = dark ? new Color(0x0D1526) : new Color(0xF3F4F6);
        C_CARD         = dark ? new Color(0x111D33) : Color.WHITE;
        C_BORDER       = dark ? new Color(0x1E3050) : new Color(0xE5E7EB);
        C_TEXT         = dark ? new Color(0xE8EEFF) : new Color(0x111827);
        C_MUTED        = dark ? new Color(0x8899BB) : new Color(0x6B7280);
        C_PRIMARY      = dark ? new Color(0x3B82F6) : new Color(0x5B2BFF);
        C_HEADER_END   = dark ? new Color(0x5E70D3) : new Color(0x6F87EE);
        C_DANGER       = dark ? new Color(0xF87171) : new Color(0xDC2626);
        C_INPUT_BG     = dark ? new Color(0x0B1220) : Color.WHITE;
        C_INPUT_BORDER = dark ? new Color(0x253552) : new Color(0xD1D5DB);
        C_ACCENT_BG    = dark ? new Color(0x172444) : new Color(0xEEF2FF);

        setLayout(new BorderLayout());
        setUndecorated(false);
        getContentPane().setBackground(C_BG);

        // ===== existing values =====
        RoomSpec spec = design.getRoomSpec();
        double w = spec != null ? spec.getWidth() : 0;
        double l = spec != null ? spec.getLength() : 0;
        double cutW = spec != null ? spec.getLCutWidth() : 0;
        double cutL = spec != null ? spec.getLCutLength() : 0;

        // ===== fields =====
        nameField = styledInput(safe(design.getDesignName(), ""));
        customerField = styledInput(safe(design.getCustomerName(), ""));
        widthField = styledInput(w > 0 ? trimZeros(w) : "");
        lengthField = styledInput(l > 0 ? trimZeros(l) : "");
        lCutWidthField = styledInput(cutW > 0 ? trimZeros(cutW) : "");
        lCutLengthField = styledInput(cutL > 0 ? trimZeros(cutL) : "");

        String[] types = {"Living Room", "Bedroom", "Kitchen", "Office", "Bathroom", "Dining Room", "Other"};
        typeBox = new JComboBox<>(types);
        styleCombo(typeBox);
        typeBox.setSelectedItem(spec != null && spec.getRoomType() != null ? spec.getRoomType() : "Living Room");

        String[] shapes = {"Rectangular", "Square", "L-Shape", "Custom"};
        shapeBox = new JComboBox<>(shapes);
        styleCombo(shapeBox);
        shapeBox.setSelectedItem(spec != null && spec.getShape() != null ? spec.getShape() : "Rectangular");

        notesArea = new JTextArea(safe(design.getNotes(), ""));
        notesArea.setRows(4);
        notesArea.setLineWrap(true);
        notesArea.setWrapStyleWord(true);
        notesArea.setFont(new Font("Inter", Font.PLAIN, 13));
        notesArea.setForeground(C_TEXT);
        notesArea.setBackground(C_INPUT_BG);
        notesArea.setCaretColor(C_TEXT);
        notesArea.setBorder(new EmptyBorder(10, 12, 10, 12));

        JScrollPane notesScroll = new JScrollPane(notesArea);
        notesScroll.setBorder(new LineBorder(C_INPUT_BORDER, 1, true));
        notesScroll.getViewport().setBackground(C_INPUT_BG);
        notesScroll.setPreferredSize(new Dimension(240, NOTES_HEIGHT));
        notesScroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, NOTES_HEIGHT));
        notesScroll.setMinimumSize(new Dimension(CONTROL_MIN_WIDTH, NOTES_HEIGHT));

        // ===== Main content =====
        JPanel content = new JPanel();
        content.setOpaque(true);
        content.setBackground(C_BG);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBorder(new EmptyBorder(0, 0, 18, 0));

        // --- Compact gradient header ---
        JPanel headerBar = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                GradientPaint gp = new GradientPaint(0, 0, C_PRIMARY, getWidth(), 0, C_HEADER_END);
                g2.setPaint(gp);
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.dispose();
                super.paintComponent(g);
            }
        };
        headerBar.setOpaque(false);
        headerBar.setBorder(new EmptyBorder(HEADER_PAD_Y, HEADER_PAD_X, HEADER_PAD_Y, HEADER_PAD_X));
        headerBar.setAlignmentX(Component.CENTER_ALIGNMENT);
        headerBar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 86));

        JPanel heroRow = new JPanel(new BorderLayout(10, 0));
        heroRow.setOpaque(false);

        JLabel heroIcon = new JLabel(FontAwesome.PENCIL);
        heroIcon.setFont(FontAwesome.solid(13f));
        heroIcon.setForeground(new Color(255, 255, 255, 230));
        heroIcon.setBorder(new EmptyBorder(2, 0, 0, 0));

        JPanel heroText = new JPanel();
        heroText.setOpaque(false);
        heroText.setLayout(new BoxLayout(heroText, BoxLayout.Y_AXIS));

        JLabel headerTitle = new JLabel("Edit Design Details");
        headerTitle.setFont(new Font("Inter", Font.BOLD, 16));
        headerTitle.setForeground(Color.WHITE);
        headerTitle.setAlignmentX(0.0f);

        JLabel headerSub = new JLabel("Modify room configuration, dimensions and notes");
        headerSub.setFont(new Font("Inter", Font.PLAIN, 12));
        headerSub.setForeground(new Color(255, 255, 255, 190));
        headerSub.setBorder(new EmptyBorder(3, 0, 0, 0));
        headerSub.setAlignmentX(0.0f);

        heroText.add(headerTitle);
        heroText.add(headerSub);

        heroRow.add(heroIcon, BorderLayout.WEST);
        heroRow.add(heroText, BorderLayout.CENTER);
        headerBar.add(heroRow, BorderLayout.CENTER);

        content.add(headerBar);
        content.add(Box.createVerticalStrut(SECTION_GAP));

        // Centered form body (max-width constrained)
        JPanel body = new JPanel();
        body.setOpaque(false);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBorder(new EmptyBorder(0, BODY_PAD_X, 0, BODY_PAD_X));
        body.setAlignmentX(Component.CENTER_ALIGNMENT);
        body.setMaximumSize(new Dimension(MAX_FORM_WIDTH + (BODY_PAD_X * 2), Integer.MAX_VALUE));

        // --- Section 1: Basic Details ---
        body.add(sectionCard(FontAwesome.PENCIL, "Basic Details", new JComponent[]{
                labeledField("Design Name", nameField),
                labeledField("Customer", customerField)
        }));
        body.add(Box.createVerticalStrut(SECTION_GAP));

        // --- Section 2: Room Dimensions ---
        body.add(sectionCard(FontAwesome.VECTOR_SQUARE, "Room Dimensions", new JComponent[]{
                twoColFields("Width", widthField, "Length", lengthField)
        }));
        body.add(Box.createVerticalStrut(SECTION_GAP));

        // --- Section 3: Room Details ---
        JPanel sec3Extra = new JPanel();
        sec3Extra.setOpaque(false);
        sec3Extra.setLayout(new BoxLayout(sec3Extra, BoxLayout.Y_AXIS));
        sec3Extra.add(twoColFields("Room Type", typeBox, "Shape", shapeBox));

        lShapeWrap = new JPanel();
        lShapeWrap.setOpaque(false);
        lShapeWrap.setLayout(new BoxLayout(lShapeWrap, BoxLayout.Y_AXIS));
        lShapeWrap.setBorder(new EmptyBorder(SECTION_GAP, 0, 0, 0));

        JLabel lHint = new JLabel("Cut-out is measured from the top-right and must be smaller than the outer room.");
        lHint.setFont(new Font("Inter", Font.PLAIN, 11));
        lHint.setForeground(C_MUTED);
        lHint.setAlignmentX(0.0f);

        lShapeWrap.add(lHint);
        lShapeWrap.add(Box.createVerticalStrut(8));
        lShapeWrap.add(twoColFields("Cut Width", lCutWidthField, "Cut Length", lCutLengthField));
        sec3Extra.add(lShapeWrap);

        body.add(sectionCardRaw(FontAwesome.CUBE, "Room Details", sec3Extra));
        body.add(Box.createVerticalStrut(SECTION_GAP));

        // --- Section 4: Notes ---
        JPanel notesInner = new JPanel();
        notesInner.setOpaque(false);
        notesInner.setLayout(new BoxLayout(notesInner, BoxLayout.Y_AXIS));

        JLabel notesLabel = fieldLabel("Designer Notes");
        notesLabel.setAlignmentX(0.0f);
        notesInner.add(notesLabel);
        notesInner.add(Box.createVerticalStrut(6));
        notesScroll.setAlignmentX(0.0f);
        notesInner.add(notesScroll);

        body.add(sectionCardRaw(FontAwesome.CIRCLE_INFO, "Notes", notesInner));
        body.add(Box.createVerticalStrut(SECTION_GAP));

        // Footer: errors + actions
        JPanel footer = new JPanel();
        footer.setOpaque(false);
        footer.setLayout(new BoxLayout(footer, BoxLayout.Y_AXIS));
        footer.setAlignmentX(0.0f);

        errorLabel = new JLabel(" ");
        errorLabel.setForeground(C_DANGER);
        errorLabel.setFont(new Font("Inter", Font.PLAIN, 12));
        errorLabel.setAlignmentX(0.0f);
        footer.add(errorLabel);
        footer.add(Box.createVerticalStrut(8));

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttons.setOpaque(false);
        buttons.setAlignmentX(0.0f);

        JButton cancel = ghostBtn("Cancel");
        normalizeButtonSize(cancel, BUTTON_MIN_WIDTH);
        cancel.addActionListener(e -> dispose());

        JButton save = accentBtn(FontAwesome.CHECK + "  Save Changes");
        normalizeButtonSize(save, BUTTON_PRIMARY_MIN_WIDTH);
        save.addActionListener(e -> save());

        buttons.add(cancel);
        buttons.add(save);
        footer.add(buttons);
        body.add(footer);

        content.add(body);

        // Scroll wrapper
        JScrollPane scroller = new JScrollPane(content);
        scroller.setBorder(null);
        scroller.getVerticalScrollBar().setUnitIncrement(16);
        scroller.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroller.getViewport().setBackground(C_BG);

        add(scroller, BorderLayout.CENTER);

        // L-Shape toggle
        updateLShapeVisibility();
        shapeBox.addActionListener(e -> updateLShapeVisibility());

        setMinimumSize(new Dimension(560, 500));
        setPreferredSize(new Dimension(700, 740));
        pack();
        setLocationRelativeTo(owner);
        setResizable(true);
    }

    /* ========================= Section Card Builder ========================= */

    private JPanel sectionCard(String icon, String title, JComponent[] fields) {
        JPanel inner = new JPanel();
        inner.setOpaque(false);
        inner.setLayout(new BoxLayout(inner, BoxLayout.Y_AXIS));
        for (int i = 0; i < fields.length; i++) {
            fields[i].setAlignmentX(0.0f);
            inner.add(fields[i]);
            if (i < fields.length - 1) inner.add(Box.createVerticalStrut(SECTION_GAP));
        }
        return sectionCardRaw(icon, title, inner);
    }

    private JPanel sectionCardRaw(String icon, String title, JComponent body) {
        JPanel card = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(C_CARD);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), CARD_RADIUS, CARD_RADIUS);
                g2.setColor(C_BORDER);
                g2.setStroke(new BasicStroke(1f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, CARD_RADIUS, CARD_RADIUS);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        card.setOpaque(false);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(new EmptyBorder(CARD_PAD_Y, CARD_PAD_X, CARD_PAD_Y, CARD_PAD_X));
        card.setAlignmentX(0.0f);
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));

        JPanel headerRow = new JPanel();
        headerRow.setOpaque(false);
        headerRow.setLayout(new BoxLayout(headerRow, BoxLayout.X_AXIS));
        headerRow.setAlignmentX(0.0f);
        headerRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));

        JLabel badge = new JLabel(icon) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(C_ACCENT_BG);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 7, 7);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        badge.setFont(FontAwesome.solid(12f));
        badge.setForeground(C_PRIMARY);
        badge.setHorizontalAlignment(SwingConstants.CENTER);
        badge.setPreferredSize(new Dimension(24, 24));
        badge.setMinimumSize(new Dimension(24, 24));
        badge.setMaximumSize(new Dimension(24, 24));
        badge.setOpaque(false);

        JLabel titleLabel = new JLabel(title);
        titleLabel.setForeground(C_TEXT);
        titleLabel.setFont(new Font("Inter", Font.BOLD, 14));

        headerRow.add(badge);
        headerRow.add(Box.createHorizontalStrut(10));
        headerRow.add(titleLabel);
        headerRow.add(Box.createHorizontalGlue());

        card.add(headerRow);
        card.add(Box.createVerticalStrut(12));

        body.setAlignmentX(0.0f);
        card.add(body);

        return card;
    }

    /* ========================= Field Helpers ========================= */

    private JPanel labeledField(String labelText, JComponent field) {
        JPanel wrap = new JPanel();
        wrap.setOpaque(false);
        wrap.setLayout(new BoxLayout(wrap, BoxLayout.Y_AXIS));
        wrap.setAlignmentX(0.0f);

        JLabel l = fieldLabel(labelText);
        l.setAlignmentX(0.0f);
        wrap.add(l);
        wrap.add(Box.createVerticalStrut(6));

        normalizeControlHeight(field);
        field.setAlignmentX(0.0f);
        wrap.add(field);
        return wrap;
    }

    private JPanel twoColFields(String label1, JComponent field1, String label2, JComponent field2) {
        JPanel row = new JPanel(new GridBagLayout());
        row.setOpaque(false);
        row.setAlignmentX(0.0f);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weighty = 1.0;
        gbc.gridy = 0;

        JPanel col1 = new JPanel();
        col1.setOpaque(false);
        col1.setLayout(new BoxLayout(col1, BoxLayout.Y_AXIS));
        JLabel l1 = fieldLabel(label1);
        l1.setAlignmentX(0.0f);
        col1.add(l1);
        col1.add(Box.createVerticalStrut(6));
        normalizeControlHeight(field1);
        field1.setAlignmentX(0.0f);
        col1.add(field1);

        JPanel col2 = new JPanel();
        col2.setOpaque(false);
        col2.setLayout(new BoxLayout(col2, BoxLayout.Y_AXIS));
        JLabel l2 = fieldLabel(label2);
        l2.setAlignmentX(0.0f);
        col2.add(l2);
        col2.add(Box.createVerticalStrut(6));
        normalizeControlHeight(field2);
        field2.setAlignmentX(0.0f);
        col2.add(field2);

        int halfGap = ROW_GAP_X / 2;

        gbc.gridx = 0;
        gbc.weightx = 0.5;
        gbc.insets = new Insets(0, 0, 0, halfGap);
        row.add(col1, gbc);

        gbc.gridx = 1;
        gbc.weightx = 0.5;
        gbc.insets = new Insets(0, halfGap, 0, 0);
        row.add(col2, gbc);

        return row;
    }

    private void normalizeControlHeight(JComponent c) {
        if (c instanceof JTextField || c instanceof JComboBox) {
            c.setPreferredSize(new Dimension(240, FIELD_HEIGHT));
            c.setMinimumSize(new Dimension(CONTROL_MIN_WIDTH, FIELD_HEIGHT));
            c.setMaximumSize(new Dimension(Integer.MAX_VALUE, FIELD_HEIGHT));
        }
    }

    private JTextField styledInput(String text) {
        JTextField tf = new JTextField(text);
        tf.setFont(new Font("Inter", Font.PLAIN, 13));
        tf.setForeground(C_TEXT);
        tf.setBackground(C_INPUT_BG);
        tf.setCaretColor(C_TEXT);
        tf.setBorder(new CompoundBorder(
                new LineBorder(C_INPUT_BORDER, 1, true),
                new EmptyBorder(9, 12, 9, 12)
        ));
        normalizeControlHeight(tf);
        return tf;
    }

    private void styleCombo(JComboBox<String> cb) {
        UiKit.styleDropdown(cb);
        cb.setFont(new Font("Inter", Font.PLAIN, 13));
        cb.setForeground(C_TEXT);
        cb.setBackground(C_INPUT_BG);
        cb.setBorder(new CompoundBorder(
                new LineBorder(C_INPUT_BORDER, 1, true),
                new EmptyBorder(8, 10, 8, 10)
        ));
        normalizeControlHeight(cb);
    }

    private JLabel fieldLabel(String text) {
        JLabel l = new JLabel(text);
        l.setForeground(C_MUTED);
        l.setFont(new Font("Inter", Font.PLAIN, 12));
        return l;
    }

    /* ========================= Buttons ========================= */

    private JButton accentBtn(String text) {
        JButton b = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                GradientPaint gp = new GradientPaint(0, 0, C_PRIMARY, getWidth(), 0, C_HEADER_END);
                g2.setPaint(gp);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        b.setFont(FontAwesome.solid(13f));
        b.setForeground(Color.WHITE);
        b.setContentAreaFilled(false);
        b.setOpaque(false);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setBorder(new EmptyBorder(0, 18, 0, 18));
        return b;
    }

    private JButton ghostBtn(String text) {
        JButton b = new JButton(text);
        b.setFont(new Font("Inter", Font.PLAIN, 13));
        b.setForeground(C_MUTED);
        b.setContentAreaFilled(false);
        b.setOpaque(false);
        b.setBorderPainted(true);
        b.setFocusPainted(false);
        b.setBorder(new CompoundBorder(
                new LineBorder(C_BORDER, 1, true),
                new EmptyBorder(0, 18, 0, 18)
        ));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    private void normalizeButtonSize(JButton b, int minWidth) {
        Dimension pref = b.getPreferredSize();
        int width = Math.max(minWidth, pref.width);
        b.setPreferredSize(new Dimension(width, FIELD_HEIGHT));
        b.setMinimumSize(new Dimension(minWidth, FIELD_HEIGHT));
    }

    /* ========================= Logic ========================= */

    private void updateLShapeVisibility() {
        boolean isL = "L-Shape".equalsIgnoreCase(String.valueOf(shapeBox.getSelectedItem()));
        if (lShapeWrap != null) lShapeWrap.setVisible(isL);

        if (!isL) {
            if (lCutWidthField != null) lCutWidthField.setText("");
            if (lCutLengthField != null) lCutLengthField.setText("");
        }

        revalidate();
        repaint();
    }

    private void save() {
        errorLabel.setText(" ");

        String name = safe(nameField.getText(), "").trim();
        String customer = safe(customerField.getText(), "").trim();
        String wStr = safe(widthField.getText(), "").trim();
        String lStr = safe(lengthField.getText(), "").trim();

        if (name.isEmpty()) {
            errorLabel.setText("Design name is required.");
            return;
        }

        double w, l;
        try {
            w = Double.parseDouble(wStr);
            l = Double.parseDouble(lStr);
            if (w <= 0 || l <= 0) throw new NumberFormatException();
        } catch (Exception e) {
            errorLabel.setText("Width and Length must be valid positive numbers.");
            return;
        }

        String chosenShape = String.valueOf(shapeBox.getSelectedItem());

        if ("Square".equalsIgnoreCase(chosenShape)) {
            if (Math.abs(w - l) > 1e-6) {
                errorLabel.setText("For Square rooms, Width and Length must be equal.");
                return;
            }
        }

        double cutW = 0;
        double cutL = 0;

        if ("L-Shape".equalsIgnoreCase(chosenShape)) {
            String cwStr = safe(lCutWidthField.getText(), "").trim();
            String clStr = safe(lCutLengthField.getText(), "").trim();

            if (cwStr.isEmpty() || clStr.isEmpty()) {
                errorLabel.setText("L-Shape rooms require Cut-out Width and Length.");
                return;
            }

            try {
                cutW = Double.parseDouble(cwStr);
                cutL = Double.parseDouble(clStr);
                if (cutW <= 0 || cutL <= 0) throw new NumberFormatException();
            } catch (Exception ex) {
                errorLabel.setText("Cut-out values must be valid positive numbers.");
                return;
            }

            if (cutW >= w || cutL >= l) {
                errorLabel.setText("Cut-out must be smaller than the outer room.");
                return;
            }
        }

        // Update design
        design.setDesignName(name);
        design.setCustomerName(customer);

        RoomSpec spec = design.getRoomSpec();
        if (spec == null) {
            spec = new RoomSpec();
            design.setRoomSpec(spec);
        }

        spec.setWidth(w);
        spec.setLength(l);
        spec.setRoomType((String) typeBox.getSelectedItem());
        spec.setShape(chosenShape);

        if ("L-Shape".equalsIgnoreCase(chosenShape)) {
            spec.setLCutWidth(cutW);
            spec.setLCutLength(cutL);
            spec.setLCorner("TOP_RIGHT");
        } else {
            spec.setLCutWidth(0);
            spec.setLCutLength(0);
            spec.setLCorner("TOP_RIGHT");
        }

        design.setNotes(safe(notesArea.getText(), "").trim());
        design.touchUpdatedAtNow();

        if (appState != null && appState.getRepo() != null) {
            appState.getRepo().upsert(design);
        }

        if (onSuccess != null) onSuccess.run();
        dispose();
    }

    private static String safe(String s, String fallback) {
        return (s == null) ? fallback : s;
    }

    private static String trimZeros(double v) {
        String s = String.valueOf(v);
        if (s.endsWith(".0")) return s.substring(0, s.length() - 2);
        if (s.contains(".")) {
            while (s.endsWith("0")) s = s.substring(0, s.length() - 1);
            if (s.endsWith(".")) s = s.substring(0, s.length() - 1);
        }
        return s;
    }
}
