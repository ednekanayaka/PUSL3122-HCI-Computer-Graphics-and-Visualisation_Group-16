package com.roomviz.screens;

import com.roomviz.app.AppFrame;
import com.roomviz.data.Session;
import com.roomviz.model.User;
import com.roomviz.ui.UiKit;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class TopBar extends JPanel {

    private final AppFrame frame;
    private final Session session;

    private final JLabel title = new JLabel("Dashboard", SwingConstants.CENTER);

    // user pill
    private final JLabel nameLabel = new JLabel("User");
    private final JLabel emailLabel = new JLabel("user@email.com");

    public TopBar(AppFrame frame, Session session) {
        this.frame = frame;
        this.session = session;

        setLayout(new BorderLayout());
        setOpaque(true);
        setBackground(UiKit.WHITE);
        setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, UiKit.BORDER));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 10));
        left.setOpaque(false);
        left.setBorder(new EmptyBorder(0, 10, 0, 10));

        title.setForeground(UiKit.TEXT);
        title.setFont(UiKit.scaled(title, Font.BOLD, 1.10f));
        title.setBorder(new EmptyBorder(12, 0, 12, 0));

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 8));
        right.setOpaque(false);
        right.setBorder(new EmptyBorder(0, 10, 0, 12));

        JPanel userPill = buildUserPill();
        JButton logoutBtn = buildLogoutBtn();

        right.add(userPill);
        right.add(logoutBtn);

        add(left, BorderLayout.WEST);
        add(title, BorderLayout.CENTER);
        add(right, BorderLayout.EAST);

        refreshUserFromSession();
    }

    private JPanel buildUserPill() {
        JPanel pill = new JPanel();
        pill.setOpaque(true);
        pill.setBackground(UiKit.WHITE);
        pill.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UiKit.BORDER, 1, true),
                new EmptyBorder(8, 10, 8, 10)
        ));
        pill.setLayout(new BoxLayout(pill, BoxLayout.X_AXIS));

        JLabel avatar = new JLabel("\u25CF");
        avatar.setForeground(isHighContrast() ? UiKit.TEXT : new Color(0x9CA3AF));
        avatar.setFont(UiKit.scaled(avatar, Font.BOLD, 1.10f));

        JPanel textCol = new JPanel();
        textCol.setOpaque(false);
        textCol.setLayout(new BoxLayout(textCol, BoxLayout.Y_AXIS));

        nameLabel.setForeground(UiKit.TEXT);
        nameLabel.setFont(UiKit.scaled(nameLabel, Font.BOLD, 0.98f));

        emailLabel.setForeground(UiKit.MUTED);
        emailLabel.setFont(UiKit.scaled(emailLabel, Font.PLAIN, 0.88f));

        textCol.add(nameLabel);
        textCol.add(emailLabel);

        pill.add(avatar);
        pill.add(Box.createHorizontalStrut(10));
        pill.add(textCol);

        return pill;
    }

    private JButton buildLogoutBtn() {
        JButton b = new JButton("Logout");
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setFont(UiKit.scaled(b, Font.PLAIN, 0.98f));
        b.setForeground(UiKit.DANGER);
        b.setBackground(UiKit.WHITE);
        b.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UiKit.BORDER, 1, true),
                new EmptyBorder(9, 12, 9, 12)
        ));

        b.addActionListener(e -> {
            if (session != null) session.logout();
            frame.goToLogin();
        });

        b.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseEntered(java.awt.event.MouseEvent e) {
                b.setBackground(isHighContrast() ? UiKit.WHITE : new Color(0xFEF2F2));
            }
            @Override public void mouseExited(java.awt.event.MouseEvent e) {
                b.setBackground(UiKit.WHITE);
            }
        });

        return b;
    }

    private boolean isHighContrast() {
        return UiKit.TEXT.equals(Color.BLACK) && UiKit.BORDER.equals(Color.BLACK);
    }

    public void setTitle(String t) {
        title.setText(t == null ? "" : t);
    }

    public void refreshUserFromSession() {
        User u = (session == null) ? null : session.getCurrentUser();

        String nm = (u == null) ? "User" : safe(u.getFullName(), "User");
        String em = (u == null) ? "user@email.com" : safe(u.getEmail(), "user@email.com");

        nameLabel.setText(nm);
        emailLabel.setText(em);
    }

    private String safe(String v, String fallback) {
        if (v == null) return fallback;
        String t = v.trim();
        return t.isEmpty() ? fallback : t;
    }
}