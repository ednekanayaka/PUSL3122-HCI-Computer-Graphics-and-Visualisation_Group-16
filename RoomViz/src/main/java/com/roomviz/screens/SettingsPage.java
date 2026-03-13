package com.roomviz.screens;

import com.roomviz.app.AppFrame;
import com.roomviz.ui.UiKit;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;

/**
 * Settings page UI – matches provided design screenshot:
 * Left column:
 *   - Profile Information
 *   - Change Password
 *   - Application Preferences
 * Right column:
 *   - Accessibility
 *   - Help & Support
 *   - Account
 * Bottom:
 *   - Cancel + Save Changes
 *
 * NOTE: This is UI-first (coursework) with safe placeholder actions.
 */
public class SettingsPage extends JPanel {

    // Profile fields
    private final JTextField fullName = new JTextField("Sarah Johnson");
    private final JTextField email = new JTextField("sarah.johnson@designstudio.com");
    private final JTextField jobTitle = new JTextField("Senior UI/UX Designer");
    private final JComboBox<String> department = new JComboBox<>(new String[] {
            "Design Team", "Engineering", "Product", "Operations"
    });

    // Password fields
    private final JPasswordField currentPw = new JPasswordField();
    private final JPasswordField newPw = new JPasswordField();
    private final JPasswordField confirmPw = new JPasswordField();
    private final JLabel pwError = new JLabel(" ");

    // Preferences
    private final JToggleButton autosaveToggle = new JToggleButton();
    private final JComboBox<String> defaultUnits = new JComboBox<>(new String[] {
            "Centimeters (cm)", "Meters (m)", "Inches (in)", "Feet (ft)"
    });

    // Accessibility
    private final JComboBox<String> fontSize = new JComboBox<>(new String[] {
            "Small", "Medium", "Large"
    });
    private final JToggleButton highContrastToggle = new JToggleButton();

    public SettingsPage(AppFrame frame) {
        setLayout(new BorderLayout());
        setBackground(UiKit.BG);
        setBorder(new EmptyBorder(18, 18, 18, 18));

        // Header (title + subtitle)
        JPanel header = buildHeader();
        add(header, BorderLayout.NORTH);

        // Two-column content
        JPanel content = new JPanel(new GridBagLayout());
        content.setOpaque(false);

        GridBagConstraints gc = new GridBagConstraints();
        gc.gridx = 0;
        gc.gridy = 0;
        gc.weightx = 1;
        gc.weighty = 1;
        gc.fill = GridBagConstraints.BOTH;
        gc.insets = new Insets(12, 0, 12, 12);

        JPanel leftCol = new JPanel();
        leftCol.setOpaque(false);
        leftCol.setLayout(new BoxLayout(leftCol, BoxLayout.Y_AXIS));
        leftCol.add(profileCard());
        leftCol.add(Box.createVerticalStrut(14));
        leftCol.add(passwordCard());
        leftCol.add(Box.createVerticalStrut(14));
        leftCol.add(preferencesCard());

        content.add(leftCol, gc);

        gc.gridx = 1;
        gc.insets = new Insets(12, 12, 12, 0);

        JPanel rightCol = new JPanel();
        rightCol.setOpaque(false);
        rightCol.setLayout(new BoxLayout(rightCol, BoxLayout.Y_AXIS));
        rightCol.add(accessibilityCard());
        rightCol.add(Box.createVerticalStrut(14));
        rightCol.add(helpSupportCard());
        rightCol.add(Box.createVerticalStrut(14));
        rightCol.add(accountCard());

        content.add(rightCol, gc);

        JScrollPane sc = new JScrollPane(content);
        sc.setBorder(BorderFactory.createEmptyBorder());
        sc.getVerticalScrollBar().setUnitIncrement(14);
        sc.getViewport().setOpaque(false);
        sc.setOpaque(false);

        add(sc, BorderLayout.CENTER);

        // Footer actions
        add(buildFooterActions(), BorderLayout.SOUTH);

        // Small UI wiring
        styleToggles();
        wirePasswordValidation();
    }

    /* ======================= Header ======================= */

    private JPanel buildHeader() {
        JPanel h = new JPanel();
        h.setOpaque(false);
        h.setLayout(new BoxLayout(h, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("Settings");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 20f));
        title.setForeground(UiKit.TEXT);

        JLabel sub = new JLabel("Manage your account and application preferences");
        sub.setFont(sub.getFont().deriveFont(Font.PLAIN, 12.5f));
        sub.setForeground(UiKit.MUTED);
        sub.setBorder(new EmptyBorder(6, 0, 0, 0));

        h.add(title);
        h.add(sub);
        return h;
    }

