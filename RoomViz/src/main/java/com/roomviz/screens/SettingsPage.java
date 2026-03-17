package com.roomviz.screens;

import com.roomviz.app.AppFrame;
import com.roomviz.app.Router;
import com.roomviz.app.ScreenKeys;
import com.roomviz.data.AppState;
import com.roomviz.data.Session;
import com.roomviz.data.SettingsRepository;
import com.roomviz.data.UserRepository;
import com.roomviz.model.User;
import com.roomviz.model.UserSettings;
import com.roomviz.ui.FontAwesome;
import com.roomviz.ui.UiKit;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;

/**
 * Settings page for profile, password, preferences, and account management.
 */
public class SettingsPage extends JPanel {

    private final AppFrame frame;
    private final Router outerRouter;
    private final AppState appState;
    private final SettingsRepository settingsRepo;
    private final UserRepository userRepo;
    private final Session session;
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
            "Feet (ft)", "Meters (m)"
    });

    // Accessibility
    private final JComboBox<String> themeMode = new JComboBox<>(new String[]{
            "Light", "Dark Blue"
    });
    private final JComboBox<String> fontSize = new JComboBox<>(new String[]{
            "Small", "Medium", "Large"
    });
    private final JToggleButton highContrastToggle = new JToggleButton();
    private final JLabel lastSavedLabel = new JLabel(" ");

    public SettingsPage(AppFrame frame,
                        Router outerRouter,
                        AppState appState,
                        SettingsRepository settingsRepo,
                        UserRepository userRepo,
                        Session session,
                        Runnable onSaved) {

        this.frame = frame;
        this.outerRouter = outerRouter;
        this.appState = appState;
        this.settingsRepo = settingsRepo;
        this.userRepo = userRepo;
        this.session = session;
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
        gc.weighty = 1;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.anchor = GridBagConstraints.NORTH;
        gc.insets = new Insets(20, 0, 40, 0);

        JPanel mainCol = new JPanel();
        mainCol.setOpaque(false);
        mainCol.setLayout(new BoxLayout(mainCol, BoxLayout.Y_AXIS));
        mainCol.setMaximumSize(new Dimension(800, Integer.MAX_VALUE));

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

        styleControls();
        wirePasswordValidation();

        loadFromRepo();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        if (!UiKit.isHighContrastMode() && !UiKit.isDarkBlueMode()) {
            int w = getWidth();
            int h = getHeight();
            
            // Base Gradient: Soft purple to soft cyan
            LinearGradientPaint lgp = new LinearGradientPaint(
                    0, 0, w, h,
                    new float[]{ 0.0f, 0.5f, 1.0f },
                    new Color[]{ new Color(223, 172, 255), new Color(210, 190, 250), new Color(130, 240, 240) }
            );
            g2.setPaint(lgp);
            g2.fillRect(0, 0, w, h);
            
            // Abstract wave 1
            g2.setPaint(new Color(255, 255, 255, 60));
            java.awt.geom.Path2D wave = new java.awt.geom.Path2D.Double();
            wave.moveTo(0, h * 0.4);
            wave.curveTo(w * 0.3, h * 0.6, w * 0.6, h * 0.2, w, h * 0.5);
            wave.lineTo(w, h);
            wave.lineTo(0, h);
            wave.closePath();
            g2.fill(wave);
            
            // Abstract wave 2
            g2.setPaint(new Color(255, 255, 255, 30));
            java.awt.geom.Path2D wave2 = new java.awt.geom.Path2D.Double();
            wave2.moveTo(0, h * 0.6);
            wave2.curveTo(w * 0.4, h * 0.8, w * 0.8, h * 0.3, w, h * 0.7);
            wave2.lineTo(w, h);
            wave2.lineTo(0, h);
            wave2.closePath();
            g2.fill(wave2);

        } else {
            g2.setColor(UiKit.BG);
            g2.fillRect(0, 0, getWidth(), getHeight());
        }
        g2.dispose();
    }

    // --- Header ---

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

    // --- Cards ---

    private JComponent profileCard() {
        UiKit.RoundedPanel card = cardBase();
        card.setLayout(new BorderLayout());
        card.setBorder(new EmptyBorder(18, 18, 18, 18));

        card.add(cardTitle(FontAwesome.USER, "Profile Information", "Manage your personal details"), BorderLayout.NORTH);

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

        card.add(cardTitle(FontAwesome.LOCK, "Change Password", "Secure your account"), BorderLayout.NORTH);

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

        card.add(cardTitle(FontAwesome.WRENCH, "Application Preferences", "Adjust your design experience"), BorderLayout.NORTH);

        JPanel body = new JPanel();
        body.setOpaque(false);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBorder(new EmptyBorder(16, 0, 0, 0));

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

        body.add(labeledDropdown("Default Units", defaultUnits));
        body.add(Box.createVerticalStrut(18));

        JButton resetBtn = UiKit.ghostButton("Reset to Defaults");
        resetBtn.addActionListener(e -> {
            int ok = JOptionPane.showConfirmDialog(this, "Reset all settings to defaults?", "Reset", JOptionPane.OK_CANCEL_OPTION);
            if (ok == JOptionPane.OK_OPTION) {
                applyPreferencesToUi(UserSettings.defaults());
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

        card.add(cardTitle(FontAwesome.EYE, "Accessibility", "Tailor the interface to your needs"), BorderLayout.NORTH);

        JPanel body = new JPanel();
        body.setOpaque(false);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBorder(new EmptyBorder(16, 0, 0, 0));

        body.add(labeledDropdown("Theme Mode", themeMode));
        body.add(Box.createVerticalStrut(18));

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

        card.add(cardTitle(FontAwesome.QUESTION_CIRCLE, "Help & Support", "Get assistance or read docs"), BorderLayout.NORTH);

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

        card.add(cardTitle(FontAwesome.GEAR, "Account", "Manage your data and privacy"), BorderLayout.NORTH);

        JPanel body = new JPanel();
        body.setOpaque(false);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBorder(new EmptyBorder(16, 0, 0, 0));

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

    // --- Footer ---

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

    // --- Load/Save ---

    private void loadFromRepo() {
        // Preferences from DB (per-user)
        UserSettings prefs = (settingsRepo == null) ? UserSettings.defaults() : settingsRepo.reload();
        applyPreferencesToUi(prefs);

        // Profile from session/userRepo (DB)
        User u = (session != null && session.isLoggedIn()) ? session.getCurrentUser() : null;
        if (u != null && userRepo != null) {
            User fresh = userRepo.findById(u.getId());
            if (fresh != null) u = fresh;
        }
        applyProfileToUi(u);

        currentPw.setText("");
        newPw.setText("");
        confirmPw.setText("");
        pwError.setText(" ");
    }

    private void applyProfileToUi(User u) {
        if (u == null) {
            fullName.setText("");
            email.setText("");
            jobTitle.setText("");
            department.setSelectedIndex(0);
            return;
        }

        fullName.setText(safe(u.getFullName()));
        email.setText(safe(u.getEmail()));
        email.setEditable(false);

        jobTitle.setText(safe(u.getJobTitle()));
        String dept = safe(u.getDepartment());
        if (!dept.isBlank()) department.setSelectedItem(dept);
    }

    private void applyPreferencesToUi(UserSettings s) {
        if (s == null) s = UserSettings.defaults();

        autosaveToggle.setSelected(s.isAutosaveEnabled());
        refreshSwitch(autosaveToggle);

        defaultUnits.setSelectedItem(unitCodeToLabel(s.getDefaultUnit()));

        String fs = s.getFontSize();
        fontSize.setSelectedItem((fs == null || fs.isBlank()) ? "Small" : fs);

        themeMode.setSelectedItem(themeCodeToLabel(s.getThemeMode()));

        highContrastToggle.setSelected(s.isHighContrast());
        refreshSwitch(highContrastToggle);
    }

    private UserSettings collectPreferencesFromUi() {
        UserSettings s = new UserSettings();
        s.setAutosaveEnabled(autosaveToggle.isSelected());
        s.setDefaultUnit(unitLabelToCode(String.valueOf(defaultUnits.getSelectedItem())));
        s.setFontSize(String.valueOf(fontSize.getSelectedItem()));
        s.setThemeMode(themeLabelToCode(String.valueOf(themeMode.getSelectedItem())));
        s.setHighContrast(highContrastToggle.isSelected());
        return s;
    }

    private void onSave() {
        if (settingsRepo == null || userRepo == null || session == null || !session.isLoggedIn()) {
            JOptionPane.showMessageDialog(this, "Not logged in or repositories not available.");
            return;
        }

        int uid = session.getCurrentUser().getId();

        // ===== password change (optional) =====
        String cur = new String(currentPw.getPassword()).trim();
        String np = new String(newPw.getPassword()).trim();
        String cp = new String(confirmPw.getPassword()).trim();

        if (!np.isEmpty() || !cp.isEmpty()) {
            if (!np.equals(cp)) {
                pwError.setText("Passwords do not match");
                return;
            }
            if (cur.isBlank()) {
                pwError.setText("Enter current password to change password");
                return;
            }
            boolean ok = userRepo.verifyPasswordById(uid, currentPw.getPassword());
            if (!ok) {
                pwError.setText("Current password is incorrect");
                return;
            }
        }

        pwError.setText(" ");

        // ===== save profile to DB =====
        try {
            userRepo.updateProfile(
                    uid,
                    fullName.getText(),
                    jobTitle.getText(),
                    String.valueOf(department.getSelectedItem())
            );

            // if changing password
            if (!np.isBlank()) {
                userRepo.updatePasswordById(uid, newPw.getPassword());
            }

            // refresh session user so TopBar updates immediately
            User fresh = userRepo.findById(uid);
            if (fresh != null) {
                session.login(fresh);
            }

        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Failed to save profile/password.");
            return;
        }

        // ===== save preferences to DB (per-user) =====
        UserSettings prefs = collectPreferencesFromUi();
        settingsRepo.save(prefs);

        // Update last saved
        java.time.format.DateTimeFormatter dtf = java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss");
        lastSavedLabel.setText("Last saved: " + java.time.LocalTime.now().format(dtf));

        // notify shell
        if (onSaved != null) onSaved.run();

        SwingUtilities.invokeLater(() -> {
            try { SwingUtilities.updateComponentTreeUI(frame); } catch (Exception ignored) {}
            frame.invalidate();
            frame.validate();
            frame.repaint();
        });

        currentPw.setText("");
        newPw.setText("");
        confirmPw.setText("");
    }

    /**
     * Deletes the actual user account (SQLite user row),
     * clears local designs + preferences, logs out, resets cursor, routes to login.
     */
    private void onDeleteAccount() {
        if (session == null || !session.isLoggedIn()) {
            JOptionPane.showMessageDialog(this, "Not logged in.");
            return;
        }

        User u = session.getCurrentUser();
        if (u == null) return;

        int ok = JOptionPane.showConfirmDialog(
                this,
                "This will PERMANENTLY delete this local account and ALL its data:\n" +
                        "• Account login (SQLite user)\n" +
                        "• Saved designs\n" +
                        "• Saved preferences\n\n" +
                        "Continue?",
                "Delete Account",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.WARNING_MESSAGE
        );

        if (ok != JOptionPane.OK_OPTION) return;

        // show wait cursor immediately
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        if (frame != null) frame.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            Exception error = null;

            @Override
            protected Void doInBackground() {
                try {
                    // 1) Clear designs + preferences
                    if (appState != null && appState.getRepo() != null) {
                        appState.getRepo().clearAll();
                    }
                    if (settingsRepo != null) {
                        settingsRepo.clearAll();
                    }

                    // 2) Delete the actual SQLite user record (this stops re-login)
                    if (userRepo != null) {
                        userRepo.deleteUserById(u.getId());
                    }

                    // 3) End session
                    session.logout();

                } catch (Exception ex) {
                    error = ex;
                }
                return null;
            }

            @Override
            protected void done() {
                // ALWAYS restore cursor (fixes spinning cursor)
                setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                if (frame != null) frame.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));

                if (error != null) {
                    error.printStackTrace();
                    JOptionPane.showMessageDialog(
                            SettingsPage.this,
                            "Failed to delete account.\nCheck console for details.",
                            "Error",
                            JOptionPane.ERROR_MESSAGE
                    );
                    return;
                }

                JOptionPane.showMessageDialog(
                        SettingsPage.this,
                        "Account deleted.\nReturning to Login.",
                        "Deleted",
                        JOptionPane.INFORMATION_MESSAGE
                );

                // Optional: reset UI to default settings (avoids carrying a weird state to login)
                UiKit.applySettings(UserSettings.defaults());
                try { SwingUtilities.updateComponentTreeUI(frame); } catch (Exception ignored) {}
                if (frame != null) {
                    frame.invalidate();
                    frame.validate();
                    frame.repaint();
                }

                if (outerRouter != null) outerRouter.show(ScreenKeys.LOGIN);
            }
        };

        worker.execute();
    }

    // --- Help dialogs ---

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

    // --- UI helpers ---

    private void styleControls() {
        styleTextField(fullName);
        styleTextField(email);
        styleTextField(jobTitle);

        stylePasswordField(currentPw);
        stylePasswordField(newPw);
        stylePasswordField(confirmPw);

        UiKit.styleDropdown(department);
        UiKit.styleDropdown(defaultUnits);
        UiKit.styleDropdown(themeMode);
        UiKit.styleDropdown(fontSize);

        setupSwitch(autosaveToggle);
        setupSwitch(highContrastToggle);

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
        iconLbl.setFont(FontAwesome.solid(20f));
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
        row.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        JLabel l = new JLabel(text);
        l.setFont(UiKit.scaled(l, Font.PLAIN, 0.98f));
        l.setForeground(UiKit.TEXT);
        l.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        JLabel arrow = new JLabel("↗");
        arrow.setForeground(UiKit.MUTED);
        arrow.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        row.add(l, BorderLayout.WEST);
        row.add(arrow, BorderLayout.EAST);

        java.awt.event.MouseAdapter click = new java.awt.event.MouseAdapter() {
            @Override public void mouseClicked(java.awt.event.MouseEvent e) {
                if (onClick != null) onClick.run();
            }
        };

        row.addMouseListener(click);
        l.addMouseListener(click);
        arrow.addMouseListener(click);

        return row;
    }

    private void setupSwitch(JToggleButton b) {
        b.setFocusPainted(false);
        b.setContentAreaFilled(true);
        b.setBorderPainted(true);
        b.setOpaque(true);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setHorizontalAlignment(SwingConstants.CENTER);
        b.setMargin(new Insets(0, 0, 0, 0));
        b.setPreferredSize(new Dimension(74, 30));
        b.setMinimumSize(new Dimension(74, 30));
        b.setMaximumSize(new Dimension(74, 30));
        b.setFont(UiKit.scaled(b, Font.BOLD, 0.80f));
        b.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(UiKit.BORDER, 1, true),
                new EmptyBorder(3, 8, 3, 8)
        ));
        refreshSwitch(b);
    }

    private void refreshSwitch(JToggleButton b) {
        if (b.isSelected()) {
            b.setBackground(UiKit.PRIMARY);
            b.setForeground(Color.WHITE);
            b.setText("ON");
            b.setBorder(BorderFactory.createCompoundBorder(
                    new LineBorder(UiKit.PRIMARY.darker(), 1, true),
                    new EmptyBorder(3, 8, 3, 8)
            ));
        } else {
            b.setBackground(UiKit.TOGGLE_OFF_BG);
            b.setForeground(UiKit.TOGGLE_OFF_FG);
            b.setText("OFF");
            b.setBorder(BorderFactory.createCompoundBorder(
                    new LineBorder(UiKit.TOGGLE_OFF_BORDER, 1, true),
                    new EmptyBorder(3, 8, 3, 8)
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
        if (label == null) return "ft";
        if (label.contains("Feet")) return "ft";
        if (label.contains("Meters")) return "m";
        return "ft";
    }

    private static String unitCodeToLabel(String code) {
        if (code == null) return "Feet (ft)";
        return switch (code) {
            case "ft" -> "Feet (ft)";
            case "m" -> "Meters (m)";
            default -> "Feet (ft)";
        };
    }

    private static String themeLabelToCode(String label) {
        if (label == null) return "light";
        if ("Dark Blue".equalsIgnoreCase(label.trim())) return "dark_blue";
        return "light";
    }

    private static String themeCodeToLabel(String code) {
        if (code == null) return "Light";
        String v = code.trim().toLowerCase();
        if ("dark_blue".equals(v) || "dark blue".equals(v) || "dark-blue".equals(v) || "dark".equals(v)) {
            return "Dark Blue";
        }
        return "Light";
    }

    private static String safe(String s) {
        return (s == null) ? "" : s.trim();
    }
}
