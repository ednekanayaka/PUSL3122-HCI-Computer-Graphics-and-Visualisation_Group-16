package com.roomviz.app;

import javax.swing.*;
import java.awt.CardLayout;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class Router {

    private final CardLayout layout = new CardLayout();
    private final JPanel root = new JPanel(layout);

    private final List<Consumer<String>> listeners = new ArrayList<>();

    // Keep track of screens so we can replace them safely
    private final Map<String, JComponent> screensByKey = new HashMap<>();

    public JPanel root() {
        return root;
    }

    /**
     * Add a screen by key.
     * If the key already exists, replace the old screen in the CardLayout.
     */
    public void add(String key, JComponent screen) {
        if (key == null || screen == null) return;

        JComponent old = screensByKey.get(key);
        if (old != null) {
            root.remove(old);
        }

        screensByKey.put(key, screen);
        root.add(screen, key);

        root.revalidate();
        root.repaint();
    }

    public void show(String key) {
        layout.show(root, key);
        for (Consumer<String> l : listeners) {
            l.accept(key);
        }
    }

    public void addListener(Consumer<String> listener) {
        if (listener != null) listeners.add(listener);
    }
}