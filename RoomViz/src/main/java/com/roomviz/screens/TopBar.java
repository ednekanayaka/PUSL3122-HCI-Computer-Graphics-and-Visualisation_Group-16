package com.roomviz.screens;

import com.roomviz.app.AppFrame;
import com.roomviz.ui.UiKit;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class TopBar extends JPanel {

    private final JLabel titleLabel;

    public TopBar(AppFrame frame) {
        setLayout(new BorderLayout());
        setBackground(Color.WHITE);
        setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, UiKit.BORDER));
        setPreferredSize(new Dimension(0, 56));

        // Left: page title (Figma-like)
        titleLabel = new JLabel("Dashboard");
        titleLabel.setForeground(UiKit.TEXT);
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 14.5f));
        titleLabel.setBorder(new EmptyBorder(0, 16, 0, 0));
        add(titleLabel, BorderLayout.WEST);

        // Right: icons + user
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        right.setOpaque(false);

        JButton bell = UiKit.iconButton("🔔");
        bell.setToolTipText("Notifications");

        JButton gear = UiKit.iconButton("⚙");
        gear.setToolTipText("Quick settings");

        // user block
        JPanel user = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        user.setOpaque(false);

        UiKit.RoundedPanel avatar = new UiKit.RoundedPanel(999, new Color(0xE5E7EB));
        avatar.setPreferredSize(new Dimension(28, 28));
        avatar.setLayout(new GridBagLayout());
        JLabel a = new JLabel("S");
        a.setForeground(new Color(0x374151));
        a.setFont(a.getFont().deriveFont(Font.BOLD, 12f));
        avatar.add(a);

        JPanel nameRole = new JPanel();
        nameRole.setOpaque(false);
        nameRole.setLayout(new BoxLayout(nameRole, BoxLayout.Y_AXIS));

        JLabel name = new JLabel("Sarah Mitchell");
        name.setForeground(UiKit.TEXT);
        name.setFont(name.getFont().deriveFont(Font.PLAIN, 12.4f));

        JLabel role = new JLabel("Senior Designer");
        role.setForeground(UiKit.MUTED);
        role.setFont(role.getFont().deriveFont(Font.PLAIN, 11.0f));

        nameRole.add(name);
        nameRole.add(role);

        JButton caret = UiKit.iconButton("▾");
        caret.setToolTipText("Account menu");

        JButton logout = new JButton("Logout");
        logout.setForeground(UiKit.DANGER);
        logout.setBackground(Color.WHITE);
        logout.setFocusPainted(false);
        logout.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        logout.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
        logout.addActionListener(e -> frame.goToLogin());

        user.add(avatar);
        user.add(nameRole);
        user.add(caret);

        right.add(bell);
        right.add(gear);
        right.add(user);
        right.add(logout);

        add(right, BorderLayout.EAST);
    }

    public void setTitle(String title) {
        titleLabel.setText(title);
    }
}
