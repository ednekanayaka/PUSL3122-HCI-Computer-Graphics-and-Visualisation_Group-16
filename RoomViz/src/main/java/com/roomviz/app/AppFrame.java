package com.roomviz.app;

import com.roomviz.data.SettingsRepository;
import com.roomviz.screens.LoginScreen;
import com.roomviz.screens.ShellScreen;
import com.roomviz.ui.UiKit;

import javax.swing.*;

public class AppFrame extends JFrame {

    private final Router router = new Router();

    // ✅ Single shared settings repo for the whole app (login + shell + topbar + settings)
    private final SettingsRepository settingsRepo = SettingsRepository.createDefault();

    public AppFrame() {
        super("RoomViz");

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 750);
        setLocationRelativeTo(null);

        // ✅ Apply theme before building UI
        UiKit.applySettings(settingsRepo.get());

        // Register screens
        router.add(ScreenKeys.LOGIN, new LoginScreen(this, router, settingsRepo));
        router.add(ScreenKeys.APP, new ShellScreen(this, router, settingsRepo));

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
}
