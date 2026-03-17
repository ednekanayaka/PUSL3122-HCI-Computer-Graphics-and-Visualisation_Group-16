package com.roomviz.screens;

import com.roomviz.app.AppFrame;
import com.roomviz.app.Router;
import com.roomviz.app.ScreenKeys;
import com.roomviz.data.AppState;
import com.roomviz.data.DesignRepository;
import com.roomviz.data.Session;
import com.roomviz.data.SettingsRepository;
import com.roomviz.data.UserRepository;
import com.roomviz.model.User;
import com.roomviz.ui.UiKit;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * Main shell hosting TopBar, Sidebar, and inner router screens.
 * Handles role-based access and presentation mode.
 */
public class ShellScreen extends JPanel {

    private final Router innerRouter = new Router();

    private TopBar topBar;
    private Sidebar sidebar;
    private JPanel body;

    // Keep references so we can rebuild safely if needed
    private final AppFrame frame;
    private final Router outerRouter;
    private final SettingsRepository settingsRepo;
    private final UserRepository userRepo;
    private final Session session;

    // App state is per-login (depends on user role)
    private AppState appState;

    public ShellScreen(AppFrame frame,
                       Router outerRouter,
                       SettingsRepository settingsRepo,
                       UserRepository userRepo,
                       Session session) {

        this.frame = frame;
        this.outerRouter = outerRouter;
        this.settingsRepo = settingsRepo;
        this.userRepo = userRepo;
        this.session = session;

        // If not logged in, redirect (do not build UI)
        if (this.session == null || !this.session.isLoggedIn()) {
            SwingUtilities.invokeLater(frame::goToLogin);
            return;
        }

        // Apply saved accessibility/theme settings BEFORE building UI
        if (this.settingsRepo != null) {
            UiKit.applySettings(this.settingsRepo.get());
        }

        setLayout(new BorderLayout());
        setOpaque(true);
        setBackground(UiKit.BG);

        buildShell();
    }

