package com.roomviz.screens;

import javax.swing.text.JTextComponent;

import com.roomviz.app.AppFrame;
import com.roomviz.app.Router;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.geom.RoundRectangle2D;

public class LoginScreen extends JPanel {
    // LoginScreen: collects credentials and navigates to the app shell

    private final JTextField emailField;
    private final JPasswordField passwordField;
    private final JLabel errorLabel;

    public LoginScreen(AppFrame frame, Router router) {
        setLayout(new BorderLayout());
        setBackground(new Color(0xF6F7FB));

        // ===== Left + Right split =====
        JPanel left = new JPanel(new BorderLayout());
        left.setBackground(Color.WHITE);

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
        brand.setFont(brand.getFont().deriveFont(Font.BOLD, 16f));
        brandBar.add(brand);

        JLabel subtitle = new JLabel("Interior Design Studio");
        subtitle.setForeground(new Color(0x6B7280));
        subtitle.setFont(subtitle.getFont().deriveFont(12f));
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
        h1.setFont(h1.getFont().deriveFont(Font.BOLD, 22f));

        JLabel p = new JLabel("<html><div style='width:280px;'>Sign in to manage your designs and start a new visualization</div></html>");
        p.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.setForeground(new Color(0x6B7280));
        p.setFont(p.getFont().deriveFont(12f));

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
        remember.setFont(remember.getFont().deriveFont(12f));

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
        errorLabel.setForeground(new Color(0xDC2626));
        errorLabel.setFont(errorLabel.getFont().deriveFont(12f));
        card.add(errorLabel);
        card.add(Box.createVerticalStrut(8));

        // Login button
        JButton loginBtn = new JButton("Log in");
        stylePrimaryButton(loginBtn);
        loginBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        loginBtn.addActionListener(e -> {
            String email = emailField.getText().trim();
            String pw = new String(passwordField.getPassword()).trim();

            if (email.isEmpty() || email.equals("designer@studio.com")) {
                showError("Please enter your email address.");
                return;
            }
            if (pw.isEmpty() || pw.equals("Enter your password")) {
                showError("Please enter your password.");
                return;
            }

            showError(" ");
            frame.goToAppShell(); // ✅ keep your existing navigation
        });

        card.add(loginBtn);
        card.add(Box.createVerticalStrut(14));

        // Small helper text
        JLabel small = new JLabel("Your account is for store designers only.");
        small.setAlignmentX(Component.LEFT_ALIGNMENT);
        small.setForeground(new Color(0x6B7280));
        small.setFont(small.getFont().deriveFont(11f));
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

        JButton demo = new JButton("Continue as demo");
        styleSecondaryButton(demo);
        demo.addActionListener(e -> frame.goToAppShell());

        bottomRow.add(requestAccess, BorderLayout.WEST);
        bottomRow.add(demo, BorderLayout.EAST);

        card.add(bottomRow);

        // Put card in center
        leftCenter.add(card, new GridBagConstraints());

        // ===== Right panel content =====
        JPanel rightContent = new JPanel();
        rightContent.setOpaque(false);
        rightContent.setLayout(new BoxLayout(rightContent, BoxLayout.Y_AXIS));
        rightContent.setBorder(new EmptyBorder(60, 60, 60, 60));

        JLabel icon = new JLabel("\uD83D\uDECB"); // simple placeholder icon
        icon.setFont(icon.getFont().deriveFont(30f));
        icon.setForeground(Color.WHITE);

        JLabel title = new JLabel("<html>Design stunning room<br/>visualizations</html>");
        title.setForeground(Color.WHITE);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 26f));

        JLabel desc = new JLabel("<html><div style='width:320px;'>Create immersive furniture layouts and bring your interior design visions to life with powerful visualization tools.</div></html>");
        desc.setForeground(new Color(255, 255, 255, 210));
        desc.setFont(desc.getFont().deriveFont(13f));

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

    // ===== UI helpers =====

    private JLabel label(String text) {
        JLabel l = new JLabel(text);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        l.setForeground(new Color(0x111827));
        l.setFont(l.getFont().deriveFont(12f));
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
        b.setForeground(new Color(0x6D28D9));
        b.setFont(b.getFont().deriveFont(12f));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    private JPanel bullet(String text) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        p.setOpaque(false);

        JLabel dot = new JLabel("✓");
        dot.setForeground(Color.WHITE);
        dot.setFont(dot.getFont().deriveFont(Font.BOLD, 14f));

        JLabel t = new JLabel(text);
        t.setForeground(new Color(255, 255, 255, 220));
        t.setFont(t.getFont().deriveFont(13f));

        p.add(dot);
        p.add(t);
        return p;
    }

    private void styleTextField(JTextComponent field) {
        field.setFont(field.getFont().deriveFont(13f));
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0xE5E7EB), 1),
                new EmptyBorder(10, 12, 10, 12)
        ));
        field.setBackground(Color.WHITE);
        field.setForeground(new Color(0x111827));
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
    }

    private void stylePrimaryButton(JButton b) {
        b.setBackground(new Color(0x6D28D9));
        b.setForeground(Color.WHITE);
        b.setFocusPainted(false);
        b.setBorder(new EmptyBorder(12, 14, 12, 14));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setFont(b.getFont().deriveFont(Font.BOLD, 13f));
        b.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
    }

    private void styleSecondaryButton(JButton b) {
        b.setBackground(Color.WHITE);
        b.setForeground(new Color(0x6D28D9));
        b.setFocusPainted(false);
        b.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0xD8B4FE), 1),
                new EmptyBorder(10, 14, 10, 14)
        ));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setFont(b.getFont().deriveFont(Font.PLAIN, 12f));
    }

    private void showError(String msg) {
        errorLabel.setText(msg);
    }

    // ===== Placeholder support (simple) =====
    private void setPlaceholder(JTextComponent field, String placeholder) {
        field.setText(placeholder);
        field.setForeground(new Color(0x9CA3AF));

        field.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                String t = field.getText();
                if (t != null && t.equals(placeholder)) {
                    field.setText("");
                    field.setForeground(new Color(0x111827));
                }
            }

            @Override
            public void focusLost(FocusEvent e) {
                String t = field.getText();
                if (t == null || t.trim().isEmpty()) {
                    field.setText(placeholder);
                    field.setForeground(new Color(0x9CA3AF));
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
            g2.setColor(Color.WHITE);
            g2.fill(r);

            // border
            g2.setColor(new Color(0xE5E7EB));
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
