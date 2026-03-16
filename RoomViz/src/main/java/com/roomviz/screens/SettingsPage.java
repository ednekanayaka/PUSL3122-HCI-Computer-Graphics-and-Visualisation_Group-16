package com.roomviz.screens;

import com.roomviz.app.AppFrame;
import com.roomviz.app.Router;
import com.roomviz.app.ScreenKeys;
import com.roomviz.data.AppState;
import com.roomviz.data.SettingsRepository;
import com.roomviz.model.UserSettings;
import com.roomviz.ui.UiKit;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;

/**
 * Settings page UI (functional)
 * - Saves/loads settings from ~/.roomviz/settings.json
 * - Export Data -> exports designs.json content to user-selected JSON
 * - Delete Account -> clears designs + settings and navigates to login
 *
 * ✅ Updated:
 * - Uses UiKit.scaled(...) for ALL fonts (so Small/Medium/Large can increase AND decrease correctly)
 * - Uses UiKit colors everywhere (so High Contrast works)
 * - Toggle setup no longer forces initial ON/OFF (prevents overriding loaded settings)
 * - After Save: triggers onSaved + refreshes Swing UI tree
 */
public class SettingsPage extends JPanel {

    private final AppFrame frame;
    private final Router outerRouter;
    private final AppState appState;
    private final SettingsRepository settingsRepo;
    private final Runnable onSaved;

    // Profile fields
    private final JTextField fullName = new JTextField();
    private final JTextField email = new JTextField();
    private final JTextField jobTitle = new JTextField();
    private final JComboBox<String> department = new JComboBox<>(new String[]{
            "Design Team", "Engineering", "Product", "Operations"
    });

    // Password fields
    private final JPasswordField currentPw = new JPasswordField();
    private final JPasswordField newPw = new JPasswordField();
    private final JPasswordField confirmPw = new JPasswordField();
    private final JLabel pwError = new JLabel(" ");

    // Preferences
    private final JToggleButton autosaveToggle = new JToggleButton();
    private final JComboBox<String> defaultUnits = new JComboBox<>(new String[]{
            "Centimeters (cm)", "Meters (m)", "Inches (in)", "Feet (ft)"
    });

    // Accessibility
    private final JComboBox<String> fontSize = new JComboBox<>(new String[]{
            "Small", "Medium", "Large"
    });
    private final JToggleButton highContrastToggle = new JToggleButton();
    private final JLabel lastSavedLabel = new JLabel(" ");

    public SettingsPage(AppFrame frame, Router outerRouter, AppState appState, SettingsRepository settingsRepo, Runnable onSaved) {
        this.frame = frame;
        this.outerRouter = outerRouter;
        this.appState = appState;
        this.settingsRepo = settingsRepo;
        this.onSaved = onSaved;

        setLayout(new BorderLayout());
        setBackground(UiKit.BG);
        setBorder(new EmptyBorder(18, 18, 18, 18));

        // Header
        JPanel header = buildHeader();
        add(header, BorderLayout.NORTH);

        // Centered Single-Column Content
        JPanel scrollContent = new JPanel(new GridBagLayout());
        scrollContent.setOpaque(false);

        GridBagConstraints gc = new GridBagConstraints();
        gc.gridx = 0;
        gc.gridy = 0;
        gc.weightx = 1;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.anchor = GridBagConstraints.NORTH;
        gc.insets = new Insets(20, 0, 40, 0);

        JPanel mainCol = new JPanel();
        mainCol.setOpaque(false);
        mainCol.setLayout(new BoxLayout(mainCol, BoxLayout.Y_AXIS));
        mainCol.setMaximumSize(new Dimension(800, 9999));
        mainCol.setPreferredSize(new Dimension(800, 1200));

        mainCol.add(profileCard());
        mainCol.add(Box.createVerticalStrut(20));
        mainCol.add(passwordCard());
        mainCol.add(Box.createVerticalStrut(20));
        mainCol.add(preferencesCard());
        mainCol.add(Box.createVerticalStrut(20));
        mainCol.add(accessibilityCard());
        mainCol.add(Box.createVerticalStrut(20));
        mainCol.add(helpSupportCard());
        mainCol.add(Box.createVerticalStrut(20));
        mainCol.add(accountCard());

        scrollContent.add(mainCol, gc);

        JScrollPane sc = new JScrollPane(scrollContent);
        sc.setBorder(BorderFactory.createEmptyBorder());
        sc.getVerticalScrollBar().setUnitIncrement(16);
        sc.getViewport().setOpaque(false);
        sc.setOpaque(false);

        add(sc, BorderLayout.CENTER);

        // Footer actions
        add(buildFooterActions(), BorderLayout.SOUTH);

        // Style controls (DO NOT override selected state here)
        styleControls();
        wirePasswordValidation();

        // Load real saved settings into UI
        loadFromRepo();
    }

