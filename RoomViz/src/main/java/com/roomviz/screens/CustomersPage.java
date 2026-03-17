package com.roomviz.screens;

import com.roomviz.app.AppFrame;
import com.roomviz.app.Router;
import com.roomviz.app.ScreenKeys;
import com.roomviz.data.AppState;
import com.roomviz.data.Session;
import com.roomviz.data.UserRepository;
import com.roomviz.model.User;
import com.roomviz.ui.FontAwesome;
import com.roomviz.ui.UiKit;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CustomersPage extends JPanel {

    private final Router router;
    private final AppState appState; // optional usage (design count)
    private final UserRepository userRepo;
    private final Session session;

    private JTextField searchField;
    private JButton addBtn;

    private JPanel listWrap;      // container for rows
    private JPanel listColumn;    // vertical box list
    private JLabel metaLabel;     // "Showing X customers"
    private JScrollPane scroll;

    private List<User> allCustomers = new ArrayList<>();

    public CustomersPage(AppFrame frame,
                         Router router,
                         AppState appState,
                         UserRepository userRepo,
                         Session session) {

        this.router = router;
        this.appState = appState;
        this.userRepo = userRepo;
        this.session = session;

        setLayout(new BorderLayout());
        setOpaque(false);

        if (!isAdmin()) {
            add(buildNotAllowedCard("Customers management is admin-only."), BorderLayout.CENTER);
            return;
        }

        add(buildTopBar(), BorderLayout.NORTH);
        add(buildList(), BorderLayout.CENTER);

        reload();
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

    // --- Admin guard ---

    private boolean isAdmin() {
        if (session == null || !session.isLoggedIn()) return false;
        User me = session.getCurrentUser();
        return me != null && me.isAdmin();
    }

    // --- UI ---

    private JComponent buildTopBar() {
        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setOpaque(false);
        wrap.setBorder(new EmptyBorder(16, 18, 10, 18));

        // Left: Title + meta
        JPanel left = new JPanel();
        left.setOpaque(false);
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("Customers");
        title.setForeground(UiKit.TEXT);
        title.setFont(UiKit.scaled(title, Font.BOLD, 1.25f));

        metaLabel = new JLabel(" ");
        metaLabel.setForeground(UiKit.MUTED);
        metaLabel.setFont(UiKit.scaled(metaLabel, Font.PLAIN, 0.92f));
        metaLabel.setBorder(new EmptyBorder(4, 0, 0, 0));

        left.add(title);
        left.add(metaLabel);

        // Right: Search + Add button
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        right.setOpaque(false);

        searchField = new JTextField(18);
        searchField.setFont(UiKit.scaled(searchField, Font.PLAIN, 0.95f));
        searchField.setForeground(UiKit.TEXT);
        searchField.setBackground(UiKit.WHITE);
        searchField.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(UiKit.BORDER, 1, true),
                new EmptyBorder(9, 10, 9, 10)
        ));
        searchField.setToolTipText("Search by name, email, job or department");

        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { applyFilter(); }
            @Override public void removeUpdate(DocumentEvent e) { applyFilter(); }
            @Override public void changedUpdate(DocumentEvent e) { applyFilter(); }
        });

        addBtn = UiKit.primaryButton("+ Add Customer");
        addBtn.addActionListener(e -> openAddDialog());

        right.add(searchField);
        right.add(addBtn);

        wrap.add(left, BorderLayout.WEST);
        wrap.add(right, BorderLayout.EAST);
        return wrap;
    }

    private JComponent buildList() {
        listWrap = new JPanel(new BorderLayout());
        listWrap.setOpaque(false);
        listWrap.setBorder(new EmptyBorder(0, 18, 18, 18));

        listColumn = new JPanel();
        listColumn.setOpaque(false);
        listColumn.setLayout(new BoxLayout(listColumn, BoxLayout.Y_AXIS));

        scroll = new JScrollPane(listColumn);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setOpaque(false);
        scroll.setOpaque(false);

        // smoother scroll
        scroll.getVerticalScrollBar().setUnitIncrement(16);

        listWrap.add(scroll, BorderLayout.CENTER);
        return listWrap;
    }

    private JComponent buildEmptyState(String msg) {
        UiKit.RoundedPanel card = new UiKit.RoundedPanel(18, UiKit.WHITE);
        card.setBorderPaint(UiKit.BORDER);
        card.setLayout(new GridBagLayout());
        card.setBorder(new EmptyBorder(48, 28, 48, 28));
        card.setAlignmentX(0.0f);
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 300));

        JPanel inner = new JPanel();
        inner.setOpaque(false);
        inner.setLayout(new BoxLayout(inner, BoxLayout.Y_AXIS));

        // Icon
        JLabel icon = new JLabel(FontAwesome.USERS);
        icon.setFont(FontAwesome.solid(36f));
        icon.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Title
        JLabel title = new JLabel("No customers found");
        title.setForeground(UiKit.TEXT);
        title.setFont(UiKit.scaled(title, Font.BOLD, 1.15f));
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Subtitle
        JLabel sub = new JLabel("<html><div style='text-align:center;'>" + msg + "</div></html>");
        sub.setForeground(UiKit.MUTED);
        sub.setFont(UiKit.scaled(sub, Font.PLAIN, 0.95f));
        sub.setBorder(new EmptyBorder(6, 0, 0, 0));
        sub.setAlignmentX(Component.CENTER_ALIGNMENT);
        sub.setHorizontalAlignment(SwingConstants.CENTER);

        // Button
        JButton add = UiKit.primaryButton("+ Add Customer");
        add.setAlignmentX(Component.CENTER_ALIGNMENT);
        add.addActionListener(e -> openAddDialog());

        inner.add(icon);
        inner.add(Box.createVerticalStrut(12));
        inner.add(title);
        inner.add(sub);
        inner.add(Box.createVerticalStrut(18));
        inner.add(add);

        card.add(inner);
        return card;
    }

    private JComponent buildNotAllowedCard(String message) {
        JPanel wrap = new JPanel(new GridBagLayout());
        wrap.setOpaque(false);

        UiKit.RoundedPanel card = new UiKit.RoundedPanel(18, UiKit.WHITE);
        card.setBorderPaint(UiKit.BORDER);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(new EmptyBorder(28, 28, 28, 28));

        JLabel title = new JLabel("Access restricted");
        title.setForeground(UiKit.TEXT);
        title.setFont(UiKit.scaled(title, Font.BOLD, 1.10f));
        title.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel sub = new JLabel("<html>" + message + "<br/>Go back to <b>Design Library</b>.</html>");
        sub.setForeground(UiKit.MUTED);
        sub.setFont(UiKit.scaled(sub, Font.PLAIN, 0.95f));
        sub.setBorder(new EmptyBorder(8, 0, 0, 0));
        sub.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 6));
        btnRow.setOpaque(false);
        btnRow.setBorder(new EmptyBorder(12, 0, 0, 0));
        btnRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        JButton goLibrary = UiKit.primaryButton("Go to Design Library");
        goLibrary.addActionListener(e -> {
            if (router != null) router.show(ScreenKeys.DESIGN_LIBRARY);
        });

        JButton goSettings = UiKit.ghostButton("Open Settings");
        goSettings.addActionListener(e -> {
            if (router != null) router.show(ScreenKeys.SETTINGS);
        });

        btnRow.add(goLibrary);
        btnRow.add(goSettings);

        card.add(title);
        card.add(sub);
        card.add(btnRow);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0; gbc.gridy = 0;
        gbc.weightx = 1.0; gbc.weighty = 1.0;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(24, 18, 24, 18);

        wrap.add(card, gbc);
        return wrap;
    }

    // --- Data Loading ---

    private void reload() {
        if (userRepo == null) {
            allCustomers = new ArrayList<>();
            renderList(new ArrayList<>());
            return;
        }

        allCustomers = userRepo.listUsersByRole(User.ROLE_CUSTOMER);
        applyFilter();
    }

    private void applyFilter() {
        String q = (searchField == null) ? "" : searchField.getText().trim().toLowerCase(Locale.ROOT);

        List<User> filtered = new ArrayList<>();
        for (User u : allCustomers) {
            if (u == null) continue;

            String hay = (safe(u.getFullName()) + " " + safe(u.getEmail()) + " " +
                    safe(u.getJobTitle()) + " " + safe(u.getDepartment()))
                    .toLowerCase(Locale.ROOT);

            if (q.isBlank() || hay.contains(q)) filtered.add(u);
        }

        renderList(filtered);
    }

    private void renderList(List<User> customers) {
        listColumn.removeAll();

        int total = (customers == null) ? 0 : customers.size();
        if (metaLabel != null) {
            metaLabel.setText("Showing " + total + " customer" + (total == 1 ? "" : "s"));
        }

        if (customers == null || customers.isEmpty()) {
            listColumn.add(buildEmptyState("Try a different search, or add a new customer."));
        } else {
            for (int i = 0; i < customers.size(); i++) {
                User u = customers.get(i);
                listColumn.add(buildCustomerRow(u));
                if (i < customers.size() - 1) listColumn.add(Box.createVerticalStrut(10));
            }
        }

        listColumn.revalidate();
        listColumn.repaint();
    }

    private JComponent buildCustomerRow(User u) {
        UiKit.RoundedPanel card = new UiKit.RoundedPanel(14, UiKit.WHITE);
        card.setBorderPaint(UiKit.BORDER);
        card.setLayout(new BorderLayout(14, 0));
        card.setBorder(new EmptyBorder(16, 18, 16, 18));
        card.setAlignmentX(0.0f);
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 120));

        // Avatar
        String initials = initials(safe(u.getFullName()), safe(u.getEmail()));
        JPanel avatar = avatarCircle(initials);

        // Center: name + email + chips
        JPanel center = new JPanel();
        center.setOpaque(false);
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));

        String displayName = safe(u.getFullName());
        if (displayName.isBlank()) displayName = safe(u.getEmail());
        JLabel name = new JLabel(displayName);
        name.setForeground(UiKit.TEXT);
        name.setFont(UiKit.scaled(name, Font.BOLD, 1.05f));
        name.setAlignmentX(0.0f);

        JLabel email = new JLabel(safe(u.getEmail()));
        email.setForeground(UiKit.MUTED);
        email.setFont(UiKit.scaled(email, Font.PLAIN, 0.88f));
        email.setBorder(new EmptyBorder(2, 0, 0, 0));
        email.setAlignmentX(0.0f);

        JPanel chips = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        chips.setOpaque(false);
        chips.setBorder(new EmptyBorder(8, 0, 0, 0));
        chips.setAlignmentX(0.0f);

        String job = safe(u.getJobTitle());
        String dep = safe(u.getDepartment());

        if (!job.isBlank()) chips.add(UiKit.chip("Job: " + job));
        if (!dep.isBlank()) chips.add(UiKit.chip("Dept: " + dep));

        int designCount = 0;
        try {
            if (appState != null && appState.getRepo() != null) {
                designCount = appState.getRepo().countByOwner(u.getId());
            }
        } catch (Exception ignored) {}

        chips.add(UiKit.chipPrimary("Designs: " + designCount));

        center.add(name);
        center.add(email);
        center.add(chips);

        // Right: actions vertically centered
        JPanel actions = new JPanel();
        actions.setOpaque(false);
        actions.setLayout(new BoxLayout(actions, BoxLayout.X_AXIS));

        JButton edit = UiKit.ghostButton("Edit");
        edit.addActionListener(e -> openEditDialog(u));

        JButton reset = UiKit.ghostButton("Reset Password");
        reset.addActionListener(e -> openResetPasswordDialog(u));

        JButton del = UiKit.ghostButton("Delete");
        del.setForeground(new Color(0xB91C1C));
        del.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(0xFCA5A5), 1, true),
                new EmptyBorder(9, 12, 9, 12)
        ));
        del.addActionListener(e -> confirmDelete(u));

        actions.add(edit);
        actions.add(Box.createHorizontalStrut(6));
        actions.add(reset);
        actions.add(Box.createHorizontalStrut(6));
        actions.add(del);

        // Wrap actions in a vertically-centered panel
        JPanel actionsWrap = new JPanel(new GridBagLayout());
        actionsWrap.setOpaque(false);
        actionsWrap.add(actions);

        // Wrap avatar in a vertically-centered panel
        JPanel avatarWrap = new JPanel(new GridBagLayout());
        avatarWrap.setOpaque(false);
        avatarWrap.add(avatar);

        card.add(avatarWrap, BorderLayout.WEST);
        card.add(center, BorderLayout.CENTER);
        card.add(actionsWrap, BorderLayout.EAST);

        return card;
    }

    private String initials(String name, String email) {
        if (!name.isBlank()) {
            String[] parts = name.split("\\s+");
            if (parts.length >= 2) {
                return ("" + parts[0].charAt(0) + parts[parts.length - 1].charAt(0)).toUpperCase();
            }
            return name.substring(0, Math.min(2, name.length())).toUpperCase();
        }
        if (!email.isBlank()) {
            return email.substring(0, Math.min(2, email.length())).toUpperCase();
        }
        return "?";
    }

    private JPanel avatarCircle(String text) {
        JPanel p = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int sz = Math.min(getWidth(), getHeight());
                g2.setColor(UiKit.PRIMARY);
                g2.fillOval((getWidth() - sz) / 2, (getHeight() - sz) / 2, sz, sz);
                g2.setColor(Color.WHITE);
                g2.setFont(getFont().deriveFont(Font.BOLD, sz * 0.38f));
                FontMetrics fm = g2.getFontMetrics();
                int tw = fm.stringWidth(text);
                int th = fm.getAscent();
                g2.drawString(text, (getWidth() - tw) / 2, (getHeight() + th) / 2 - 2);
                g2.dispose();
            }
        };
        p.setOpaque(false);
        p.setPreferredSize(new Dimension(44, 44));
        p.setMinimumSize(new Dimension(44, 44));
        p.setMaximumSize(new Dimension(44, 44));
        return p;
    }

    // --- Dialogs ---

    // Step 5: Add Customer
    private void openAddDialog() {
        JDialog dialog = baseDialog("Add Customer");

        JPanel content = dialogContentPanel();
        dialog.setContentPane(content);

        JTextField fullName = field("Full Name");
        JTextField email = field("Email");
        JPasswordField pass = passwordField("Temp Password");

        JTextField job = field("Job Title (optional)");
        JTextField dept = field("Department (optional)");

        // Auto-generate a reasonable temp password
        String temp = "RV-" + (System.currentTimeMillis() % 1000000);
        pass.setText(temp);

        content.add(formRow("Full Name", fullName));
        content.add(formRow("Email", email));
        content.add(formRow("Temp Password", pass));
        content.add(Box.createVerticalStrut(10));
        content.add(formRow("Job Title", job));
        content.add(formRow("Department", dept));

        JLabel hint = hintLabel("Tip: You can reset the password later from the customer row.");
        content.add(Box.createVerticalStrut(10));
        content.add(hint);

        JPanel actions = dialogActionsRow();
        JButton cancel = UiKit.ghostButton("Cancel");
        cancel.addActionListener(e -> dialog.dispose());

        JButton create = UiKit.primaryButton("Create");
        create.addActionListener(e -> {
            String fn = safe(fullName.getText());
            String em = safe(email.getText()).toLowerCase(Locale.ROOT);
            char[] pw = pass.getPassword();

            if (fn.isBlank() || em.isBlank() || pw.length == 0) {
                warn("Please fill Full Name, Email, and Password.");
                return;
            }

            try {
                User created = userRepo.createUser(fn, em, pw, User.ROLE_CUSTOMER);
                // Immediately update optional profile fields if provided
                userRepo.updateProfile(created.getId(), fn, job.getText(), dept.getText());
                dialog.dispose();
                reload();
                info("Customer created successfully.");
            } catch (SQLException ex) {
                ex.printStackTrace();
                warn("Could not create customer. The email might already exist.");
            } catch (Exception ex) {
                ex.printStackTrace();
                warn("Something went wrong while creating the customer.");
            } finally {
                // best-effort clear
                for (int i = 0; i < pw.length; i++) pw[i] = 0;
            }
        });

        actions.add(cancel);
        actions.add(create);

        content.add(Box.createVerticalStrut(14));
        content.add(actions);

        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    // Step 6: Edit Customer
    private void openEditDialog(User u) {
        if (u == null) return;

        JDialog dialog = baseDialog("Edit Customer");

        JPanel content = dialogContentPanel();
        dialog.setContentPane(content);

        JTextField fullName = field("Full Name");
        JTextField job = field("Job Title");
        JTextField dept = field("Department");

        fullName.setText(safe(u.getFullName()));
        job.setText(safe(u.getJobTitle()));
        dept.setText(safe(u.getDepartment()));

        content.add(formRow("Full Name", fullName));
        content.add(formRow("Job Title", job));
        content.add(formRow("Department", dept));

        JPanel actions = dialogActionsRow();
        JButton cancel = UiKit.ghostButton("Cancel");
        cancel.addActionListener(e -> dialog.dispose());

        JButton save = UiKit.primaryButton("Save");
        save.addActionListener(e -> {
            String fn = safe(fullName.getText());
            if (fn.isBlank()) {
                warn("Full Name cannot be empty.");
                return;
            }
            try {
                userRepo.updateProfile(u.getId(), fn, job.getText(), dept.getText());
                dialog.dispose();
                reload();
                info("Customer updated successfully.");
            } catch (SQLException ex) {
                ex.printStackTrace();
                warn("Could not update customer.");
            }
        });

        actions.add(cancel);
        actions.add(save);

        content.add(Box.createVerticalStrut(14));
        content.add(actions);

        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    // Step 7: Reset Password
    private void openResetPasswordDialog(User u) {
        if (u == null) return;

        JDialog dialog = baseDialog("Reset Password");

        JPanel content = dialogContentPanel();
        dialog.setContentPane(content);

        JLabel who = hintLabel("Reset password for: " + safe(u.getEmail()));
        content.add(who);
        content.add(Box.createVerticalStrut(10));

        JPasswordField p1 = passwordField("New Password");
        JPasswordField p2 = passwordField("Confirm Password");

        content.add(formRow("New Password", p1));
        content.add(formRow("Confirm", p2));

        JPanel actions = dialogActionsRow();
        JButton cancel = UiKit.ghostButton("Cancel");
        cancel.addActionListener(e -> dialog.dispose());

        JButton save = UiKit.primaryButton("Update Password");
        save.addActionListener(e -> {
            char[] a = p1.getPassword();
            char[] b = p2.getPassword();

            if (a.length == 0 || b.length == 0) {
                warn("Please enter the new password twice.");
                return;
            }
            if (!same(a, b)) {
                warn("Passwords do not match.");
                return;
            }

            try {
                userRepo.updatePasswordById(u.getId(), a);
                dialog.dispose();
                info("Password updated successfully.");
            } catch (SQLException ex) {
                ex.printStackTrace();
                warn("Could not update password.");
            } finally {
                for (int i = 0; i < a.length; i++) a[i] = 0;
                for (int i = 0; i < b.length; i++) b[i] = 0;
            }
        });

        actions.add(cancel);
        actions.add(save);

        content.add(Box.createVerticalStrut(14));
        content.add(actions);

        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    // Step 8: Delete Customer (and cascade designs)
    private void confirmDelete(User u) {
        if (u == null) return;

        String msg = "Delete customer:\n\n"
                + safe(u.getFullName()) + " (" + safe(u.getEmail()) + ")\n\n"
                + "This will also delete all designs owned by this customer.\n\n"
                + "Are you sure?";

        int res = JOptionPane.showConfirmDialog(
                this,
                msg,
                "Confirm Delete",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );

        if (res != JOptionPane.YES_OPTION) return;

        try {
            userRepo.deleteUserById(u.getId());
            reload();
            info("Customer deleted successfully.");
        } catch (SQLException ex) {
            ex.printStackTrace();
            warn("Could not delete customer.");
        }
    }

    // --- Dialog Helpers ---

    private JDialog baseDialog(String title) {
        Window w = SwingUtilities.getWindowAncestor(this);
        JDialog d = new JDialog(w, title, Dialog.ModalityType.APPLICATION_MODAL);
        d.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        d.setResizable(false);
        return d;
    }

    private JPanel dialogContentPanel() {
        JPanel p = new JPanel();
        p.setBackground(UiKit.WHITE);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBorder(new EmptyBorder(16, 16, 16, 16));
        return p;
    }

    private JPanel dialogActionsRow() {
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        actions.setOpaque(false);
        return actions;
    }

    private JTextField field(String placeholder) {
        JTextField t = new JTextField(24);
        t.setFont(UiKit.scaled(t, Font.PLAIN, 0.95f));
        t.setForeground(UiKit.TEXT);
        t.setBackground(UiKit.WHITE);
        t.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(UiKit.BORDER, 1, true),
                new EmptyBorder(9, 10, 9, 10)
        ));
        t.putClientProperty("JTextField.placeholderText", placeholder);
        return t;
    }

    private JPasswordField passwordField(String placeholder) {
        JPasswordField t = new JPasswordField(24);
        t.setFont(UiKit.scaled(t, Font.PLAIN, 0.95f));
        t.setForeground(UiKit.TEXT);
        t.setBackground(UiKit.WHITE);
        t.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(UiKit.BORDER, 1, true),
                new EmptyBorder(9, 10, 9, 10)
        ));
        t.putClientProperty("JTextField.placeholderText", placeholder);
        return t;
    }

    private JComponent formRow(String label, JComponent field) {
        JPanel row = new JPanel(new BorderLayout(10, 6));
        row.setOpaque(false);
        row.setBorder(new EmptyBorder(6, 0, 0, 0));

        JLabel l = new JLabel(label);
        l.setForeground(UiKit.MUTED);
        l.setFont(UiKit.scaled(l, Font.BOLD, 0.90f));
        l.setPreferredSize(new Dimension(120, 0));

        row.add(l, BorderLayout.WEST);
        row.add(field, BorderLayout.CENTER);
        return row;
    }

    private JLabel hintLabel(String text) {
        JLabel l = new JLabel("<html>" + safe(text) + "</html>");
        l.setForeground(UiKit.MUTED);
        l.setFont(UiKit.scaled(l, Font.PLAIN, 0.92f));
        return l;
    }

    // --- Utility ---

    private static String safe(String s) {
        return (s == null) ? "" : s.trim();
    }

    private static boolean same(char[] a, char[] b) {
        if (a == null || b == null) return false;
        if (a.length != b.length) return false;
        for (int i = 0; i < a.length; i++) {
            if (a[i] != b[i]) return false;
        }
        return true;
    }

    private void info(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Info", JOptionPane.INFORMATION_MESSAGE);
    }

    private void warn(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Warning", JOptionPane.WARNING_MESSAGE);
    }
}