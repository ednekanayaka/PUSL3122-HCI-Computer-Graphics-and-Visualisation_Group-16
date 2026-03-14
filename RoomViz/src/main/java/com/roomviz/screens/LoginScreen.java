package com.roomviz.screens;

import javax.swing.text.JTextComponent;

import com.roomviz.app.AppFrame;
import com.roomviz.app.Router;
import com.roomviz.data.SettingsRepository;
import com.roomviz.model.UserSettings;
import com.roomviz.ui.UiKit;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.geom.RoundRectangle2D;

public class LoginScreen extends JPanel {

    private final JTextField emailField;
    private final JPasswordField passwordField;
    private final JLabel errorLabel;

    private final SettingsRepository settingsRepo;

    public LoginScreen(AppFrame frame, Router router, SettingsRepository settingsRepo) {
        this.settingsRepo = settingsRepo;

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

        // Make split similar to Figma (left wider than card, right panel fixed-ish)
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, left, right);
        split.setDividerSize(0);
        split.setEnabled(false);
        split.setResizeWeight(0.55); // left takes ~55%
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

        // ===== Center card =====
        JPanel leftCenter = new JPanel(new GridBagLayout());
        leftCenter.setOpaque(false);
        left.add(leftCenter, BorderLayout.CENTER);

        RoundedCard card = new RoundedCard();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(new EmptyBorder(26, 26, 26, 26));
        card.setPreferredSize(new Dimension(360, 420));

        JLabel h1 = new JLabel("Welcome back");
        h1.setAlignmentX(Component.LEFT_ALIGNMENT);
        h1.setForeground(UiKit.TEXT);
        h1.setFont(UiKit.scaled(h1, Font.BOLD, 1.35f));

        JLabel p = new JLabel("<html><div style='width:280px;'>Sign in to manage your designs and start a new visualization</div></html>");
        p.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.setForeground(UiKit.MUTED);
        p.setFont(UiKit.scaled(p, Font.PLAIN, 0.92f));

        card.add(h1);
        card.add(Box.createVerticalStrut(6));
        card.add(p);
        card.add(Box.createVerticalStrut(18));

        // Email
        JLabel emailLbl = label("Email address");
        card.add(emailLbl);
        card.add(Box.createVerticalStrut(6));

        emailField = new JTextField();
        styleTextField(emailField);
        setPlaceholder(emailField, "designer@studio.com");
        card.add(wrapField(emailField));
        card.add(Box.createVerticalStrut(14));

        // Password
        JLabel passLbl = label("Password");
        card.add(passLbl);
        card.add(Box.createVerticalStrut(6));

        passwordField = new JPasswordField();
        styleTextField(passwordField);
        setPlaceholder(passwordField, "Enter your password");
        card.add(wrapField(passwordField));
        card.add(Box.createVerticalStrut(12));