    /**
     * Build (or rebuild) the entire Shell UI.
     * Safe to call when settings change or user context changes.
     */
    private void buildShell() {
        removeAll();

        // Guard session again (if called after logout)
        if (session == null || !session.isLoggedIn()) {
            SwingUtilities.invokeLater(frame::goToLogin);
            return;
        }

        User me = session.getCurrentUser();
        if (me == null) {
            SwingUtilities.invokeLater(frame::goToLogin);
            return;
        }

        int uid = me.getId();
        boolean isCustomer = me.isCustomer();
        boolean isAdmin = me.isAdmin();

        // Repo depends on role
        DesignRepository repo = isAdmin
                ? DesignRepository.createAdminView(uid)
                : DesignRepository.createForOwner(uid);

        appState = new AppState(repo);

        // Let shell background show through behind inner pages
        innerRouter.root().setOpaque(false);

        // TopBar reads session (and can show user name / logout etc.)
        topBar = new TopBar(frame, session);

        // Pages inside shell (ROLE-BASED)

        // Everyone has Design Library + 2D + 3D + Settings
        innerRouter.add(
                ScreenKeys.DESIGN_LIBRARY,
                new DesignLibraryPage(frame, innerRouter, appState, session)
        );

        innerRouter.add(
                ScreenKeys.PLANNER_2D,
                new Planner2DPage(frame, innerRouter, appState, settingsRepo, session)
        );

        innerRouter.add(
                ScreenKeys.VIEW_3D,
                new Visual3DPage(frame, innerRouter, appState, session, this::setPresentationModeOnShell)
        );

        innerRouter.add(
                ScreenKeys.SETTINGS,
                new SettingsPage(frame, outerRouter, appState, settingsRepo, userRepo, session, () -> {
                    // Settings page can request shell rebuild (theme scaling etc.)
                    frame.rebuildShell();
                })
        );

        // IMPORTANT:
        // Register admin-only screen keys even for customers,
        // so any accidental router.show(...) never leads to a blank state.
        if (isAdmin) {
            innerRouter.add(ScreenKeys.DASHBOARD, new DashboardPage(frame, innerRouter, appState));

            innerRouter.add(
                    ScreenKeys.NEW_DESIGN,
                    new NewDesignWizardPage(frame, innerRouter, appState, settingsRepo, userRepo, session)
            );

            innerRouter.add(ScreenKeys.DESIGN_DETAILS, new DesignDetailsPage(frame, innerRouter, appState));
            innerRouter.add(ScreenKeys.SHADING_COLOR, new ShadingColorPage(frame, innerRouter, appState));

            // Customers page (Admin-only)
            // NOTE: Ensure ScreenKeys.CUSTOMERS exists: public static final String CUSTOMERS = "customers";
            innerRouter.add(
                    ScreenKeys.CUSTOMERS,
                    new CustomersPage(frame, innerRouter, appState, userRepo, session)
            );

        } else {
            // Customer placeholders (safe redirect UI)
            innerRouter.add(ScreenKeys.DASHBOARD, notAllowedPage("Dashboard is admin-only."));
            innerRouter.add(ScreenKeys.NEW_DESIGN, notAllowedPage("Creating a new design is admin-only."));
            innerRouter.add(ScreenKeys.DESIGN_DETAILS, notAllowedPage("Design details tools are admin-only."));
            innerRouter.add(ScreenKeys.SHADING_COLOR, notAllowedPage("Shading & Colour tools are admin-only."));

            // Customers placeholder (Customer cannot access)
            innerRouter.add(ScreenKeys.CUSTOMERS, notAllowedPage("Customers management is admin-only."));
        }

        // Pass session so Sidebar can hide admin-only nav for customers
        sidebar = new Sidebar(innerRouter, topBar::setTitle, session);

        // Default landing: customer -> library, admin -> dashboard
        if (isCustomer) {
            innerRouter.show(ScreenKeys.DESIGN_LIBRARY);
            topBar.setTitle("Design Library");
        } else {
            innerRouter.show(ScreenKeys.DASHBOARD);
            topBar.setTitle("Dashboard");
        }

        body = new JPanel(new BorderLayout());
        body.setOpaque(false);
        body.add(sidebar, BorderLayout.WEST);
        body.add(innerRouter.root(), BorderLayout.CENTER);

        add(topBar, BorderLayout.NORTH);
        add(body, BorderLayout.CENTER);

        revalidate();
        repaint();
    }

    private JComponent notAllowedPage(String message) {
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
        goLibrary.addActionListener(e -> innerRouter.show(ScreenKeys.DESIGN_LIBRARY));

        JButton goSettings = UiKit.ghostButton("Open Settings");
        goSettings.addActionListener(e -> showSettings());

        btnRow.add(goLibrary);
        btnRow.add(goSettings);

        card.add(title);
        card.add(sub);
        card.add(btnRow);

        JPanel row = new JPanel();
        row.setOpaque(false);
        row.setLayout(new BoxLayout(row, BoxLayout.X_AXIS));
        row.add(Box.createHorizontalGlue());
        row.add(card);
        row.add(Box.createHorizontalGlue());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(24, 18, 24, 18);

        wrap.add(row, gbc);
        return wrap;
    }

    /** Convenience for outer callers (e.g., AppFrame) */
    public void showSettings() {
        innerRouter.show(ScreenKeys.SETTINGS);
        if (topBar != null) topBar.setTitle("Settings");
    }

    /** Called by Visual3DPage when presentation mode toggles */
    private void setPresentationModeOnShell(boolean on) {
        if (topBar != null) topBar.setVisible(!on);
        if (sidebar != null) sidebar.setVisible(!on);

        revalidate();
        repaint();
    }

    /** Optional helper if you ever need access to state from outside */
    public AppState getAppState() {
        return appState;
    }
}