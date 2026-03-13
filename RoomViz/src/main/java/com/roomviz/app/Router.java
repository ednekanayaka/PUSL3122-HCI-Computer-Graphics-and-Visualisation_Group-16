package com.roomviz.app;

import javax.swing.*;
import java.awt.*;

public class Router {
    private final CardLayout layout = new CardLayout();
    private final JPanel root = new JPanel(layout);

    public JPanel root() {
        return root;
    }

    public void add(String key, JComponent screen) {
        root.add(screen, key);
    }

    public void show(String key) {
        layout.show(root, key);
    }
}
