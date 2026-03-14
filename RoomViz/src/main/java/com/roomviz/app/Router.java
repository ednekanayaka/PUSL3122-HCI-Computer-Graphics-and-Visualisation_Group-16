package com.roomviz.app;

import javax.swing.*;
import java.awt.*;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class Router {
    private final CardLayout layout = new CardLayout();
    private final JPanel root = new JPanel(layout);
    private final List<Consumer<String>> listeners = new ArrayList<>();

    public JPanel root() {
        return root;
    }

    public void add(String key, JComponent screen) {
        root.add(screen, key);
    }

    public void show(String key) {
        layout.show(root, key);
        for (Consumer<String> l : listeners) {
            l.accept(key);
        }
    }

    public void addListener(Consumer<String> listener) {
        listeners.add(listener);
    }
}
