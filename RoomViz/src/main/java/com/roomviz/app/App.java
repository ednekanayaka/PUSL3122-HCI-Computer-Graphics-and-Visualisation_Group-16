package com.roomviz.app;

import com.formdev.flatlaf.FlatLightLaf;

import javax.swing.*;

public class App {
    public static void main(String[] args) {
        FlatLightLaf.setup();

        SwingUtilities.invokeLater(() -> {
            AppFrame frame = new AppFrame();
            frame.setVisible(true);
        });
    }
}
