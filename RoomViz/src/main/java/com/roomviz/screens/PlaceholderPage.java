package com.roomviz.screens;

import javax.swing.*;
import java.awt.*;

public class PlaceholderPage extends JPanel {

    public PlaceholderPage(String title) {
        setLayout(new BorderLayout());
        JLabel label = new JLabel(title, SwingConstants.CENTER);
        label.setFont(label.getFont().deriveFont(28f));
        add(label, BorderLayout.CENTER);
    }
}
