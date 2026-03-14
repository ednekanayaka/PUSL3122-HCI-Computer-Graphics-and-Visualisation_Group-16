package com.roomviz.screens;

import com.roomviz.data.AppState;
import com.roomviz.model.Design;
import com.roomviz.model.RoomSpec;
import com.roomviz.ui.UiKit;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;

public class EditDesignDialog extends JDialog {

    private final Design design;
    private final AppState appState;
    private final Runnable onSuccess;

    private JTextField nameField;
    private JTextField customerField;
    private JTextField widthField;
    private JTextField lengthField;

    private JComboBox<String> typeBox;
    private JComboBox<String> shapeBox;
    private JTextArea notesArea;

    private JLabel errorLabel;

    public EditDesignDialog(Window owner, Design design, AppState appState, Runnable onSuccess) {
        super(owner, "Edit Design Details", ModalityType.APPLICATION_MODAL);
        this.design = design;
        this.appState = appState;
        this.onSuccess = onSuccess;

        setLayout(new BorderLayout());
        getContentPane().setBackground(UiKit.WHITE);

        JPanel content = new JPanel();
        content.setOpaque(true);
        content.setBackground(UiKit.WHITE);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBorder(new EmptyBorder(18, 18, 18, 18));

        // ===== existing values =====
        RoomSpec spec = design.getRoomSpec();
        double w = spec != null ? spec.getWidth() : 0;
        double l = spec != null ? spec.getLength() : 0;

        // ===== fields =====
        nameField = UiKit.inputField("Design Name", safe(design.getDesignName(), ""));
        customerField = UiKit.inputField("Customer Name", safe(design.getCustomerName(), ""));

        widthField = UiKit.inputField("Width", (w > 0 ? trimZeros(w) : ""));
        lengthField = UiKit.inputField("Length", (l > 0 ? trimZeros(l) : ""));

        // Dropdowns
        String[] types = {"Living Room", "Bedroom", "Kitchen", "Office", "Bathroom", "Dining Room", "Other"};
        typeBox = new JComboBox<>(types);
        UiKit.styleDropdown(typeBox);
        typeBox.setSelectedItem(spec != null && spec.getRoomType() != null ? spec.getRoomType() : "Living Room");

        String[] shapes = {"Rectangular", "Square", "L-Shape", "Custom"};
        shapeBox = new JComboBox<>(shapes);
        UiKit.styleDropdown(shapeBox);
        shapeBox.setSelectedItem(spec != null && spec.getShape() != null ? spec.getShape() : "Rectangular");

        notesArea = new JTextArea(safe(design.getNotes(), ""));
        notesArea.setRows(4);
        notesArea.setLineWrap(true);
        notesArea.setWrapStyleWord(true);
        notesArea.setFont(UiKit.scaled(notesArea, Font.PLAIN, 0.95f));
        notesArea.setForeground(UiKit.TEXT);

        JScrollPane notesScroll = new JScrollPane(notesArea);
        notesScroll.setBorder(new CompoundBorder(
                new LineBorder(UiKit.BORDER, 1, true),
                new EmptyBorder(6, 8, 6, 8)
        ));
        notesScroll.getViewport().setBackground(Color.WHITE);

        // ===== layout =====
        content.add(sectionTitle("Basic Details"));
        content.add(Box.createVerticalStrut(10));

        content.add(label("Design Name"));
        content.add(nameField);
        content.add(Box.createVerticalStrut(12));

        content.add(label("Customer Name"));
        content.add(customerField);
        content.add(Box.createVerticalStrut(14));

        content.add(sectionTitle("Room Dimensions"));
        content.add(Box.createVerticalStrut(10));

        JPanel dimPanel = new JPanel(new GridLayout(1, 2, 12, 0));
        dimPanel.setOpaque(false);

        JPanel wPanel = new JPanel(new BorderLayout());
        wPanel.setOpaque(false);
        wPanel.add(label("Width"), BorderLayout.NORTH);
        wPanel.add(widthField, BorderLayout.CENTER);

        JPanel lPanel = new JPanel(new BorderLayout());
        lPanel.setOpaque(false);
        lPanel.add(label("Length"), BorderLayout.NORTH);
        lPanel.add(lengthField, BorderLayout.CENTER);

        dimPanel.add(wPanel);
        dimPanel.add(lPanel);

        content.add(dimPanel);
        content.add(Box.createVerticalStrut(14));

        content.add(sectionTitle("Room Details"));
        content.add(Box.createVerticalStrut(10));

        JPanel extraPanel = new JPanel(new GridLayout(1, 2, 12, 0));
        extraPanel.setOpaque(false);

        JPanel tPanel = new JPanel(new BorderLayout());
        tPanel.setOpaque(false);
        tPanel.add(label("Room Type"), BorderLayout.NORTH);
        tPanel.add(typeBox, BorderLayout.CENTER);

        JPanel sPanel = new JPanel(new BorderLayout());
        sPanel.setOpaque(false);
        sPanel.add(label("Shape"), BorderLayout.NORTH);
        sPanel.add(shapeBox, BorderLayout.CENTER);

        extraPanel.add(tPanel);
        extraPanel.add(sPanel);

        content.add(extraPanel);
        content.add(Box.createVerticalStrut(14));

        content.add(sectionTitle("Notes"));
        content.add(Box.createVerticalStrut(10));

        content.add(label("Designer Notes"));
        content.add(notesScroll);
        content.add(Box.createVerticalStrut(16));

        errorLabel = new JLabel(" ");
        errorLabel.setForeground(UiKit.DANGER);
        errorLabel.setFont(UiKit.scaled(errorLabel, Font.PLAIN, 0.90f));
        errorLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(errorLabel);
        content.add(Box.createVerticalStrut(10));

        // ===== buttons =====
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttons.setOpaque(false);

        JButton cancel = UiKit.ghostButton("Cancel");
        cancel.addActionListener(e -> dispose());

        JButton save = UiKit.primaryButton("Save Changes");
        save.addActionListener(e -> save());

        buttons.add(cancel);
        buttons.add(save);

        content.add(buttons);

        add(content, BorderLayout.CENTER);

        pack();
        setLocationRelativeTo(owner);
        setResizable(false);
    }