    /* ======================= Cards ======================= */

    private JComponent profileCard() {
        UiKit.RoundedPanel card = cardBase();
        card.setLayout(new BorderLayout());
        card.setBorder(new EmptyBorder(14, 14, 14, 14));

        card.add(cardTitle("Profile Information", "Manage your personal details"), BorderLayout.NORTH);

        JPanel form = new JPanel(new GridLayout(2, 2, 14, 12));
        form.setOpaque(false);
        form.setBorder(new EmptyBorder(12, 0, 0, 0));

        form.add(labeledField("Full Name", fullName));
        form.add(labeledField("Email Address", email));
        form.add(labeledField("Job Title", jobTitle));
        form.add(labeledDropdown("Department", department));

        card.add(form, BorderLayout.CENTER);
        return card;
    }

    private JComponent passwordCard() {
        UiKit.RoundedPanel card = cardBase();
        card.setLayout(new BorderLayout());
        card.setBorder(new EmptyBorder(14, 14, 14, 14));

        card.add(cardTitle("Change Password", null), BorderLayout.NORTH);

        JPanel form = new JPanel();
        form.setOpaque(false);
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setBorder(new EmptyBorder(12, 0, 0, 0));

        form.add(labeledPassword("Current Password", currentPw));
        form.add(Box.createVerticalStrut(12));
        form.add(labeledPassword("New Password", newPw));
        form.add(Box.createVerticalStrut(12));
        form.add(labeledPassword("Confirm New Password", confirmPw));
        form.add(Box.createVerticalStrut(8));

        pwError.setFont(pwError.getFont().deriveFont(Font.PLAIN, 11.2f));
        pwError.setForeground(new Color(0xEF4444));
        form.add(pwError);

        card.add(form, BorderLayout.CENTER);
        return card;
    }