        // Remember + forgot row
        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);

        JCheckBox remember = new JCheckBox("Remember me");
        remember.setOpaque(false);
        remember.setForeground(isHighContrast() ? UiKit.TEXT : new Color(0x374151));
        remember.setFont(UiKit.scaled(remember, Font.PLAIN, 0.92f));

        JButton forgot = linkButton("Forgot password?");
        forgot.addActionListener(e ->
                JOptionPane.showMessageDialog(this, "Forgot password flow can be added later.", "Info", JOptionPane.INFORMATION_MESSAGE)
        );

        row.add(remember, BorderLayout.WEST);
        row.add(forgot, BorderLayout.EAST);

        card.add(row);
        card.add(Box.createVerticalStrut(12));

        // Error label (hidden until needed)
        errorLabel = new JLabel(" ");
        errorLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        errorLabel.setForeground(UiKit.DANGER);
        errorLabel.setFont(UiKit.scaled(errorLabel, Font.PLAIN, 0.92f));
        card.add(errorLabel);
        card.add(Box.createVerticalStrut(8));

        // Login button
        JButton loginBtn = UiKit.primaryGradientButton("Log in");
        loginBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        loginBtn.addActionListener(e -> {
            String email = emailField.getText().trim();
            String pw = new String(passwordField.getPassword()).trim();

            // Basic placeholder checks
            if (email.isEmpty() || email.equals("designer@studio.com")) {
                showError("Please enter your email address.");
                return;
            }
            if (pw.isEmpty() || pw.equals("Enter your password")) {
                showError("Please enter your password.");
                return;
            }

            // ✅ REAL validation against settings.json
            UserSettings s = (settingsRepo == null) ? null : settingsRepo.get();

            String expectedEmail = (s == null) ? "" : safeLower(s.getEmail());
            String expectedPw = (s == null) ? "" : safe(s.getPasswordPlain());

            // Email must match (case-insensitive)
            if (!expectedEmail.isEmpty() && !safeLower(email).equals(expectedEmail)) {
                showError("Email not recognized for this device. Use the saved account email.");
                return;
            }

            // If password is set, enforce it
            if (expectedPw != null && !expectedPw.isEmpty()) {
                if (!pw.equals(expectedPw)) {
                    showError("Incorrect password.");
                    return;
                }
            } else {
                // Demo mode: no password has been set yet
                JOptionPane.showMessageDialog(
                        this,
                        "No password is set yet in Settings.\nLogging in using coursework demo mode.",
                        "Demo Login",
                        JOptionPane.INFORMATION_MESSAGE
                );
            }

            showError(" ");
            frame.goToAppShell();
        });

        // Actions Row (Login + Demo)
        JPanel actionsRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        actionsRow.setOpaque(false);
        actionsRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        actionsRow.add(loginBtn);

        JButton demoBtn = UiKit.ghostButton("Demo Login");
        demoBtn.addActionListener(e -> {
            JOptionPane.showMessageDialog(
                    this,
                    "Logging in using coursework demo mode.",
                    "Demo Login",
                    JOptionPane.INFORMATION_MESSAGE
            );
            frame.goToAppShell();
        });
        actionsRow.add(demoBtn);

        card.add(actionsRow);
        card.add(Box.createVerticalStrut(14));

        // Small helper text
        JLabel small = new JLabel("Your account is for store designers only.");
        small.setAlignmentX(Component.LEFT_ALIGNMENT);
        small.setForeground(UiKit.MUTED);
        small.setFont(UiKit.scaled(small, Font.PLAIN, 0.88f));
        card.add(small);
        card.add(Box.createVerticalStrut(12));

        // Bottom row: Request access + demo
        JPanel bottomRow = new JPanel(new BorderLayout());
        bottomRow.setOpaque(false);
        bottomRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        JButton requestAccess = linkButton("Request access");
        requestAccess.addActionListener(e ->
                JOptionPane.showMessageDialog(this, "Request access flow can be added later.", "Info", JOptionPane.INFORMATION_MESSAGE)
        );

        bottomRow.add(requestAccess, BorderLayout.WEST);

        card.add(bottomRow);

        // Put card in center
        leftCenter.add(card, new GridBagConstraints());

        // ===== Right panel content =====
        JPanel rightContent = new JPanel();
        rightContent.setOpaque(false);
        rightContent.setLayout(new BoxLayout(rightContent, BoxLayout.Y_AXIS));
        rightContent.setBorder(new EmptyBorder(60, 60, 60, 60));

        JLabel icon = new JLabel("\uD83D\uDECB"); // simple placeholder icon
        icon.setFont(UiKit.scaled(icon, Font.PLAIN, 1.65f)); // scales with settings
        icon.setForeground(Color.WHITE);

        JLabel title = new JLabel("<html>Design stunning room<br/>visualizations</html>");
        title.setForeground(Color.WHITE);
        title.setFont(UiKit.scaled(title, Font.BOLD, 1.70f));

        JLabel desc = new JLabel("<html><div style='width:320px;'>Create immersive furniture layouts and bring your interior design visions to life with powerful visualization tools.</div></html>");
        desc.setForeground(new Color(255, 255, 255, 210));
        desc.setFont(UiKit.scaled(desc, Font.PLAIN, 1.00f));

        rightContent.add(icon);
        rightContent.add(Box.createVerticalStrut(14));
        rightContent.add(title);
        rightContent.add(Box.createVerticalStrut(12));
        rightContent.add(desc);
        rightContent.add(Box.createVerticalStrut(20));

        rightContent.add(bullet("Real-time 3D room rendering"));
        rightContent.add(Box.createVerticalStrut(10));
        rightContent.add(bullet("Extensive furniture catalog"));
        rightContent.add(Box.createVerticalStrut(10));
        rightContent.add(bullet("Client collaboration features"));

        right.add(rightContent);
    }

    // ===== helpers =====

    private boolean isHighContrast() {
        return UiKit.TEXT.equals(Color.BLACK) && UiKit.BORDER.equals(Color.BLACK);
    }

    private static String safe(String s) {
        return s == null ? "" : s.trim();
    }

    private static String safeLower(String s) {
        return safe(s).toLowerCase();
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


    private void showError(String msg) {
        errorLabel.setText(msg == null ? " " : msg);
    }

    private Color placeholderColor() {
        return isHighContrast() ? UiKit.TEXT : new Color(0x9CA3AF);
    }

    // ===== Placeholder support (simple) =====
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
        public RoundedCard() {
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int arc = 18;
            Shape r = new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), arc, arc);

            // shadow-ish
            g2.setColor(new Color(0, 0, 0, 18));
            g2.fill(new RoundRectangle2D.Float(3, 4, getWidth() - 6, getHeight() - 6, arc, arc));

            // main
            g2.setColor(UiKit.WHITE);
            g2.fill(r);

            // border
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

            // soft circles like Figma (subtle)
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
