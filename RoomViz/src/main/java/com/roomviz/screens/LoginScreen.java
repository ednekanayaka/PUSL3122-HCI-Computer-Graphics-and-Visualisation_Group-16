package com.roomviz.screens;

import javax.swing.text.JTextComponent;

import com.roomviz.app.AppFrame;
import com.roomviz.app.Router;
import com.roomviz.app.ScreenKeys;
import com.roomviz.data.Session;
import com.roomviz.data.SettingsRepository;
import com.roomviz.data.UserRepository;
import com.roomviz.model.User;
import com.roomviz.model.UserSettings;
import com.roomviz.security.PasswordUtil;
import com.roomviz.ui.FontAwesome;
import com.roomviz.ui.UiKit;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.geom.RoundRectangle2D;
import java.util.Arrays;

public class LoginScreen extends JPanel {

    // Fixed light-mode palette — Login page never changes with dark/light mode
    private static final Color L_BG        = new Color(0xF6F7FB);
    private static final Color L_WHITE     = Color.WHITE;
    private static final Color L_TEXT      = new Color(0x111827);
    private static final Color L_MUTED     = new Color(0x6B7280);
    private static final Color L_BORDER    = new Color(0xE5E7EB);
    private static final Color L_PRIMARY   = new Color(0x5B2BFF);
    private static final Color L_PRIMARY_DARK = new Color(0x8E2DE2);

    private final JTextField emailField;
    private final JPasswordField passwordField;
    private final JLabel errorLabel;
    private final JButton loginBtn;

    private final SettingsRepository settingsRepo;
    private final UserRepository userRepo;
    private final Session session;

