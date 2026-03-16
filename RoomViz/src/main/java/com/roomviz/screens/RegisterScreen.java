package com.roomviz.screens;

import com.roomviz.app.AppFrame;
import com.roomviz.app.Router;
import com.roomviz.app.ScreenKeys;
import com.roomviz.data.Session;
import com.roomviz.data.SettingsRepository;
import com.roomviz.data.UserRepository;
import com.roomviz.model.User;
import com.roomviz.model.UserSettings;
import com.roomviz.ui.UiKit;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.geom.RoundRectangle2D;

public class RegisterScreen extends JPanel {

    private final JTextField nameField;
    private final JTextField emailField;
    private final JPasswordField passwordField;
    private final JPasswordField confirmPasswordField;
    private final JLabel errorLabel;

    private final SettingsRepository settingsRepo;
    private final UserRepository userRepo;
    private final Session session;

    public RegisterScreen(AppFrame frame, Router router, SettingsRepository settingsRepo,
                          UserRepository userRepo, Session session) {
        this.settingsRepo = settingsRepo;
        this.userRepo = userRepo;
        this.session = session;

        setLayout(new BorderLayout());
        setBackground(UiKit.BG);

        // ===== Left + Right split =====
        JPanel left = new JPanel(new BorderLayout());
        left.setBackground(UiKit.WHITE);

        JPanel right = new GradientPanel(
                new Color(0x5B2BFF),
                new Color(0x8E2DE2)
        );
        right.setLayout(new GridBagLayout());

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, left, right);
        split.setDividerSize(0);
        split.setEnabled(false);
        split.setResizeWeight(0.55);
        add(split, BorderLayout.CENTER);

