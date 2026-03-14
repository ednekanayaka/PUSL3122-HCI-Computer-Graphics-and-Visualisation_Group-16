package com.roomviz.screens;

import com.roomviz.data.AppState;
import com.roomviz.model.Design;
import com.roomviz.model.RoomSpec;
import com.roomviz.ui.UiKit;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class EditDesignDialog extends JDialog {

    private final Design design;
    private final AppState appState;
    private final Runnable onSuccess;

    private JTextField nameField;
    private JTextField customerField;
    private JTextField widthField;
    private JTextField lengthField;
    
    // New fields
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
        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBorder(new EmptyBorder(20, 20, 20, 20));
        content.setBackground(UiKit.WHITE);

        // Fields
        nameField = UiKit.inputField("Design Name", design.getDesignName());
        customerField = UiKit.inputField("Customer Name", design.getCustomerName());

        RoomSpec spec = design.getRoomSpec();
        double w = spec != null ? spec.getWidth() : 0;
        double l = spec != null ? spec.getLength() : 0;
        
        widthField = UiKit.inputField("Width", String.valueOf(w));
        lengthField = UiKit.inputField("Length", String.valueOf(l));
        
        // New fields initialization
        String[] types = {"Living Room", "Bedroom", "Kitchen", "Office", "Bathroom", "Dining Room", "Other"};
        typeBox = new JComboBox<>(types);
        UiKit.styleDropdown(typeBox);
        typeBox.setSelectedItem(spec != null && spec.getRoomType() != null ? spec.getRoomType() : "Living Room");
        
        String[] shapes = {"Rectangular", "Square", "L-Shape", "Custom"};
        shapeBox = new JComboBox<>(shapes);
        UiKit.styleDropdown(shapeBox);
        shapeBox.setSelectedItem(spec != null && spec.getShape() != null ? spec.getShape() : "Rectangular");
        
        notesArea = new JTextArea(design.getNotes() != null ? design.getNotes() : "");
        notesArea.setRows(3);
        notesArea.setLineWrap(true);
        notesArea.setWrapStyleWord(true);
        JScrollPane notesScroll = new JScrollPane(notesArea);
        notesScroll.setBorder(BorderFactory.createLineBorder(UiKit.BORDER));

        content.add(label("Design Name"));
        content.add(nameField);
        content.add(Box.createVerticalStrut(12));
        content.add(label("Customer Name"));
        content.add(customerField);
        content.add(Box.createVerticalStrut(12));
        
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
        content.add(Box.createVerticalStrut(12));

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
        content.add(Box.createVerticalStrut(12));
        
        content.add(label("Designer Notes"));
        content.add(notesScroll);
        content.add(Box.createVerticalStrut(20));

        errorLabel = new JLabel(" ");
        errorLabel.setForeground(UiKit.DANGER);
        errorLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(errorLabel);
        content.add(Box.createVerticalStrut(10));

        // Buttons
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
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

    private JLabel label(String text) {
        JLabel l = new JLabel(text);
        l.setFont(UiKit.semibold(l.getFont(), 12f));
        l.setForeground(UiKit.TEXT);
        l.setBorder(new EmptyBorder(0, 0, 4, 0));
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        return l;
    }

    private void save() {
        String name = nameField.getText().trim();
        String customer = customerField.getText().trim();
        String wStr = widthField.getText().trim();
        String lStr = lengthField.getText().trim();

        if (name.isEmpty()) {
            errorLabel.setText("Design name is required.");
            return;
        }

        double w, l;
        try {
            w = Double.parseDouble(wStr);
            l = Double.parseDouble(lStr);
            if (w <= 0 || l <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            errorLabel.setText("Dimensions must be valid positive numbers.");
            return;
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
        // Preserve other spec fields (unit, shape, colorScheme)
        spec.setRoomType((String) typeBox.getSelectedItem());
        spec.setShape((String) shapeBox.getSelectedItem());
        design.setNotes(notesArea.getText().trim());

        design.touchUpdatedAtNow();
        
        // Save to repo
        appState.getRepo().upsert(design);

        if (onSuccess != null) onSuccess.run();
        dispose();
    }
}