    private JComponent preferencesCard() {
        UiKit.RoundedPanel card = cardBase();
        card.setLayout(new BorderLayout());
        card.setBorder(new EmptyBorder(14, 14, 14, 14));

        card.add(cardTitle("Application Preferences", null), BorderLayout.NORTH);

        JPanel body = new JPanel();
        body.setOpaque(false);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBorder(new EmptyBorder(12, 0, 0, 0));

        // Autosave row
        JPanel autosaveRow = new JPanel(new BorderLayout(10, 0));
        autosaveRow.setOpaque(false);

        JPanel text = new JPanel();
        text.setOpaque(false);
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));

        JLabel a1 = new JLabel("Autosave");
        a1.setFont(a1.getFont().deriveFont(Font.BOLD, 12.2f));
        a1.setForeground(UiKit.TEXT);

        JLabel a2 = new JLabel("Automatically save your work every 5 minutes");
        a2.setFont(a2.getFont().deriveFont(Font.PLAIN, 11.2f));
        a2.setForeground(UiKit.MUTED);

        text.add(a1);
        text.add(Box.createVerticalStrut(3));
        text.add(a2);

        autosaveRow.add(text, BorderLayout.WEST);
        autosaveRow.add(autosaveToggle, BorderLayout.EAST);

        body.add(autosaveRow);
        body.add(Box.createVerticalStrut(14));

        // Units dropdown
        body.add(labeledDropdown("Default Units", defaultUnits));

        card.add(body, BorderLayout.CENTER);
        return card;
    }

    private JComponent accessibilityCard() {
        UiKit.RoundedPanel card = cardBase();
        card.setLayout(new BorderLayout());
        card.setBorder(new EmptyBorder(14, 14, 14, 14));

        card.add(cardTitle("Accessibility", null), BorderLayout.NORTH);

        JPanel body = new JPanel();
        body.setOpaque(false);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBorder(new EmptyBorder(12, 0, 0, 0));

        body.add(labeledDropdown("Font Size", fontSize));
        body.add(Box.createVerticalStrut(14));

        JPanel hcRow = new JPanel(new BorderLayout(10, 0));
        hcRow.setOpaque(false);

        JPanel text = new JPanel();
        text.setOpaque(false);
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));

        JLabel t1 = new JLabel("High Contrast");
        t1.setFont(t1.getFont().deriveFont(Font.BOLD, 12.2f));
        t1.setForeground(UiKit.TEXT);

        JLabel t2 = new JLabel("Increase contrast for better visibility");
        t2.setFont(t2.getFont().deriveFont(Font.PLAIN, 11.2f));
        t2.setForeground(UiKit.MUTED);

        text.add(t1);
        text.add(Box.createVerticalStrut(3));
        text.add(t2);

        hcRow.add(text, BorderLayout.WEST);
        hcRow.add(highContrastToggle, BorderLayout.EAST);

        body.add(hcRow);

        card.add(body, BorderLayout.CENTER);
        return card;
    }

    private JComponent helpSupportCard() {
        UiKit.RoundedPanel card = cardBase();
        card.setLayout(new BorderLayout());
        card.setBorder(new EmptyBorder(14, 14, 14, 14));

        card.add(cardTitle("Help & Support", null), BorderLayout.NORTH);

        JPanel body = new JPanel();
        body.setOpaque(false);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBorder(new EmptyBorder(12, 0, 0, 0));

        body.add(linkRow("Documentation"));
        body.add(Box.createVerticalStrut(10));
        body.add(linkRow("Contact Support"));
        body.add(Box.createVerticalStrut(10));
        body.add(linkRow("Keyboard Shortcuts"));
        body.add(Box.createVerticalStrut(10));
        body.add(linkRow("Report a Bug"));

        card.add(body, BorderLayout.CENTER);
        return card;
    }

    private JComponent accountCard() {
        UiKit.RoundedPanel card = cardBase();
        card.setLayout(new BorderLayout());
        card.setBorder(new EmptyBorder(14, 14, 14, 14));

        card.add(cardTitle("Account", null), BorderLayout.NORTH);

        JPanel body = new JPanel();
        body.setOpaque(false);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBorder(new EmptyBorder(12, 0, 0, 0));

        body.add(linkRow("Export Data"));
        body.add(Box.createVerticalStrut(10));
        body.add(linkRow("Privacy Settings"));
        body.add(Box.createVerticalStrut(10));

        JLabel delete = new JLabel("Delete Account");
        delete.setForeground(new Color(0xEF4444));
        delete.setFont(delete.getFont().deriveFont(Font.PLAIN, 12.0f));
        delete.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        delete.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseClicked(java.awt.event.MouseEvent e) {
                int ok = JOptionPane.showConfirmDialog(
                        SettingsPage.this,
                        "This is a demo action for the coursework UI.\nAre you sure you want to continue?",
                        "Delete Account",
                        JOptionPane.OK_CANCEL_OPTION,
                        JOptionPane.WARNING_MESSAGE
                );
                if (ok == JOptionPane.OK_OPTION) {
                    JOptionPane.showMessageDialog(SettingsPage.this, "Account deletion is not implemented (UI demo).");
                }
            }
        });

        body.add(delete);

        card.add(body, BorderLayout.CENTER);
        return card;
    }

    /* ======================= Footer ======================= */

    private JComponent buildFooterActions() {
        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);
        footer.setBorder(new EmptyBorder(14, 0, 0, 0));

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        right.setOpaque(false);

        JButton cancel = UiKit.ghostButton("Cancel");
        JButton save = UiKit.primaryButton("Save Changes");

        cancel.addActionListener(e -> resetDemoValues());
        save.addActionListener(e -> onSave());

        right.add(cancel);
        right.add(save);

        footer.add(right, BorderLayout.EAST);
        return footer;
    }

    private void onSave() {
        // simple safe validation
        String np = new String(newPw.getPassword());
        String cp = new String(confirmPw.getPassword());

        if (!np.isEmpty() || !cp.isEmpty()) {
            if (!np.equals(cp)) {
                pwError.setText("Passwords do not match");
                return;
            }
        }

        pwError.setText(" ");
        JOptionPane.showMessageDialog(
                this,
                "Saved (UI demo).\n\nName: " + fullName.getText().trim() +
                        "\nEmail: " + email.getText().trim() +
                        "\nDepartment: " + department.getSelectedItem(),
                "Settings",
                JOptionPane.INFORMATION_MESSAGE
        );
    }

    private void resetDemoValues() {
        fullName.setText("Sarah Johnson");
        email.setText("sarah.johnson@designstudio.com");
        jobTitle.setText("Senior UI/UX Designer");
        department.setSelectedItem("Design Team");

        currentPw.setText("");
        newPw.setText("");
        confirmPw.setText("");
        pwError.setText(" ");

        autosaveToggle.setSelected(true);
        defaultUnits.setSelectedItem("Centimeters (cm)");

        fontSize.setSelectedItem("Small");
        highContrastToggle.setSelected(false);
    }

    /* ======================= Small UI helpers ======================= */

    private UiKit.RoundedPanel cardBase() {
        UiKit.RoundedPanel card = new UiKit.RoundedPanel(18, Color.WHITE);
        card.setBorderPaint(UiKit.BORDER);
        return card;
    }

    private JComponent cardTitle(String title, String subtitle) {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));

        JLabel t = new JLabel(title);
        t.setFont(t.getFont().deriveFont(Font.BOLD, 13.0f));
        t.setForeground(UiKit.TEXT);

        p.add(t);

        if (subtitle != null && !subtitle.isBlank()) {
            JLabel s = new JLabel(subtitle);
            s.setFont(s.getFont().deriveFont(Font.PLAIN, 11.2f));
            s.setForeground(UiKit.MUTED);
            s.setBorder(new EmptyBorder(4, 0, 0, 0));
            p.add(s);
        }

        return p;
    }

    private JComponent labeledField(String label, JTextField field) {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));

        JLabel l = new JLabel(label);
        l.setFont(l.getFont().deriveFont(Font.PLAIN, 11.2f));
        l.setForeground(UiKit.MUTED);

        field.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(UiKit.BORDER, 1, true),
                new EmptyBorder(9, 10, 9, 10)
        ));

        p.add(l);
        p.add(Box.createVerticalStrut(6));
        p.add(field);
        return p;
    }

    private JComponent labeledPassword(String label, JPasswordField field) {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));

        JLabel l = new JLabel(label);
        l.setFont(l.getFont().deriveFont(Font.PLAIN, 11.2f));
        l.setForeground(UiKit.MUTED);

        field.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(UiKit.BORDER, 1, true),
                new EmptyBorder(9, 10, 9, 10)
        ));

        p.add(l);
        p.add(Box.createVerticalStrut(6));
        p.add(field);
        return p;
    }

    private JComponent labeledDropdown(String label, JComboBox<?> combo) {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));

        JLabel l = new JLabel(label);
        l.setFont(l.getFont().deriveFont(Font.PLAIN, 11.2f));
        l.setForeground(UiKit.MUTED);

        combo.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(UiKit.BORDER, 1, true),
                new EmptyBorder(7, 10, 7, 10)
        ));

        p.add(l);
        p.add(Box.createVerticalStrut(6));
        p.add(combo);
        return p;
    }

    private JComponent linkRow(String text) {
        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(false);

        JLabel l = new JLabel(text);
        l.setFont(l.getFont().deriveFont(Font.PLAIN, 12.0f));
        l.setForeground(new Color(0x111827));
        l.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        JLabel arrow = new JLabel("↗");
        arrow.setForeground(UiKit.MUTED);

        row.add(l, BorderLayout.WEST);
        row.add(arrow, BorderLayout.EAST);

        l.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseClicked(java.awt.event.MouseEvent e) {
                JOptionPane.showMessageDialog(
                        SettingsPage.this,
                        "\"" + text + "\" is a UI demo action (not implemented yet).",
                        "Help & Support",
                        JOptionPane.INFORMATION_MESSAGE
                );
            }
        });

        return row;
    }

    private void styleToggles() {
        // Toggle look like a switch (simple)
        setupSwitch(autosaveToggle, true);
        setupSwitch(highContrastToggle, false);
    }

    private void setupSwitch(JToggleButton b, boolean initial) {
        b.setSelected(initial);
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setPreferredSize(new Dimension(46, 26));
        b.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(UiKit.BORDER, 1, true),
                new EmptyBorder(4, 8, 4, 8)
        ));
        refreshSwitch(b);
        b.addActionListener(e -> refreshSwitch(b));
    }

    private void refreshSwitch(JToggleButton b) {
        if (b.isSelected()) {
            b.setBackground(new Color(0x4F46E5));
            b.setForeground(Color.WHITE);
            b.setText("ON");
        } else {
            b.setBackground(new Color(0xE5E7EB));
            b.setForeground(new Color(0x374151));
            b.setText("OFF");
        }
    }

    private void wirePasswordValidation() {
        // live check to show red error like screenshot
        Runnable check = () -> {
            String np = new String(newPw.getPassword());
            String cp = new String(confirmPw.getPassword());
            if (!cp.isEmpty() && !np.equals(cp)) pwError.setText("Passwords do not match");
            else pwError.setText(" ");
        };

        newPw.addKeyListener(new java.awt.event.KeyAdapter() {
            @Override public void keyReleased(java.awt.event.KeyEvent e) { check.run(); }
        });
        confirmPw.addKeyListener(new java.awt.event.KeyAdapter() {
            @Override public void keyReleased(java.awt.event.KeyEvent e) { check.run(); }
        });
    }
}