        // ===== Top-left brand =====
        JPanel brandBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 18, 18));
        brandBar.setOpaque(false);

        JLabel brand = new JLabel("RoomViz");
        brand.setForeground(UiKit.TEXT);
        brand.setFont(UiKit.scaled(brand, Font.BOLD, 1.10f));
        brandBar.add(brand);

        JLabel subtitle = new JLabel("Interior Design Studio");
        subtitle.setForeground(UiKit.MUTED);
        subtitle.setFont(UiKit.scaled(subtitle, Font.PLAIN, 0.92f));
        brandBar.add(subtitle);

        left.add(brandBar, BorderLayout.NORTH);

        // ===== Center card (SCROLL FIX - REAL) =====
        JPanel leftCenter = new JPanel(new GridBagLayout());
        leftCenter.setOpaque(false);

        JScrollPane leftScroll = new JScrollPane(leftCenter,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        leftScroll.setBorder(null);
        leftScroll.getViewport().setOpaque(false);
        leftScroll.setOpaque(false);
        leftScroll.getVerticalScrollBar().setUnitIncrement(14);

        left.add(leftScroll, BorderLayout.CENTER);

        RoundedCard card = new RoundedCard();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(new EmptyBorder(26, 26, 26, 26));

        // IMPORTANT: DO NOT force a fixed preferred height (it causes clipping)
        card.setMinimumSize(new Dimension(360, 0));
        card.setMaximumSize(new Dimension(420, Integer.MAX_VALUE));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel h1 = new JLabel("Create your account");
        h1.setAlignmentX(Component.LEFT_ALIGNMENT);
        h1.setForeground(UiKit.TEXT);
        h1.setFont(UiKit.scaled(h1, Font.BOLD, 1.35f));

        JLabel p = new JLabel("<html><div style='width:280px;'>Register once and your account is saved in the local RoomViz database.</div></html>");
        p.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.setForeground(UiKit.MUTED);
        p.setFont(UiKit.scaled(p, Font.PLAIN, 0.92f));

        card.add(h1);
        card.add(Box.createVerticalStrut(6));
        card.add(p);
        card.add(Box.createVerticalStrut(18));

        // Full name
        JLabel nameLbl = label("Full name");
        card.add(nameLbl);
        card.add(Box.createVerticalStrut(6));

        nameField = new JTextField();
        styleTextField(nameField);
        setPlaceholder(nameField, "Sarah Johnson");
        card.add(wrapField(nameField));
        card.add(Box.createVerticalStrut(14));

        // Email
        JLabel emailLbl = label("Email address");
        card.add(emailLbl);
        card.add(Box.createVerticalStrut(6));

        emailField = new JTextField();
        styleTextField(emailField);
        setPlaceholder(emailField, "sarah.johnson@designstudio.com");
        card.add(wrapField(emailField));
        card.add(Box.createVerticalStrut(14));

        // Password
        JLabel passLbl = label("Password");
        card.add(passLbl);
        card.add(Box.createVerticalStrut(6));

        passwordField = new JPasswordField();
        styleTextField(passwordField);
        setPlaceholder(passwordField, "Create a password");
        card.add(wrapField(passwordField));
        card.add(Box.createVerticalStrut(14));

        // Confirm
        JLabel confirmLbl = label("Confirm password");
        card.add(confirmLbl);
        card.add(Box.createVerticalStrut(6));

        confirmPasswordField = new JPasswordField();
        styleTextField(confirmPasswordField);
        setPlaceholder(confirmPasswordField, "Re-enter password");
        card.add(wrapField(confirmPasswordField));
        card.add(Box.createVerticalStrut(12));

        // Error label
        errorLabel = new JLabel(" ");
        errorLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        errorLabel.setForeground(UiKit.DANGER);
        errorLabel.setFont(UiKit.scaled(errorLabel, Font.PLAIN, 0.92f));
        card.add(errorLabel);
        card.add(Box.createVerticalStrut(8));

        // Register button
        JButton registerBtn = new JButton("Create account");
        stylePrimaryButton(registerBtn);
        registerBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        registerBtn.addActionListener(e -> {
            String name = nameField.getText().trim();
            String email = emailField.getText().trim();
            String pw = new String(passwordField.getPassword()).trim();
            String cpw = new String(confirmPasswordField.getPassword()).trim();

            if (name.isEmpty() || name.equals("Sarah Johnson")) {
                showError("Please enter your full name.");
                return;
            }
            if (email.isEmpty() || email.equals("sarah.johnson@designstudio.com") || !email.contains("@")) {
                showError("Please enter a valid email address.");
                return;
            }
            if (pw.isEmpty() || pw.equals("Create a password")) {
                showError("Please create a password.");
                return;
            }
            if (pw.length() < 4) {
                showError("Password is too short (min 4 characters).");
                return;
            }
            if (!pw.equals(cpw)) {
                showError("Passwords do not match.");
                return;
            }

            try {
                String emailLower = email.toLowerCase().trim();

                // ✅ Check if email already exists
                if (userRepo.findByEmail(emailLower) != null) {
                    showError("An account with this email already exists.");
                    return;
                }

                // ✅ Create real DB user (password is hashed in repo)
                User u = userRepo.createUser(name, emailLower, pw.toCharArray());

                // ✅ Login session immediately
                session.login(u);

                // ✅ Save only display info to settings (NOT password)
                UserSettings s = (settingsRepo == null) ? null : settingsRepo.get();
                if (s == null) s = UserSettings.defaults();
                s.setFullName(u.getFullName());
                s.setEmail(u.getEmail());
                s.setPasswordPlain(""); // stop using plain password
                settingsRepo.save(s);

                showError(" ");
                JOptionPane.showMessageDialog(
                        this,
                        "Account created successfully.\nYou are now logged in.",
                        "Registered",
                        JOptionPane.INFORMATION_MESSAGE
                );

                frame.goToAppShell();

            } catch (Exception ex) {
                ex.printStackTrace();
                showError("Registration failed. Please try again.");
            }
        });

        card.add(registerBtn);
        card.add(Box.createVerticalStrut(14));

        // Back to login row
        JPanel bottomRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        bottomRow.setOpaque(false);
        bottomRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel small = new JLabel("Already have an account? ");
        small.setForeground(UiKit.MUTED);
        small.setFont(UiKit.scaled(small, Font.PLAIN, 0.90f));

        JButton backToLogin = linkButton("Log in");
        backToLogin.addActionListener(e -> router.show(ScreenKeys.LOGIN));

        bottomRow.add(small);
        bottomRow.add(backToLogin);
        card.add(bottomRow);

        // Put card top-aligned, allow scroll naturally
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.insets = new Insets(24, 24, 24, 24);
        gbc.anchor = GridBagConstraints.NORTH;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;

        leftCenter.add(card, gbc);

        // ===== Right panel content =====
        JPanel rightContent = new JPanel();
        rightContent.setOpaque(false);
        rightContent.setLayout(new BoxLayout(rightContent, BoxLayout.Y_AXIS));
        rightContent.setBorder(new EmptyBorder(60, 60, 60, 60));

        JLabel icon = new JLabel("\uD83D\uDD8C");
        icon.setFont(UiKit.scaled(icon, Font.PLAIN, 1.65f));
        icon.setForeground(Color.WHITE);

        JLabel title = new JLabel("<html>Start building your<br/>room designs</html>");
        title.setForeground(Color.WHITE);
        title.setFont(UiKit.scaled(title, Font.BOLD, 1.70f));

        JLabel desc = new JLabel("<html><div style='width:320px;'>Register to save your account locally, then explore the dashboard, 2D planner, and 3D view.</div></html>");
        desc.setForeground(new Color(255, 255, 255, 210));
        desc.setFont(UiKit.scaled(desc, Font.PLAIN, 1.00f));

        rightContent.add(icon);
        rightContent.add(Box.createVerticalStrut(14));
        rightContent.add(title);
        rightContent.add(Box.createVerticalStrut(12));
        rightContent.add(desc);
        rightContent.add(Box.createVerticalStrut(20));

        rightContent.add(bullet("Local SQLite account"));
        rightContent.add(Box.createVerticalStrut(10));
        rightContent.add(bullet("Instant access to dashboard"));
        rightContent.add(Box.createVerticalStrut(10));
        rightContent.add(bullet("2D + 3D visualization tools"));

        right.add(rightContent);
    }

    // ===== helpers =====

    private boolean isHighContrast() {
        return UiKit.TEXT.equals(Color.BLACK) && UiKit.BORDER.equals(Color.BLACK);
    }

    private JLabel label(String text) {
        JLabel l = new JLabel(text);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        l.setForeground(UiKit.TEXT);
        l.setFont(UiKit.scaled(l, Font.PLAIN, 0.92f));
        return l;
    }

    private JPanel wrapField(JComponent field) {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        p.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(field, BorderLayout.CENTER);
        return p;
    }

    private JButton linkButton(String text) {
        JButton b = new JButton(text);
        b.setBorderPainted(false);
        b.setContentAreaFilled(false);
        b.setFocusPainted(false);
        b.setForeground(isHighContrast() ? UiKit.TEXT : new Color(0x6D28D9));
        b.setFont(UiKit.scaled(b, Font.PLAIN, 0.92f));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    private JPanel bullet(String text) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        p.setOpaque(false);

        JLabel dot = new JLabel("✓");
        dot.setForeground(Color.WHITE);
        dot.setFont(UiKit.scaled(dot, Font.BOLD, 1.05f));

        JLabel t = new JLabel(text);
        t.setForeground(new Color(255, 255, 255, 220));
        t.setFont(UiKit.scaled(t, Font.PLAIN, 1.00f));

        p.add(dot);
        p.add(t);
        return p;
    }

    private void styleTextField(JTextComponent field) {
        field.setFont(UiKit.scaled((JComponent) field, Font.PLAIN, 1.00f));
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UiKit.BORDER, 1, true),
                new EmptyBorder(10, 12, 10, 12)
        ));
        field.setBackground(UiKit.WHITE);
        field.setForeground(UiKit.TEXT);
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        field.setCaretColor(UiKit.TEXT);
    }

    private void stylePrimaryButton(JButton b) {
        b.setBackground(isHighContrast() ? UiKit.TEXT : new Color(0x6D28D9));
        b.setForeground(Color.WHITE);
        b.setFocusPainted(false);
        b.setBorder(new EmptyBorder(12, 14, 12, 14));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setFont(UiKit.scaled(b, Font.BOLD, 1.00f));
        b.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
    }

    private void showError(String msg) {
        errorLabel.setText(msg == null ? " " : msg);
    }

    private Color placeholderColor() {
        return isHighContrast() ? UiKit.TEXT : new Color(0x9CA3AF);
    }

    private void setPlaceholder(JTextComponent field, String placeholder) {
        field.setText(placeholder);
        field.setForeground(placeholderColor());

        field.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                String t = field.getText();
                if (t != null && t.equals(placeholder)) {
                    field.setText("");
                    field.setForeground(UiKit.TEXT);
                }
            }

            @Override
            public void focusLost(FocusEvent e) {
                String t = field.getText();
                if (t == null || t.trim().isEmpty()) {
                    field.setText(placeholder);
                    field.setForeground(placeholderColor());
                }
            }
        });
    }

    // ===== Custom rounded card =====
    static class RoundedCard extends JPanel {
        public RoundedCard() { setOpaque(false); }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int arc = 18;
            Shape r = new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), arc, arc);

            g2.setColor(new Color(0, 0, 0, 18));
            g2.fill(new RoundRectangle2D.Float(3, 4, getWidth() - 6, getHeight() - 6, arc, arc));

            g2.setColor(UiKit.WHITE);
            g2.fill(r);

            g2.setColor(UiKit.BORDER);
            g2.draw(r);

            g2.dispose();
            super.paintComponent(g);
        }
    }

    // ===== Gradient panel (right side) =====
    static class GradientPanel extends JPanel {
        private final Color a;
        private final Color b;

        public GradientPanel(Color a, Color b) {
            this.a = a;
            this.b = b;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            GradientPaint gp = new GradientPaint(0, 0, a, getWidth(), getHeight(), b);
            g2.setPaint(gp);
            g2.fillRect(0, 0, getWidth(), getHeight());

            g2.setColor(new Color(255, 255, 255, 28));
            int s1 = Math.min(getWidth(), getHeight()) / 2;
            g2.fillOval(getWidth() / 2 - s1 / 2, 80, s1, s1);

            g2.setColor(new Color(255, 255, 255, 18));
            int s2 = Math.min(getWidth(), getHeight()) / 2;
            g2.fillOval(getWidth() / 2 - s2 / 2, getHeight() - s2 - 80, s2, s2);

            g2.dispose();
        }
    }
}