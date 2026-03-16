package com.roomviz.screens;

import com.roomviz.app.AppFrame;
import com.roomviz.app.Router;
import com.roomviz.app.ScreenKeys;
import com.roomviz.data.AppState;
import com.roomviz.data.DesignRepository;
import com.roomviz.data.Session;
import com.roomviz.data.SettingsRepository;
import com.roomviz.data.UserRepository;
import com.roomviz.ui.UiKit;

import javax.swing.*;
import java.awt.*;

public class ShellScreen extends JPanel {

    private final Router innerRouter = new Router();

    public ShellScreen(AppFrame frame,
                       Router outerRouter,
                       SettingsRepository settingsRepo,
                       UserRepository userRepo,
                       Session session) {

        // ✅ BLOCK ACCESS: if not logged in, kick back to login
        if (session == null || !session.isLoggedIn()) {
            SwingUtilities.invokeLater(frame::goToLogin);
            return;
        }

        // ✅ Apply saved accessibility settings BEFORE building UI
        UiKit.applySettings(settingsRepo.get());

        setLayout(new BorderLayout());
        setBackground(UiKit.BG);

        // Shared state across screens (save/load designs + selection)
        DesignRepository repo = DesignRepository.createDefault();
        AppState appState = new AppState(repo);

        // Let shell background show through behind inner pages
        innerRouter.root().setOpaque(false);

        // ✅ TopBar now uses Session logout + reads display info from settings
        TopBar topBar = new TopBar(frame, settingsRepo, session);

        /* =========================
           Pages inside shell
           ========================= */

        innerRouter.add(ScreenKeys.DASHBOARD, new DashboardPage(frame, innerRouter, appState));

        innerRouter.add(ScreenKeys.DESIGN_LIBRARY, new DesignLibraryPage(frame, innerRouter, appState));
        innerRouter.add(ScreenKeys.NEW_DESIGN, new NewDesignWizardPage(frame, innerRouter, appState, settingsRepo));
        innerRouter.add(ScreenKeys.DESIGN_DETAILS, new DesignDetailsPage(frame, innerRouter, appState));

        innerRouter.add(ScreenKeys.PLANNER_2D, new Planner2DPage(frame, innerRouter, appState, settingsRepo));
        innerRouter.add(ScreenKeys.VIEW_3D, new Visual3DPage(frame, innerRouter, appState));
        innerRouter.add(ScreenKeys.SHADING_COLOR, new ShadingColorPage(frame, innerRouter, appState));

        innerRouter.add(
                ScreenKeys.SETTINGS,
                new SettingsPage(frame, outerRouter, appState, settingsRepo, () -> {
                    // refresh TopBar immediately after saving settings
                    topBar.refreshUserFromSettings();

                    // ✅ Apply + refresh whole window (no cumulative scaling now because UiKit is fixed)
                    UiKit.applySettings(settingsRepo.get());
                    UiKit.refreshUI(frame);

                    // ✅ Ensure shell background also updates (contrast mode)
                    setBackground(UiKit.BG);
                })
        );

        // Sidebar (ALL navigation here)
        Sidebar sidebar = new Sidebar(frame, innerRouter, topBar::setTitle);

        // Default page
        innerRouter.show(ScreenKeys.DASHBOARD);
        topBar.setTitle("Dashboard");

        // Layout: top bar + (sidebar + content)
        JPanel body = new JPanel(new BorderLayout());
        body.setOpaque(false);
        body.add(sidebar, BorderLayout.WEST);
        body.add(innerRouter.root(), BorderLayout.CENTER);

        add(topBar, BorderLayout.NORTH);
        add(body, BorderLayout.CENTER);
    }
}