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

    // ✅ Single shared settings repo for the whole app (theme + user display info)
    private final SettingsRepository settingsRepo = SettingsRepository.createDefault();

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

        // Register screens (now with real auth wiring)
        router.add(ScreenKeys.LOGIN, new LoginScreen(this, router, settingsRepo, userRepo, session));
        router.add(ScreenKeys.REGISTER, new RegisterScreen(this, router, settingsRepo, userRepo, session));
        router.add(ScreenKeys.APP, new ShellScreen(this, router, settingsRepo, userRepo, session));

        setContentPane(router.root());
        router.show(ScreenKeys.LOGIN);
    }

    public void goToAppShell() {
        router.show(ScreenKeys.APP);
    }

    public void goToLogin() {
        router.show(ScreenKeys.LOGIN);
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