    private JLabel sectionTitle(String text) {
        JLabel l = new JLabel(text);
        l.setForeground(UiKit.TEXT);
        l.setFont(UiKit.scaled(l, Font.BOLD, 1.02f));
        l.setBorder(new EmptyBorder(0, 0, 2, 0));
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        return l;
    }

    private JLabel label(String text) {
        JLabel l = new JLabel(text);
        l.setForeground(UiKit.MUTED);
        l.setFont(UiKit.scaled(l, Font.PLAIN, 0.90f));
        l.setBorder(new EmptyBorder(0, 0, 4, 0));
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        return l;
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

        // Update design
        design.setDesignName(name);
        design.setCustomerName(customer);

        // Room spec (preserve existing fields)
        RoomSpec spec = design.getRoomSpec();
        if (spec == null) {
            spec = new RoomSpec();
            design.setRoomSpec(spec);
        }

        spec.setWidth(w);
        spec.setLength(l);

        // Keep unit / colorScheme as-is (if already set), update these:
        spec.setRoomType((String) typeBox.getSelectedItem());
        spec.setShape((String) shapeBox.getSelectedItem());

        design.setNotes(safe(notesArea.getText(), "").trim());

        design.touchUpdatedAtNow();

        // Save to repo
        if (appState != null && appState.getRepo() != null) {
            appState.getRepo().upsert(design);
        }

        if (onSuccess != null) onSuccess.run();
        dispose();
    }

    private static String safe(String s, String fallback) {
        if (s == null) return fallback;
        return s;
    }

    private static String trimZeros(double v) {
        // e.g., 12.0 -> "12", 12.50 -> "12.5"
        String s = String.valueOf(v);
        if (s.endsWith(".0")) return s.substring(0, s.length() - 2);
        // remove trailing zeros for decimals
        if (s.contains(".")) {
            while (s.endsWith("0")) s = s.substring(0, s.length() - 1);
            if (s.endsWith(".")) s = s.substring(0, s.length() - 1);
        }
        return s;
    }
}
