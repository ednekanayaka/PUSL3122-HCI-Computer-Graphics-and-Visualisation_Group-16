package com.roomviz.app;

import com.roomviz.screens.LoginScreen;
import com.roomviz.screens.ShellScreen;

import javax.swing.*;

public class AppFrame extends JFrame {

    private final Router router = new Router();

    public AppFrame() {
        super("RoomViz");

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 750);
        setLocationRelativeTo(null);

        // Register screens
        router.add(ScreenKeys.LOGIN, new LoginScreen(this, router));
        router.add(ScreenKeys.APP, new ShellScreen(this, router));

        setContentPane(router.root());
        router.show(ScreenKeys.LOGIN);
    }

    public void goToAppShell() {
        router.show(ScreenKeys.APP);
    }

    public void goToLogin() {
        router.show(ScreenKeys.LOGIN);
    }
}