    public LoginScreen(AppFrame frame, Router router, SettingsRepository settingsRepo,
                       UserRepository userRepo, Session session) {
        this.settingsRepo = settingsRepo;
        this.userRepo = userRepo;
        this.session = session;

        setLayout(new BorderLayout());
        setBackground(L_BG);

        // ===== Left + Right split =====
        JPanel left = new JPanel(new BorderLayout());
        left.setBackground(L_WHITE);

        JPanel right = new GradientPanel(
                heroGradientStart(),
                heroGradientEnd()
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

        JLabel badge = new JLabel("RV", SwingConstants.CENTER);
        badge.setOpaque(true);
        badge.setBackground(L_PRIMARY);
        badge.setForeground(Color.WHITE);
        badge.setFont(UiKit.scaled(badge, Font.BOLD, 0.92f));
        badge.setBorder(new EmptyBorder(6, 9, 6, 9));

        JLabel brand = new JLabel("RoomViz");
        brand.setForeground(L_TEXT);
        brand.setFont(UiKit.scaled(brand, Font.BOLD, 1.10f));

        JLabel subtitle = new JLabel("Interior Design Studio");
        subtitle.setForeground(L_MUTED);
        subtitle.setFont(UiKit.scaled(subtitle, Font.PLAIN, 0.92f));

        JPanel brandText = new JPanel();
        brandText.setOpaque(false);
        brandText.setLayout(new BoxLayout(brandText, BoxLayout.Y_AXIS));
        brandText.add(brand);
        brandText.add(subtitle);

        brandBar.add(badge);
        brandBar.add(brandText);

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

        card.setMinimumSize(new Dimension(360, 0));
        card.setMaximumSize(new Dimension(420, Integer.MAX_VALUE));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel h1 = new JLabel("Welcome back");
        h1.setAlignmentX(Component.LEFT_ALIGNMENT);
        h1.setForeground(L_TEXT);
        h1.setFont(UiKit.scaled(h1, Font.BOLD, 1.35f));

        JLabel p = new JLabel("<html><div style='width:280px;'>Sign in using the account stored in the RoomViz local database.</div></html>");
        p.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.setForeground(L_MUTED);
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
        remember.setForeground(new Color(0x374151));
        remember.setFont(UiKit.scaled(remember, Font.PLAIN, 0.92f));

        JButton forgot = linkButton("Forgot password?");
        forgot.addActionListener(e ->
                JOptionPane.showMessageDialog(this, "Forgot password flow can be added later.", "Info", JOptionPane.INFORMATION_MESSAGE)
        );

        row.add(remember, BorderLayout.WEST);
        row.add(forgot, BorderLayout.EAST);

        card.add(row);
        card.add(Box.createVerticalStrut(12));

        // Error label
        errorLabel = new JLabel(" ");
        errorLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        errorLabel.setForeground(UiKit.DANGER);
        errorLabel.setFont(UiKit.scaled(errorLabel, Font.PLAIN, 0.92f));
        card.add(errorLabel);
        card.add(Box.createVerticalStrut(8));

        // Login button
        loginBtn = UiKit.primaryButton("Log in");
        stylePrimaryButton(loginBtn);
        loginBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        loginBtn.addActionListener(e -> handleLogin(frame));

        card.add(loginBtn);
        card.add(Box.createVerticalStrut(14));

        JLabel small = new JLabel("Your account is for store designers only.");
        small.setAlignmentX(Component.LEFT_ALIGNMENT);
        small.setForeground(L_MUTED);
        small.setFont(UiKit.scaled(small, Font.PLAIN, 0.88f));
        card.add(small);
        card.add(Box.createVerticalStrut(10));

        JPanel registerRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        registerRow.setOpaque(false);
        registerRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel noAcc = new JLabel("Don't have an account? ");
        noAcc.setForeground(L_MUTED);
        noAcc.setFont(UiKit.scaled(noAcc, Font.PLAIN, 0.90f));

        JButton goRegister = linkButton("Register");
        goRegister.addActionListener(e -> router.show(ScreenKeys.REGISTER));

        registerRow.add(noAcc);
        registerRow.add(goRegister);

        card.add(registerRow);
        card.add(Box.createVerticalStrut(10));

        JLabel secure = new JLabel("Secure local authentication enabled");
        secure.setAlignmentX(Component.LEFT_ALIGNMENT);
        secure.setForeground(L_MUTED);
        secure.setFont(UiKit.scaled(secure, Font.PLAIN, 0.86f));
        card.add(secure);

        // Put card center-aligned, allow scroll naturally
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.insets = new Insets(24, 24, 24, 24);
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;

        leftCenter.add(card, gbc);

        // ===== Right panel content =====
        JPanel rightContent = new JPanel();
        rightContent.setOpaque(false);
        rightContent.setLayout(new BoxLayout(rightContent, BoxLayout.Y_AXIS));
        rightContent.setBorder(new EmptyBorder(60, 60, 60, 60));

        JLabel icon = new JLabel(FontAwesome.COUCH);
        icon.setFont(FontAwesome.solid(36f));
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

        // When returning to this screen (after delete/logout),
        // force pending=false so we never stay stuck in WAIT cursor.
        addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentShown(java.awt.event.ComponentEvent e) {
                setLoginPending(false);
                showError(" ");
            }
        });
    }

    // ===== helpers =====

    private boolean isHighContrast() {
        return false; // Login page always uses light mode style
    }

    private Color heroGradientStart() {
        return new Color(0x5B2BFF);
    }

    private Color heroGradientEnd() {
        return new Color(0x8E2DE2);
    }

    private JLabel label(String text) {
        JLabel l = new JLabel(text);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        l.setForeground(L_TEXT);
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
        b.setForeground(L_PRIMARY_DARK);
        b.setFont(UiKit.scaled(b, Font.PLAIN, 0.92f));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    private JPanel bullet(String text) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        p.setOpaque(false);

        JLabel dot = new JLabel(FontAwesome.CHECK);
        dot.setForeground(Color.WHITE);
        dot.setFont(FontAwesome.solid(13f));

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
                BorderFactory.createLineBorder(L_BORDER, 1, true),
                new EmptyBorder(10, 12, 10, 12)
        ));
        field.setBackground(L_WHITE);
        field.setForeground(L_TEXT);
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        field.setCaretColor(L_TEXT);
    }

    private void stylePrimaryButton(JButton b) {
        b.setBackground(L_PRIMARY);
        if (b instanceof UiKit.RoundButton rb && !isHighContrast()) {
            rb.setGradient(L_PRIMARY, L_PRIMARY_DARK);
        }
        b.setForeground(Color.WHITE);
        b.setFocusPainted(false);
        b.setBorder(new EmptyBorder(12, 14, 12, 14));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setFont(UiKit.scaled(b, Font.BOLD, 1.00f));
        b.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
    }

    private void handleLogin(AppFrame frame) {
        if (!loginBtn.isEnabled()) return;

        String email = emailField.getText().trim();
        char[] pwChars = passwordField.getPassword();

        if (email.isEmpty() || email.equals("designer@studio.com")) {
            Arrays.fill(pwChars, '\0');
            showError("Please enter your email address.");
            return;
        }
        String pw = new String(pwChars).trim();
        if (pw.isEmpty() || pw.equals("Enter your password")) {
            Arrays.fill(pwChars, '\0');
            showError("Please enter your password.");
            return;
        }

        String emailLower = email.toLowerCase().trim();
        char[] verifyChars = Arrays.copyOf(pwChars, pwChars.length);
        Arrays.fill(pwChars, '\0');

        setLoginPending(true);
        showInfo("Signing in... Please wait.");

        SwingWorker<LoginResult, Void> worker = new SwingWorker<>() {
            @Override
            protected LoginResult doInBackground() {
                try {
                    User u = userRepo.findByEmail(emailLower);
                    if (u == null) {
                        return LoginResult.error("No account found for this email. Please register first.");
                    }

                    boolean ok = PasswordUtil.verify(verifyChars, u.getPasswordHash());
                    if (!ok) return LoginResult.error("Incorrect password.");

                    return LoginResult.success(u);
                } catch (Exception ex) {
                    return LoginResult.failure("Login failed. Please try again.", ex);
                } finally {
                    Arrays.fill(verifyChars, '\0');
                }
            }

            @Override
            protected void done() {
                LoginResult result;
                try {
                    result = get();
                } catch (Exception ex) {
                    result = LoginResult.failure("Login failed. Please try again.", ex);
                }

                if (result.exception != null) result.exception.printStackTrace();

                if (!result.ok) {
                    setLoginPending(false);
                    showError(result.message);
                    return;
                }

                try {
                    User u = result.user;
                    session.login(u);

                    if (settingsRepo != null) {
                        UserSettings s = settingsRepo.get();
                        if (s == null) s = UserSettings.defaults();
                        s.setFullName(u.getFullName());
                        s.setEmail(u.getEmail());
                        s.setPasswordPlain("");
                        settingsRepo.save(s);
                    }

                    showInfo("Login successful. Opening workspace...");
                    Timer t = new Timer(300, ev -> {
                        ((Timer) ev.getSource()).stop();
                        frame.goToAppShell();
                    });
                    t.setRepeats(false);
                    t.start();
                } catch (Exception ex) {
                    ex.printStackTrace();
                    setLoginPending(false);
                    showError("Login failed. Please try again.");
                }
            }
        };

        worker.execute();
    }

    private void setLoginPending(boolean pending) {
        loginBtn.setEnabled(!pending);
        loginBtn.setText(pending ? "Logging in..." : "Log in");
        loginBtn.setCursor(Cursor.getPredefinedCursor(pending ? Cursor.WAIT_CURSOR : Cursor.HAND_CURSOR));
        emailField.setEnabled(!pending);
        passwordField.setEnabled(!pending);
        setCursor(Cursor.getPredefinedCursor(pending ? Cursor.WAIT_CURSOR : Cursor.DEFAULT_CURSOR));
    }

    private void showInfo(String msg) {
        errorLabel.setForeground(L_PRIMARY);
        errorLabel.setText(msg == null || msg.trim().isEmpty() ? " " : msg);
    }

    private void showError(String msg) {
        errorLabel.setForeground(new Color(0xEF4444));
        errorLabel.setText(msg == null || msg.trim().isEmpty() ? " " : msg);
    }

    private static class LoginResult {
        final boolean ok;
        final User user;
        final String message;
        final Exception exception;

        private LoginResult(boolean ok, User user, String message, Exception exception) {
            this.ok = ok;
            this.user = user;
            this.message = message;
            this.exception = exception;
        }

        static LoginResult success(User user) {
            return new LoginResult(true, user, null, null);
        }

        static LoginResult error(String message) {
            return new LoginResult(false, null, message, null);
        }

        static LoginResult failure(String message, Exception exception) {
            return new LoginResult(false, null, message, exception);
        }
    }

    private Color placeholderColor() {
        return new Color(0x9CA3AF);
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
                    field.setForeground(L_TEXT);
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

            g2.setColor(L_WHITE);
            g2.fill(r);

            g2.setColor(L_BORDER);
            g2.draw(r);

            g2.dispose();
            super.paintComponent(g);
        }
    }

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
