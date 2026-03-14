package com.roomviz.screens;

import com.roomviz.app.AppFrame;
import com.roomviz.app.Router;
import com.roomviz.app.ScreenKeys;
import com.roomviz.data.AppState;
import com.roomviz.data.DesignRepository;

import javax.swing.*;
import java.awt.*;

public class ShellScreen extends JPanel {

    private final Router innerRouter = new Router();

    public ShellScreen(AppFrame frame, Router outerRouter) {
        setLayout(new BorderLayout());
        setBackground(new Color(0xF6F7FB));

        // Shared state across screens (Step 1: save/load designs + selection)
        DesignRepository repo = DesignRepository.createDefault();
        AppState appState = new AppState(repo);

        // Let shell background show through behind inner pages
        innerRouter.root().setOpaque(false);

        // Minimal TopBar (title controlled by Sidebar callback)
        TopBar topBar = new TopBar(frame);

        /* =========================
           Pages inside shell
           ========================= */

        innerRouter.add(ScreenKeys.DASHBOARD, new DashboardPage(frame, innerRouter, appState));

        // ✅ REAL pages (wired)
        innerRouter.add(ScreenKeys.DESIGN_LIBRARY, new DesignLibraryPage(frame, innerRouter, appState));
        innerRouter.add(ScreenKeys.NEW_DESIGN, new NewDesignWizardPage(frame, innerRouter, appState));
        innerRouter.add(ScreenKeys.DESIGN_DETAILS, new DesignDetailsPage(frame, innerRouter, appState));

        // ✅ Design tools (NOW share AppState + Router)
        innerRouter.add(ScreenKeys.PLANNER_2D, new Planner2DPage(frame, innerRouter, appState));
        innerRouter.add(ScreenKeys.VIEW_3D, new Visual3DPage(frame, innerRouter, appState));
        innerRouter.add(ScreenKeys.SHADING_COLOR, new ShadingColorPage(frame, innerRouter, appState));

        // ✅ Settings
        innerRouter.add(ScreenKeys.SETTINGS, new SettingsPage(frame));

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
