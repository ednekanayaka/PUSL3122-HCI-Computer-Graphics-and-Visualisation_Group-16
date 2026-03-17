package com.roomviz.app;

import com.roomviz.data.SettingsRepository;
import com.roomviz.data.Session;
import com.roomviz.data.UserRepository;
import com.roomviz.screens.LoginScreen;
import com.roomviz.screens.RegisterScreen;
import com.roomviz.screens.ShellScreen;
import com.roomviz.ui.UiKit;

import javax.swing.*;
import java.awt.*;

public class AppFrame extends JFrame {

    private final Router router = new Router();

    // Start with default settings (login/register uses this)
    // After login, we swap to per-user settingsRepo
    private SettingsRepository settingsRepo = SettingsRepository.createDefault();

    // Real authentication storage (SQLite) + session
    private final UserRepository userRepo = UserRepository.createDefault();
    private final Session session = new Session();

    // Presentation / Fullscreen state
    private boolean presentationMode = false;
    private boolean prevUndecorated = false;
    private Rectangle prevBounds = null;
    private int prevExtendedState = JFrame.NORMAL;

    public AppFrame() {
        super("RoomViz");

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 750);
        setLocationRelativeTo(null);

        // Apply theme before building UI
        UiKit.applySettings(settingsRepo.get());

        // Register only auth screens at startup
        router.add(ScreenKeys.LOGIN, new LoginScreen(this, router, settingsRepo, userRepo, session));
        router.add(ScreenKeys.REGISTER, new RegisterScreen(this, router, settingsRepo, userRepo, session));

        setContentPane(router.root());

        // Start at login
        router.show(ScreenKeys.LOGIN);
    }

    /**
     * Called after successful login/register.
     * Switch to per-user settings file, then build shell.
     */
    public void goToAppShell() {
        if (session != null && session.isLoggedIn() && session.getCurrentUser() != null) {
            int uid = session.getCurrentUser().getId();
            this.settingsRepo = SettingsRepository.createForUser(uid);
            UiKit.applySettings(settingsRepo.get());
        }

        router.add(ScreenKeys.APP, new ShellScreen(this, router, settingsRepo, userRepo, session));
        router.show(ScreenKeys.APP);
    }

    /**
     * Rebuild the shell (and all its inner screens) after a theme change.
     * This fully re-creates every screen so they pick up the new UiKit tokens.
     */
    public void rebuildShell() {
        if (session == null || !session.isLoggedIn()) return;
        UiKit.applySettings(settingsRepo.get());

        ShellScreen newShell = new ShellScreen(this, router, settingsRepo, userRepo, session);
        router.add(ScreenKeys.APP, newShell);
        router.show(ScreenKeys.APP);

        // Navigate to settings page so the user stays where they were
        newShell.showSettings();

        SwingUtilities.invokeLater(() -> {
            invalidate();
            validate();
            repaint();
        });
    }

    public void goToLogin() {
        router.show(ScreenKeys.LOGIN);
    }

    public void goToRegister() {
        router.show(ScreenKeys.REGISTER);
    }

    public SettingsRepository getSettingsRepo() {
        return settingsRepo;
    }

    public UserRepository getUserRepo() {
        return userRepo;
    }

    public Session getSession() {
        return session;
    }

    // Presentation / Fullscreen helpers

    /** Enter fullscreen presentation mode (undecorated + maximized). */
    public void enterPresentationMode() {
        if (presentationMode) return;

        presentationMode = true;
        prevBounds = getBounds();
        prevExtendedState = getExtendedState();
        prevUndecorated = isUndecorated();

        // Need dispose() before changing decorations
        try { dispose(); } catch (Throwable ignored) { }

        setUndecorated(true);
        setExtendedState(JFrame.MAXIMIZED_BOTH);

        setVisible(true);
        toFront();
        requestFocus();
    }

    /** Exit presentation mode and restore previous window state. */
    public void exitPresentationMode() {
        if (!presentationMode) return;

        presentationMode = false;

        try { dispose(); } catch (Throwable ignored) { }

        setUndecorated(prevUndecorated);
        setVisible(true);

        setExtendedState(prevExtendedState);
        if (prevBounds != null) setBounds(prevBounds);

        toFront();
        requestFocus();
    }

    public void togglePresentationMode() {
        if (presentationMode) exitPresentationMode();
        else enterPresentationMode();
    }

    public boolean isPresentationMode() {
        return presentationMode;
    }
}