    /* ======================= Header ======================= */

    private JPanel buildHeader() {
        JPanel h = new JPanel();
        h.setOpaque(false);
        h.setLayout(new BoxLayout(h, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("Settings");
        title.setFont(UiKit.scaled(title, Font.BOLD, 1.35f));
        title.setForeground(UiKit.TEXT);
        title.setAlignmentX(0.0f);

        JLabel sub = new JLabel("Manage your account and application preferences");
        sub.setFont(UiKit.scaled(sub, Font.PLAIN, 0.98f));
        sub.setForeground(UiKit.MUTED);
        sub.setBorder(new EmptyBorder(6, 0, 0, 0));
        sub.setAlignmentX(0.0f);

        h.add(title);
        h.add(sub);
        return h;
    }

    /* ======================= Cards ======================= */

    private JComponent profileCard() {
        UiKit.RoundedPanel card = cardBase();
        card.setLayout(new BorderLayout());
        card.setBorder(new EmptyBorder(18, 18, 18, 18));

        card.add(cardTitle("👤", "Profile Information", "Manage your personal details"), BorderLayout.NORTH);

        JPanel form = new JPanel(new GridLayout(2, 2, 16, 16));
        form.setOpaque(false);
        form.setBorder(new EmptyBorder(16, 0, 0, 0));

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
        card.setBorder(new EmptyBorder(18, 18, 18, 18));

        card.add(cardTitle("🔒", "Change Password", "Secure your account"), BorderLayout.NORTH);

        JPanel form = new JPanel();
        form.setOpaque(false);
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setBorder(new EmptyBorder(16, 0, 0, 0));

        form.add(labeledPassword("Current Password", currentPw));
        form.add(Box.createVerticalStrut(14));
        form.add(labeledPassword("New Password", newPw));
        form.add(Box.createVerticalStrut(14));
        form.add(labeledPassword("Confirm New Password", confirmPw));
        form.add(Box.createVerticalStrut(10));

        pwError.setFont(UiKit.scaled(pwError, Font.PLAIN, 0.88f));
        pwError.setForeground(UiKit.DANGER);
        form.add(pwError);

        card.add(form, BorderLayout.CENTER);
        return card;
    }

    private JComponent preferencesCard() {
        UiKit.RoundedPanel card = cardBase();
        card.setLayout(new BorderLayout());
        card.setBorder(new EmptyBorder(18, 18, 18, 18));

        card.add(cardTitle("🛠️", "Application Preferences", "Adjust your design experience"), BorderLayout.NORTH);

        JPanel body = new JPanel();
        body.setOpaque(false);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBorder(new EmptyBorder(16, 0, 0, 0));

        // Autosave row
        JPanel autosaveRow = new JPanel(new BorderLayout(14, 0));
        autosaveRow.setOpaque(false);
        autosaveRow.setAlignmentX(0.0f);
        autosaveRow.setMaximumSize(new Dimension(800, 50));

        JPanel text = new JPanel();
        text.setOpaque(false);
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        text.setAlignmentX(0.0f);

        JLabel a1 = new JLabel("Autosave");
        a1.setFont(UiKit.scaled(a1, Font.BOLD, 1.00f));
        a1.setForeground(UiKit.TEXT);
        a1.setAlignmentX(0.0f);

        JLabel a2 = new JLabel("Automatically save your work while editing designs");
        a2.setFont(UiKit.scaled(a2, Font.PLAIN, 0.90f));
        a2.setForeground(UiKit.MUTED);
        a2.setAlignmentX(0.0f);

        text.add(a1);
        text.add(Box.createVerticalStrut(3));
        text.add(a2);

        autosaveRow.add(text, BorderLayout.WEST);
        autosaveRow.add(autosaveToggle, BorderLayout.EAST);

        body.add(autosaveRow);
        body.add(Box.createVerticalStrut(18));

        // Units dropdown
        body.add(labeledDropdown("Default Units", defaultUnits));
        body.add(Box.createVerticalStrut(18));

        JButton resetBtn = UiKit.ghostButton("Reset to Defaults");
        resetBtn.addActionListener(e -> {
            int ok = JOptionPane.showConfirmDialog(this, "Reset all settings to defaults?", "Reset", JOptionPane.OK_CANCEL_OPTION);
            if (ok == JOptionPane.OK_OPTION) {
                applyToUi(UserSettings.defaults());
            }
        });
        JPanel btnWrap = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        btnWrap.setOpaque(false);
        btnWrap.setAlignmentX(0.0f);
        btnWrap.setMaximumSize(new Dimension(800, 40));
        btnWrap.add(resetBtn);
        body.add(btnWrap);

        card.add(body, BorderLayout.CENTER);
        return card;
    }

    private JComponent accessibilityCard() {
        UiKit.RoundedPanel card = cardBase();
        card.setLayout(new BorderLayout());
        card.setBorder(new EmptyBorder(18, 18, 18, 18));

        card.add(cardTitle("👁️", "Accessibility", "Tailor the interface to your needs"), BorderLayout.NORTH);

        JPanel body = new JPanel();
        body.setOpaque(false);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBorder(new EmptyBorder(16, 0, 0, 0));

        body.add(labeledDropdown("Font Size", fontSize));
        body.add(Box.createVerticalStrut(18));

        JPanel hcRow = new JPanel(new BorderLayout(14, 0));
        hcRow.setOpaque(false);
        hcRow.setAlignmentX(0.0f);
        hcRow.setMaximumSize(new Dimension(800, 50));

        JPanel text = new JPanel();
        text.setOpaque(false);
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        text.setAlignmentX(0.0f);

        JLabel t1 = new JLabel("High Contrast");
        t1.setFont(UiKit.scaled(t1, Font.BOLD, 1.00f));
        t1.setForeground(UiKit.TEXT);
        t1.setAlignmentX(0.0f);

        JLabel t2 = new JLabel("Increase contrast for better visibility");
        t2.setFont(UiKit.scaled(t2, Font.PLAIN, 0.90f));
        t2.setForeground(UiKit.MUTED);
        t2.setAlignmentX(0.0f);

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
        card.setBorder(new EmptyBorder(18, 18, 18, 18));

        card.add(cardTitle("❓", "Help & Support", "Get assistance or read docs"), BorderLayout.NORTH);

        JPanel body = new JPanel();
        body.setOpaque(false);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBorder(new EmptyBorder(16, 0, 0, 0));

        body.add(linkRow("Documentation", this::showDocs));
        body.add(Box.createVerticalStrut(12));
        body.add(linkRow("Contact Support", this::showSupport));
        body.add(Box.createVerticalStrut(12));
        body.add(linkRow("Keyboard Shortcuts", this::showShortcuts));
        body.add(Box.createVerticalStrut(12));
        body.add(linkRow("Report a Bug", this::showBugReport));

        card.add(body, BorderLayout.CENTER);
        return card;
    }

    private JComponent accountCard() {
        UiKit.RoundedPanel card = cardBase();
        card.setLayout(new BorderLayout());
        card.setBorder(new EmptyBorder(18, 18, 18, 18));

        card.add(cardTitle("⚙️", "Account", "Manage your data and privacy"), BorderLayout.NORTH);

        JPanel body = new JPanel();
        body.setOpaque(false);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBorder(new EmptyBorder(16, 0, 0, 0));

        body.add(linkRow("Export Data", this::onExportData));
        body.add(Box.createVerticalStrut(12));
        body.add(linkRow("Privacy Settings", this::showPrivacy));
        body.add(Box.createVerticalStrut(12));

        JLabel delete = new JLabel("Delete Account");
        delete.setForeground(UiKit.DANGER);
        delete.setFont(UiKit.scaled(delete, Font.BOLD, 0.98f));
        delete.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        delete.setAlignmentX(0.0f);
        delete.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseClicked(java.awt.event.MouseEvent e) {
                onDeleteAccount();
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

        cancel.setFont(UiKit.scaled(cancel, Font.PLAIN, 1.00f));
        save.setFont(UiKit.scaled(save, Font.BOLD, 1.00f));

        cancel.addActionListener(e -> loadFromRepo());
        save.addActionListener(e -> onSave());

        right.add(cancel);
        right.add(save);

        lastSavedLabel.setFont(UiKit.scaled(lastSavedLabel, Font.PLAIN, 0.85f));
        lastSavedLabel.setForeground(UiKit.MUTED);
        footer.add(lastSavedLabel, BorderLayout.WEST);

        footer.add(right, BorderLayout.EAST);
        return footer;
    }

    /* ======================= REAL LOGIC ======================= */

    private void loadFromRepo() {
        UserSettings s = (settingsRepo == null) ? UserSettings.defaults() : settingsRepo.reload();
        applyToUi(s);

        currentPw.setText("");
        newPw.setText("");
        confirmPw.setText("");
        pwError.setText(" ");
    }

    private void applyToUi(UserSettings s) {
        if (s == null) s = UserSettings.defaults();

        fullName.setText(s.getFullName());
        email.setText(s.getEmail());
        jobTitle.setText(s.getJobTitle());

        String dept = s.getDepartment();
        if (dept != null && !dept.isBlank()) department.setSelectedItem(dept);

        autosaveToggle.setSelected(s.isAutosaveEnabled());
        refreshSwitch(autosaveToggle);

        defaultUnits.setSelectedItem(unitCodeToLabel(s.getDefaultUnit()));

        String fs = s.getFontSize();
        fontSize.setSelectedItem((fs == null || fs.isBlank()) ? "Small" : fs);

        highContrastToggle.setSelected(s.isHighContrast());
        refreshSwitch(highContrastToggle);
    }

    private UserSettings collectFromUi() {
        UserSettings s = new UserSettings();
        s.setFullName(fullName.getText());
        s.setEmail(email.getText());
        s.setJobTitle(jobTitle.getText());
        s.setDepartment(String.valueOf(department.getSelectedItem()));

        s.setAutosaveEnabled(autosaveToggle.isSelected());
        s.setDefaultUnit(unitLabelToCode(String.valueOf(defaultUnits.getSelectedItem())));

        s.setFontSize(String.valueOf(fontSize.getSelectedItem()));
        s.setHighContrast(highContrastToggle.isSelected());
        return s;
    }

    private void onSave() {
        if (settingsRepo == null) {
            JOptionPane.showMessageDialog(this, "Settings repository not available.");
            return;
        }

        String cur = new String(currentPw.getPassword()).trim();
        String np = new String(newPw.getPassword()).trim();
        String cp = new String(confirmPw.getPassword()).trim();

        if (!np.isEmpty() || !cp.isEmpty()) {
            if (!np.equals(cp)) {
                pwError.setText("Passwords do not match");
                return;
            }

            String existing = settingsRepo.get().getPasswordPlain();
            if (existing != null && !existing.isBlank()) {
                if (cur.isBlank() || !existing.equals(cur)) {
                    pwError.setText("Current password is incorrect");
                    return;
                }
            }
        }

        pwError.setText(" ");

        UserSettings s = collectFromUi();

        if (np.isBlank()) {
            s.setPasswordPlain(settingsRepo.get().getPasswordPlain());
        } else {
            s.setPasswordPlain(np);
        }

        settingsRepo.save(s);

        // Update last saved
        java.time.format.DateTimeFormatter dtf = java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss");
        lastSavedLabel.setText("Last saved: " + java.time.LocalTime.now().format(dtf));

        // ✅ notify shell + apply global UI refresh
        if (onSaved != null) onSaved.run();
        SwingUtilities.invokeLater(() -> {
            try {
                SwingUtilities.updateComponentTreeUI(frame);
            } catch (Exception ignored) {}
            frame.invalidate();
            frame.validate();
            frame.repaint();
        });

        currentPw.setText("");
        newPw.setText("");
        confirmPw.setText("");
    }

    private void onExportData() {
        if (appState == null || appState.getRepo() == null) {
            JOptionPane.showMessageDialog(this, "Design repository not available.");
            return;
        }

        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Export RoomViz Designs");
        chooser.setSelectedFile(new java.io.File("roomviz-designs-export.json"));

        int res = chooser.showSaveDialog(this);
        if (res != JFileChooser.APPROVE_OPTION) return;

        java.io.File out = chooser.getSelectedFile();
        appState.getRepo().exportTo(out);

        JOptionPane.showMessageDialog(
                this,
                "Exported designs to:\n" + out.getAbsolutePath(),
                "Export Complete",
                JOptionPane.INFORMATION_MESSAGE
        );
    }

    private void onDeleteAccount() {
        int ok = JOptionPane.showConfirmDialog(
                this,
                "This will remove ALL local data:\n" +
                        "• Saved designs\n" +
                        "• Saved settings\n\n" +
                        "Continue?",
                "Delete Account",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.WARNING_MESSAGE
        );

        if (ok != JOptionPane.OK_OPTION) return;

        try {
            if (appState != null && appState.getRepo() != null) {
                appState.getRepo().clearAll();
            }
            if (settingsRepo != null) {
                settingsRepo.clearAll();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        JOptionPane.showMessageDialog(
                this,
                "Local account data deleted.\nReturning to Login.",
                "Deleted",
                JOptionPane.INFORMATION_MESSAGE
        );

        if (outerRouter != null) outerRouter.show(ScreenKeys.LOGIN);
    }

    /* ======================= Help dialogs ======================= */

    private void showDocs() {
        JOptionPane.showMessageDialog(
                this,
                "RoomViz Documentation (prototype)\n\n" +
                        "• Create a design in Design Library\n" +
                        "• Open 2D Planner to place furniture\n" +
                        "• Use Shading & Colour tools for styling\n" +
                        "• Open 3D Visual for preview\n",
                "Documentation",
                JOptionPane.INFORMATION_MESSAGE
        );
    }

    private void showSupport() {
        JOptionPane.showMessageDialog(
                this,
                "Contact Support (prototype)\n\n" +
                        "This is a coursework prototype.\n" +
                        "You can add a real email / form later.",
                "Support",
                JOptionPane.INFORMATION_MESSAGE
        );
    }

    private void showShortcuts() {
        JOptionPane.showMessageDialog(
                this,
                "Keyboard Shortcuts (prototype)\n\n" +
                        "• Delete: Remove selected item\n" +
                        "• (Add more later: Ctrl+Z, Ctrl+Y etc.)",
                "Keyboard Shortcuts",
                JOptionPane.INFORMATION_MESSAGE
        );
    }

    private void showBugReport() {
        JOptionPane.showMessageDialog(
                this,
                "Report a Bug (prototype)\n\n" +
                        "Add a bug report form or GitHub issues link later.",
                "Report a Bug",
                JOptionPane.INFORMATION_MESSAGE
        );
    }

    private void showPrivacy() {
        JOptionPane.showMessageDialog(
                this,
                "Privacy Settings\n\n" +
                        "• This app stores data locally on your device.\n" +
                        "• No cloud sync is implemented in this prototype.",
                "Privacy Settings",
                JOptionPane.INFORMATION_MESSAGE
        );
    }

    /* ======================= UI helpers ======================= */

    private void styleControls() {
        // Ensure consistent font + colors on inputs (important for High Contrast)
        styleTextField(fullName);
        styleTextField(email);
        styleTextField(jobTitle);

        stylePasswordField(currentPw);
        stylePasswordField(newPw);
        stylePasswordField(confirmPw);

        UiKit.styleDropdown(department);
        UiKit.styleDropdown(defaultUnits);
        UiKit.styleDropdown(fontSize);

        // Switches: do NOT force selected state here
        setupSwitch(autosaveToggle);
        setupSwitch(highContrastToggle);

        // ensure switch looks correct when selection changes
        autosaveToggle.addActionListener(e -> refreshSwitch(autosaveToggle));
        highContrastToggle.addActionListener(e -> refreshSwitch(highContrastToggle));
    }

    private void styleTextField(JTextField field) {
        field.setFont(UiKit.scaled(field, Font.PLAIN, 1.00f));
        field.setForeground(UiKit.TEXT);
        field.setBackground(UiKit.WHITE);
        field.setCaretColor(UiKit.TEXT);
        field.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(UiKit.BORDER, 1, true),
                new EmptyBorder(9, 10, 9, 10)
        ));
    }

    private void stylePasswordField(JPasswordField field) {
        field.setFont(UiKit.scaled(field, Font.PLAIN, 1.00f));
        field.setForeground(UiKit.TEXT);
        field.setBackground(UiKit.WHITE);
        field.setCaretColor(UiKit.TEXT);
        field.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(UiKit.BORDER, 1, true),
                new EmptyBorder(9, 10, 9, 10)
        ));
    }

    private UiKit.RoundedPanel cardBase() {
        UiKit.RoundedPanel card = new UiKit.RoundedPanel(18, UiKit.WHITE);
        card.setBorderPaint(UiKit.BORDER);
        return card;
    }

    private JComponent cardTitle(String icon, String title, String subtitle) {
        JPanel wrapper = new JPanel(new BorderLayout(14, 0));
        wrapper.setOpaque(false);
        wrapper.setAlignmentX(0.0f);

        JLabel iconLbl = new JLabel(icon);
        iconLbl.setFont(UiKit.scaled(iconLbl, Font.PLAIN, 1.4f));
        iconLbl.setForeground(UiKit.PRIMARY);

        JPanel text = new JPanel();
        text.setOpaque(false);
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        text.setAlignmentX(0.0f);

        JLabel t = new JLabel(title);
        t.setFont(UiKit.scaled(t, Font.BOLD, 1.05f));
        t.setForeground(UiKit.TEXT);
        t.setAlignmentX(0.0f);
        text.add(t);

        if (subtitle != null && !subtitle.isBlank()) {
            JLabel s = new JLabel(subtitle);
            s.setFont(UiKit.scaled(s, Font.PLAIN, 0.88f));
            s.setForeground(UiKit.MUTED);
            s.setBorder(new EmptyBorder(2, 0, 0, 0));
            s.setAlignmentX(0.0f);
            text.add(s);
        }

        wrapper.add(iconLbl, BorderLayout.WEST);
        wrapper.add(text, BorderLayout.CENTER);

        return wrapper;
    }

    private JComponent labeledField(String label, JTextField field) {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setAlignmentX(0.0f);

        JLabel l = new JLabel(label);
        l.setFont(UiKit.scaled(l, Font.PLAIN, 0.88f));
        l.setForeground(UiKit.MUTED);
        l.setAlignmentX(0.0f);

        p.add(l);
        p.add(Box.createVerticalStrut(6));
        field.setAlignmentX(0.0f);
        p.add(field);
        return p;
    }

    private JComponent labeledPassword(String label, JPasswordField field) {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setAlignmentX(0.0f);

        JLabel l = new JLabel(label);
        l.setFont(UiKit.scaled(l, Font.PLAIN, 0.88f));
        l.setForeground(UiKit.MUTED);
        l.setAlignmentX(0.0f);

        p.add(l);
        p.add(Box.createVerticalStrut(6));
        field.setAlignmentX(0.0f);
        p.add(field);
        return p;
    }

    private JComponent labeledDropdown(String label, JComboBox<?> combo) {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));

