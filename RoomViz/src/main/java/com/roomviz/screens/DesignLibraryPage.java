package com.roomviz.screens;

import com.roomviz.app.AppFrame;
import com.roomviz.app.Router;
import com.roomviz.app.ScreenKeys;
import com.roomviz.ui.UiKit;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class DesignLibraryPage extends JPanel {

    private final Router router;

    public DesignLibraryPage(AppFrame frame, Router router) {
        this.router = router;

        setLayout(new BorderLayout());
        setOpaque(false);

        JPanel content = new JPanel();
        content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBorder(new EmptyBorder(22, 24, 22, 24));

        content.add(headerRow());
        content.add(Box.createVerticalStrut(14));
        content.add(searchFilterCard());
        content.add(Box.createVerticalStrut(16));
        content.add(metaRow());
        content.add(Box.createVerticalStrut(12));
        content.add(grid(frame));
        content.add(Box.createVerticalStrut(18));

        JScrollPane scroller = new JScrollPane(content);
        scroller.setBorder(BorderFactory.createEmptyBorder());
        scroller.getViewport().setOpaque(true);
        scroller.getViewport().setBackground(UiKit.BG);
        scroller.setOpaque(false);
        scroller.getVerticalScrollBar().setUnitIncrement(18);

        add(scroller, BorderLayout.CENTER);
    }

    private JComponent headerRow() {
        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(false);

        JPanel left = new JPanel();
        left.setOpaque(false);
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("Design Library");
        title.setForeground(UiKit.TEXT);
        title.setFont(UiKit.semibold(title.getFont(), 24f));

        JLabel sub = new JLabel("Manage and organize your saved room designs");
        sub.setForeground(UiKit.MUTED);
        sub.setFont(UiKit.regular(sub.getFont(), 12.5f));
        sub.setBorder(new EmptyBorder(6, 0, 0, 0));

        left.add(title);
        left.add(sub);

        JButton cta = UiKit.primaryButton("+   Create New Design");
        // ✅ Navigate to the Wizard page
        cta.addActionListener(e -> this.router.show(ScreenKeys.NEW_DESIGN));

        row.add(left, BorderLayout.WEST);
        row.add(cta, BorderLayout.EAST);
        return row;
    }

    private JComponent searchFilterCard() {
        UiKit.RoundedPanel card = new UiKit.RoundedPanel(14, UiKit.WHITE);
        card.setBorderPaint(UiKit.BORDER);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(new EmptyBorder(12, 12, 12, 12));

        // Row 1: search + filter button
        JPanel row1 = new JPanel(new BorderLayout(10, 0));
        row1.setOpaque(false);

        JTextField search = UiKit.searchField("Search designs by name, customer, or tags...");
        JButton filters = UiKit.ghostButton("Filters");
        filters.setPreferredSize(new Dimension(110, filters.getPreferredSize().height));

        row1.add(search, BorderLayout.CENTER);
        row1.add(filters, BorderLayout.EAST);

        // Row 2: tags chips
        JPanel tags = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 10));
        tags.setOpaque(false);
        tags.add(new JLabel("Tags:"));
        tags.add(UiKit.chipPrimary("Modern"));
        tags.add(UiKit.chip("Classic"));
        tags.add(UiKit.chip("Minimalist"));
        tags.add(UiKit.chip("Industrial"));
        tags.add(linkChip("+ More"));

        // Row 3: dropdown filters (Room Shape, Size)
        JPanel row3 = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        row3.setOpaque(false);

        JLabel roomShapeLbl = new JLabel("Room Shape:");
        roomShapeLbl.setForeground(UiKit.MUTED);
        roomShapeLbl.setFont(roomShapeLbl.getFont().deriveFont(11.5f));

        JComboBox<String> shapes = new JComboBox<>(new String[]{"All Shapes", "Rectangular", "Square", "L-Shape"});
        UiKit.styleDropdown(shapes);

        JLabel sizeLbl = new JLabel("Size:");
        sizeLbl.setForeground(UiKit.MUTED);
        sizeLbl.setFont(sizeLbl.getFont().deriveFont(11.5f));

        JComboBox<String> size = new JComboBox<>(new String[]{"All Sizes", "Small", "Medium", "Large"});
        UiKit.styleDropdown(size);

        row3.add(roomShapeLbl);
        row3.add(shapes);
        row3.add(sizeLbl);
        row3.add(size);

        card.add(row1);
        card.add(tags);
        card.add(row3);

        return card;
    }

    private JComponent metaRow() {
        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(false);

        JLabel left = new JLabel("Showing 24 designs    ");
        left.setForeground(UiKit.MUTED);
        left.setFont(left.getFont().deriveFont(11.8f));

        JLabel clear = new JLabel("Clear all filters");
        clear.setForeground(UiKit.PRIMARY);
        clear.setFont(clear.getFont().deriveFont(Font.PLAIN, 11.8f));

        JPanel leftWrap = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        leftWrap.setOpaque(false);
        leftWrap.add(left);
        leftWrap.add(clear);

        JPanel rightWrap = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        rightWrap.setOpaque(false);

        JLabel sortLbl = new JLabel("Sort by:");
        sortLbl.setForeground(UiKit.MUTED);
        sortLbl.setFont(sortLbl.getFont().deriveFont(11.8f));

        JComboBox<String> sort = new JComboBox<>(new String[]{"Most Recent", "Oldest", "Name A-Z"});
        UiKit.styleDropdown(sort);

        JButton grid = UiKit.iconButton("▦");
        JButton list = UiKit.iconButton("≡");

        rightWrap.add(sortLbl);
        rightWrap.add(sort);
        rightWrap.add(grid);
        rightWrap.add(list);

        row.add(leftWrap, BorderLayout.WEST);
        row.add(rightWrap, BorderLayout.EAST);
        return row;
    }

    private JComponent grid(AppFrame frame) {
        // 4 columns like your Figma screenshot at 1200w
        JPanel grid = new JPanel(new GridLayout(0, 4, 14, 14));
        grid.setOpaque(false);

        grid.add(designCard("Riverside Apartment Living Room", "Johnson Family", "24' x 18'", "2 days ago"));
        grid.add(designCard("Downtown Loft Master Bedroom", "Martinez Residence", "15' x 20'", "6 days ago"));
        grid.add(designCard("Urban Kitchen Renovation", "Chen Family", "12' x 16'", "1 week ago"));
        grid.add(designCard("Executive Home Office", "Thompson & Associates", "14' x 18'", "2 weeks ago"));

        grid.add(designCard("Luxury Dining Space", "Williams Estate", "16' x 22'", "3 weeks ago"));
        grid.add(designCard("Scandinavian Nursery", "Anderson Family", "11' x 13'", "1 month ago"));
        grid.add(designCard("Beach House Bathroom", "Roberts Vacation Home", "9' x 12'", "1 month ago"));
        grid.add(designCard("Farmhouse Mudroom", "Peterson Ranch", "8' x 14'", "2 months ago"));

        return grid;
    }

    private JComponent designCard(String title, String client, String size, String timeAgo) {
        UiKit.RoundedPanel card = new UiKit.RoundedPanel(14, UiKit.WHITE);
        card.setBorderPaint(UiKit.BORDER);
        card.setLayout(new BorderLayout());
        card.setBorder(new EmptyBorder(10, 10, 10, 10));

        // Image placeholder (top)
        UiKit.RoundedPanel img = new UiKit.RoundedPanel(14, new Color(0xE5E7EB));
        img.setPreferredSize(new Dimension(0, 130));
        img.setLayout(new GridBagLayout());
        JLabel imgLbl = new JLabel("Image");
        imgLbl.setForeground(new Color(0x9CA3AF));
        img.add(imgLbl);

        // Info (middle)
        JPanel info = new JPanel();
        info.setOpaque(false);
        info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));
        info.setBorder(new EmptyBorder(10, 2, 6, 2));

        JLabel t = new JLabel(title);
        t.setForeground(UiKit.TEXT);
        t.setFont(t.getFont().deriveFont(Font.BOLD, 12.8f));

        JLabel sub = new JLabel(client);
        sub.setForeground(UiKit.MUTED);
        sub.setFont(sub.getFont().deriveFont(11.5f));
        sub.setBorder(new EmptyBorder(4, 0, 0, 0));

        JLabel meta = new JLabel(size + "   •   " + timeAgo);
        meta.setForeground(new Color(0x9CA3AF));
        meta.setFont(meta.getFont().deriveFont(11.2f));
        meta.setBorder(new EmptyBorder(6, 0, 0, 0));

        info.add(t);
        info.add(sub);
        info.add(meta);

        // Actions (bottom)
        JPanel actions = new JPanel(new BorderLayout(10, 0));
        actions.setOpaque(false);
        actions.setBorder(new EmptyBorder(8, 0, 0, 0));

        JButton open = UiKit.primaryButton("Open");
        open.setBorder(new EmptyBorder(8, 12, 8, 12));
        open.setFont(open.getFont().deriveFont(Font.BOLD, 11.8f));

        // ✅ you can decide: open wizard or open planner (for now planner is better)
        open.addActionListener(e -> this.router.show(ScreenKeys.PLANNER_2D));

        JPanel iconRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        iconRow.setOpaque(false);

        JButton duplicate = UiKit.iconButton("⧉");
        duplicate.setToolTipText("Duplicate");

        JButton edit = UiKit.iconButton("✎");
        edit.setToolTipText("Edit");

        JButton delete = UiKit.iconButton("🗑");
        delete.setToolTipText("Delete");
        delete.setForeground(UiKit.DANGER);

        iconRow.add(duplicate);
        iconRow.add(edit);
        iconRow.add(delete);

        actions.add(open, BorderLayout.CENTER);
        actions.add(iconRow, BorderLayout.EAST);

        JPanel body = new JPanel();
        body.setOpaque(false);
        body.setLayout(new BorderLayout());
        body.add(info, BorderLayout.CENTER);
        body.add(actions, BorderLayout.SOUTH);

        card.add(img, BorderLayout.NORTH);
        card.add(body, BorderLayout.CENTER);

        // hover
        card.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) {
                card.setFill(new Color(0xFAFAFB));
                card.repaint();
            }
            @Override public void mouseExited(MouseEvent e) {
                card.setFill(UiKit.WHITE);
                card.repaint();
            }
        });

        return card;
    }

    private JComponent linkChip(String text) {
        JLabel l = new JLabel(text);
        l.setOpaque(true);
        l.setBackground(new Color(0xEEF2FF));
        l.setForeground(UiKit.PRIMARY_DARK);
        l.setFont(l.getFont().deriveFont(Font.BOLD, 11.5f));
        l.setBorder(new EmptyBorder(6, 10, 6, 10));
        return l;
    }
}
