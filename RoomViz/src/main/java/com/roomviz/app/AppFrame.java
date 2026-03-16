package com.roomviz.app;

import com.roomviz.data.SettingsRepository;
import com.roomviz.data.Session;
import com.roomviz.data.UserRepository;
import com.roomviz.screens.LoginScreen;
import com.roomviz.screens.RegisterScreen;
import com.roomviz.screens.ShellScreen;
import com.roomviz.ui.UiKit;

import javax.swing.*;

public class AppFrame extends JFrame {

    private final Router router = new Router();

    // ✅ Start with default settings (login/register uses this)
    // After login, we swap to per-user settingsRepo
    private SettingsRepository settingsRepo = SettingsRepository.createDefault();

    // ✅ Real authentication storage (SQLite) + session
    private final UserRepository userRepo = UserRepository.createDefault();
    private final Session session = new Session();

    public AppFrame() {
        super("RoomViz");

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 750);
        setLocationRelativeTo(null);

        // ✅ Apply theme before building UI
        UiKit.applySettings(settingsRepo.get());

        // ✅ Register only auth screens at startup
        router.add(ScreenKeys.LOGIN, new LoginScreen(this, router, settingsRepo, userRepo, session));
        router.add(ScreenKeys.REGISTER, new RegisterScreen(this, router, settingsRepo, userRepo, session));

        setContentPane(router.root());

        // ✅ Start at login
        router.show(ScreenKeys.LOGIN);
    }

    /**
     * ✅ Called after successful login/register.
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
}