        JLabel l = new JLabel(label);
        l.setFont(UiKit.scaled(l, Font.PLAIN, 0.88f));
        l.setForeground(UiKit.MUTED);
        l.setAlignmentX(0.0f);

        combo.setFont(UiKit.scaled(combo, Font.PLAIN, 1.00f));
        combo.setForeground(UiKit.TEXT);
        combo.setBackground(UiKit.WHITE);
        combo.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(UiKit.BORDER, 1, true),
                new EmptyBorder(7, 10, 7, 10)
        ));
        combo.setAlignmentX(0.0f);

        p.add(l);
        p.add(Box.createVerticalStrut(6));
        p.add(combo);
        return p;
    }

    private JComponent linkRow(String text, Runnable onClick) {
        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(false);
        row.setAlignmentX(0.0f);
        row.setMaximumSize(new Dimension(800, 30));

        JLabel l = new JLabel(text);
        l.setFont(UiKit.scaled(l, Font.PLAIN, 0.98f));
        l.setForeground(UiKit.TEXT);
        l.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        JLabel arrow = new JLabel("↗");
        arrow.setForeground(UiKit.MUTED);

        row.add(l, BorderLayout.WEST);
        row.add(arrow, BorderLayout.EAST);

        l.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseClicked(java.awt.event.MouseEvent e) {
                if (onClick != null) onClick.run();
            }
        });

        return row;
    }

    private void setupSwitch(JToggleButton b) {
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setPreferredSize(new Dimension(46, 26));
        b.setFont(UiKit.scaled(b, Font.BOLD, 0.80f));
        b.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(UiKit.BORDER, 1, true),
                new EmptyBorder(4, 8, 4, 8)
        ));
        refreshSwitch(b);
    }

    private void refreshSwitch(JToggleButton b) {
        if (b.isSelected()) {
            b.setBackground(UiKit.PRIMARY);
            b.setForeground(Color.WHITE);
            b.setText("  ON  ");
            b.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(UiKit.PRIMARY.darker(), 1, true),
                new EmptyBorder(4, 10, 4, 10)
            ));
        } else {
            b.setBackground(new Color(0xE5E7EB));
            b.setForeground(new Color(0x374151));
            b.setText("  OFF  ");
            b.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(0xD1D5DB), 1, true),
                new EmptyBorder(4, 10, 4, 10)
            ));
        }
    }

    private void wirePasswordValidation() {
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

    private static String unitLabelToCode(String label) {
        if (label == null) return "cm";
        if (label.contains("Feet")) return "ft";
        if (label.contains("Inches")) return "in";
        if (label.contains("Meters")) return "m";
        return "cm";
    }

    private static String unitCodeToLabel(String code) {
        if (code == null) return "Centimeters (cm)";
        return switch (code) {
            case "ft" -> "Feet (ft)";
            case "in" -> "Inches (in)";
            case "m" -> "Meters (m)";
            default -> "Centimeters (cm)";
        };
    }
}