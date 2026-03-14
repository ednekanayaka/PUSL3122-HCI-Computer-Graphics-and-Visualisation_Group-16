package com.roomviz.screens;

import com.roomviz.app.AppFrame;
import com.roomviz.app.Router;
import com.roomviz.app.ScreenKeys;
import com.roomviz.data.AppState;
import com.roomviz.model.Design;
import com.roomviz.model.RoomSpec;
import com.roomviz.ui.Mini2DPreviewPanel;
import com.roomviz.ui.Mini3DPreviewPanel;
import com.roomviz.ui.UiKit;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Design Details Page (Step 1 wired)
 * - Opens when user clicks a design in the Design Library.
 * - Shows real data from repository via AppState.
 *
 * ✅ Preview behaviour (UPDATED):
 * - 2D View = REAL mini preview (Mini2DPreviewPanel)
 * - 3D View = REAL mini preview (Mini3DPreviewPanel)
 * - Toggle switches the preview card (CardLayout)
 *
 * ✅ Completed:
 * - 2D/3D toggle is wired (CardLayout preview switch)
 * - Fullscreen works (opens preview dialog)
 * - Download works (exports active preview PNG)
 */
public class DesignDetailsPage extends JPanel {

    private final Router router;
    private final AppState appState;

    // ===== Palette (aligned with UiKit) =====
    private static final Color TEXT = UiKit.TEXT;
    private static final Color MUTED = UiKit.MUTED;
    private static final Color BORDER = UiKit.BORDER;
    private static final Color WHITE = UiKit.WHITE;

    private static final Color PRIMARY = UiKit.PRIMARY;
    private static final Color PRIMARY_DARK = new Color(0x6D28D9);

    private static final Color DANGER = UiKit.DANGER;
    private static final Color WARNING_BG = new Color(0xFFFBEB);
    private static final Color WARNING_BORDER = new Color(0xFDE68A);
    private static final Color WARNING_TEXT = new Color(0x92400E);

    // ===== Dynamic refs =====
    private JLabel headerTitle;
    private JLabel headerSubtitle;

    private JLabel customerName;
    private JLabel customerEmail;
    private JLabel roomTypeVal;
    private JLabel roomSizeVal;
    private JLabel roomShapeVal;
    private JLabel themeText;

    private JTextArea notesArea;

    // Preview refs
    private JPanel previewCardHost;
    private CardLayout previewCardLayout;
    private String previewMode = "2D"; // "2D" or "3D"

    // Real preview components
    private Mini2DPreviewPanel mini2DPanel;
    private Mini3DPreviewPanel mini3DPanel;

    // Root layout refs
    private JPanel rootContent;
    private JPanel mainGridHost;

    private JLabel createdVal;
    private JLabel modifiedVal;
    private AvatarCircle avatarCircle;

    public DesignDetailsPage(AppFrame frame, Router router) {
        this(frame, router, null);
    }

    public DesignDetailsPage(AppFrame frame, Router router, AppState appState) {
        this.router = router;
        this.appState = appState;

        setLayout(new BorderLayout());
        setOpaque(false);

        rootContent = new JPanel();
        rootContent.setOpaque(false);
        rootContent.setLayout(new BoxLayout(rootContent, BoxLayout.Y_AXIS));
        rootContent.setBorder(new EmptyBorder(16, 18, 16, 18));

        rootContent.add(buildHeader(frame));
        rootContent.add(Box.createVerticalStrut(14));

        mainGridHost = new JPanel(new BorderLayout());
        mainGridHost.setOpaque(false);
        rootContent.add(mainGridHost);

        JScrollPane scroll = new JScrollPane(rootContent);
        scroll.setBorder(null);
        scroll.getViewport().setOpaque(false);
        scroll.setOpaque(false);
        scroll.getVerticalScrollBar().setUnitIncrement(16);

        add(scroll, BorderLayout.CENTER);

        if (router != null) {
            router.addListener(key -> {
                if (ScreenKeys.DESIGN_DETAILS.equals(key)) {
                    refresh(frame);
                }
            });
        }

        refresh(frame);
    }

    /* ===================== Refresh ===================== */

    private void refresh(AppFrame frame) {
        if (appState == null || appState.getCurrentDesignId() == null) {
            mainGridHost.removeAll();
            customerName = null;

            mainGridHost.add(emptyState(
                    "No design selected",
                    "Go back to the Design Library and open a design."
            ), BorderLayout.CENTER);

            if (headerTitle != null) headerTitle.setText("Design Details");
            if (headerSubtitle != null) headerSubtitle.setText("—");

            revalidate();
            repaint();
            return;
        }

        Design d = appState.getRepo().getById(appState.getCurrentDesignId());
        if (d == null) {
            mainGridHost.removeAll();
            customerName = null;

            mainGridHost.add(emptyState(
                    "Design not found",
                    "This design may have been deleted. Go back to the Design Library."
            ), BorderLayout.CENTER);

            if (headerTitle != null) headerTitle.setText("Design Details");
            if (headerSubtitle != null) headerSubtitle.setText("—");

            revalidate();
            repaint();
            return;
        }

        if (mainGridHost.getComponentCount() == 0 || customerName == null) {
            mainGridHost.removeAll();
            mainGridHost.add(buildMainGrid(), BorderLayout.CENTER);
            mainGridHost.revalidate();
        }

        bindDesignToUI(d);
        revalidate();
        repaint();
    }

    private void bindDesignToUI(Design d) {
        String title = safe(d.getDesignName(), "Untitled Design");
        if (headerTitle != null) headerTitle.setText(title);
        if (headerSubtitle != null) headerSubtitle.setText("Last edited " + timeAgoLabel(d.getLastUpdatedEpochMs()));

        String cust = safe(d.getCustomerName(), "Unknown Customer");
        if (customerName != null) customerName.setText(cust);
        if (customerEmail != null) customerEmail.setText(generateEmailHint(cust));
        if (avatarCircle != null) avatarCircle.setLetter(firstLetter(cust));

        RoomSpec spec = d.getRoomSpec();

        String unit = (spec == null) ? "" : safe(spec.getUnit(), "");
        String shape = (spec == null) ? "-" : safe(spec.getShape(), "-");
        String scheme = (spec == null) ? "-" : safe(spec.getColorScheme(), "-");
        String type = (spec == null) ? "-" : safe(spec.getRoomType(), "Room");

        if (roomTypeVal != null) roomTypeVal.setText(type);
        if (roomShapeVal != null) roomShapeVal.setText(shape);

        if (roomSizeVal != null) {
            if (spec == null || spec.getWidth() <= 0 || spec.getLength() <= 0) {
                roomSizeVal.setText("-");
            } else {
                double w = spec.getWidth();
                double l = spec.getLength();
                double area = w * l;
                roomSizeVal.setText(
                        formatNumber(w) + " " + unit + " × " + formatNumber(l) + " " + unit +
                                " (" + formatNumber(area) + " sq " + unit + ")"
                );
            }
        }

        if (themeText != null) themeText.setText(scheme);

        if (createdVal != null) createdVal.setText(dateLabel(d.getCreatedAtEpochMs()));
        if (modifiedVal != null) modifiedVal.setText(dateLabel(d.getLastUpdatedEpochMs()));

        String notes = safe(d.getNotes(), "");
        if (notesArea != null) {
            if (notes.isEmpty()) {
                notesArea.setText("No notes added yet.");
                notesArea.setForeground(MUTED);
            } else {
                notesArea.setText(notes);
                notesArea.setForeground(TEXT);
            }
        }

        // ✅ Bind real previews
        if (mini2DPanel != null) {
            mini2DPanel.setDesign(d);
        }
        if (mini3DPanel != null) {
            mini3DPanel.setDesign(d);
        }
    }

    /* ===================== Header ===================== */

    private JComponent buildHeader(AppFrame frame) {
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        left.setOpaque(false);

        JButton back = UiKit.iconButton("←");
        back.setToolTipText("Back to Design Library");
        back.addActionListener(e -> {
            if (router != null) router.show(ScreenKeys.DESIGN_LIBRARY);
        });

        JPanel titleBox = new JPanel();
        titleBox.setOpaque(false);
        titleBox.setLayout(new BoxLayout(titleBox, BoxLayout.Y_AXIS));

        headerTitle = new JLabel("Design Details");
        headerTitle.setForeground(TEXT);
        headerTitle.setFont(UiKit.scaled(headerTitle, Font.BOLD, 1.30f));

        headerSubtitle = new JLabel("—");
        headerSubtitle.setForeground(MUTED);
        headerSubtitle.setFont(UiKit.scaled(headerSubtitle, Font.PLAIN, 0.90f));

        titleBox.add(headerTitle);
        titleBox.add(Box.createVerticalStrut(3));
        titleBox.add(headerSubtitle);

        left.add(back);
        left.add(titleBox);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        actions.setOpaque(false);

        JButton duplicate = ghostButton("Duplicate");
        JButton edit = ghostButton("Edit Details");
        JButton delete = dangerOutlineButton("Delete");

        duplicate.addActionListener(e -> {
            if (appState == null || appState.getCurrentDesignId() == null) return;

            Design copy = appState.getRepo().duplicate(appState.getCurrentDesignId());
            if (copy != null) {
                appState.setCurrentDesignId(copy.getId());
                JOptionPane.showMessageDialog(this, "Duplicated ✅");
                refresh(frame);
            }
        });

        edit.addActionListener(e -> {
            if (appState == null || appState.getCurrentDesignId() == null) return;
            Design d = appState.getRepo().getById(appState.getCurrentDesignId());
            if (d == null) return;

            new EditDesignDialog(
                    SwingUtilities.getWindowAncestor(this),
                    d,
                    appState,
                    () -> refresh(frame)
            ).setVisible(true);
        });

        delete.addActionListener(e -> {
            if (appState == null || appState.getCurrentDesignId() == null) return;
            Design d = appState.getRepo().getById(appState.getCurrentDesignId());
            String current = d == null ? "this design" : safe(d.getDesignName(), "this design");

            int ok = JOptionPane.showConfirmDialog(
                    this,
                    "Delete \"" + current + "\"?\nThis cannot be undone.",
                    "Confirm delete",
                    JOptionPane.YES_NO_OPTION
            );
            if (ok == JOptionPane.YES_OPTION) {
                appState.getRepo().delete(appState.getCurrentDesignId());
                appState.setCurrentDesignId(null);
                if (router != null) router.show(ScreenKeys.DESIGN_LIBRARY);
            }
        });

        actions.add(duplicate);
        actions.add(edit);
        actions.add(delete);

        header.add(left, BorderLayout.WEST);
        header.add(actions, BorderLayout.EAST);
        return header;
    }

    /* ===================== Main grid ===================== */

    private JComponent buildMainGrid() {
        JPanel grid = new JPanel(new GridBagLayout());
        grid.setOpaque(false);

        GridBagConstraints gc = new GridBagConstraints();
        gc.gridy = 0;
        gc.fill = GridBagConstraints.BOTH;
        gc.weighty = 1;

        JPanel leftCol = new JPanel();
        leftCol.setOpaque(false);
        leftCol.setLayout(new BoxLayout(leftCol, BoxLayout.Y_AXIS));

        leftCol.add(buildPreviewCard());
        leftCol.add(Box.createVerticalStrut(14));
        leftCol.add(buildNotesCard());

        JPanel rightCol = new JPanel();
        rightCol.setOpaque(false);
        rightCol.setLayout(new BoxLayout(rightCol, BoxLayout.Y_AXIS));

        rightCol.add(buildInfoCard());
        rightCol.add(Box.createVerticalStrut(14));
        rightCol.add(buildTagsCard());

        gc.gridx = 0;
        gc.weightx = 1.0;
        gc.insets = new Insets(0, 0, 0, 0);
        grid.add(leftCol, gc);

        gc.gridx = 1;
        gc.weightx = 0.38;
        gc.insets = new Insets(0, 14, 0, 0);
        grid.add(rightCol, gc);

        return grid;
    }

    /* ===================== Preview Card ===================== */

    private JComponent buildPreviewCard() {
        RoundedPanel card = cardPanel();
        card.setLayout(new BorderLayout());
        card.setBorder(new EmptyBorder(14, 14, 14, 14));

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);

        JLabel title = sectionTitle("Design Preview");

        JPanel toggle = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        toggle.setOpaque(false);

        JToggleButton v2d = toggleChip("2D View", true);
        JToggleButton v3d = toggleChip("3D View", false);

        ButtonGroup bg = new ButtonGroup();
        bg.add(v2d);
        bg.add(v3d);

        v2d.addActionListener(e -> switchPreviewMode("2D"));
        v3d.addActionListener(e -> switchPreviewMode("3D"));

        toggle.add(v2d);
        toggle.add(v3d);

        top.add(title, BorderLayout.WEST);
        top.add(toggle, BorderLayout.EAST);

        // Host
        previewCardLayout = new CardLayout();
        previewCardHost = new JPanel(previewCardLayout);
        previewCardHost.setOpaque(false);

        JPanel wrap2D = new JPanel(new BorderLayout());
        wrap2D.setOpaque(false);
        wrap2D.setBorder(new EmptyBorder(12, 0, 12, 0));
        wrap2D.add(build2DPreview(), BorderLayout.CENTER);

        JPanel wrap3D = new JPanel(new BorderLayout());
        wrap3D.setOpaque(false);
        wrap3D.setBorder(new EmptyBorder(12, 0, 12, 0));
        wrap3D.add(build3DPreview(), BorderLayout.CENTER);

        previewCardHost.add(wrap2D, "2D");
        previewCardHost.add(wrap3D, "3D");
        previewCardLayout.show(previewCardHost, "2D");

        // Bottom buttons
        JPanel bottom = new JPanel(new GridLayout(1, 2, 10, 0));
        bottom.setOpaque(false);

        JButton edit2d = primaryPill("Edit in 2D");
        edit2d.addActionListener(e -> {
            if (router != null) router.show(ScreenKeys.PLANNER_2D);
        });

        JButton view3d = primaryGradientPill("View in 3D");
        view3d.addActionListener(e -> {
            if (router != null) router.show(ScreenKeys.VIEW_3D);
        });

        bottom.add(edit2d);
        bottom.add(view3d);

        card.add(top, BorderLayout.NORTH);
        card.add(previewCardHost, BorderLayout.CENTER);
        card.add(bottom, BorderLayout.SOUTH);
        return card;
    }

    private void switchPreviewMode(String mode) {
        previewMode = mode;
        if (previewCardLayout != null && previewCardHost != null) {
            previewCardLayout.show(previewCardHost, mode);
        }
    }

    /**
     * Overlay icons (fullscreen + download) stacked above a content panel.
     * IMPORTANT: Pass the inner content panel, not something you also add elsewhere.
     */
    private JComponent buildPreviewStack(JComponent innerContent) {
        JPanel previewIcons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        previewIcons.setOpaque(false);

        JButton full = iconPill("⛶");
        full.setToolTipText("Fullscreen");
        JButton dl = iconPill("⬇");
        dl.setToolTipText("Download PNG");

        full.addActionListener(e -> openPreviewFullscreen());
        dl.addActionListener(e -> exportPreviewPng());

        previewIcons.add(full);
        previewIcons.add(dl);

        JPanel overlay = new JPanel(new BorderLayout());
        overlay.setOpaque(false);
        overlay.add(previewIcons, BorderLayout.NORTH);

        JPanel stack = new JPanel();
        stack.setOpaque(false);
        stack.setLayout(new OverlayLayout(stack));

        stack.add(overlay);
        stack.add(innerContent);

        return stack;
    }

    // ✅ REAL 2D preview (Mini2DPreviewPanel)
    private JComponent build2DPreview() {
        RoundedPanel inner = new RoundedPanel(16, new Color(0xF9FAFB));
        inner.setBorderPaint(BORDER);
        inner.setLayout(new BorderLayout());
        inner.setBorder(new EmptyBorder(10, 10, 10, 10));

        mini2DPanel = new Mini2DPreviewPanel();
        mini2DPanel.setOpaque(true);
        mini2DPanel.setBackground(new Color(0xF9FAFB));
        mini2DPanel.setPreferredSize(new Dimension(860, 320));

        JPanel hint = new JPanel(new BorderLayout());
        hint.setOpaque(false);
        hint.setBorder(new EmptyBorder(0, 2, 8, 2));

        JLabel t = new JLabel("2D Preview");
        t.setForeground(TEXT);
        t.setFont(UiKit.scaled(t, Font.BOLD, 0.95f));

        JLabel s = new JLabel("Read-only snapshot. Use “Edit in 2D” to modify.");
        s.setForeground(MUTED);
        s.setFont(UiKit.scaled(s, Font.PLAIN, 0.90f));

        JPanel hintLeft = new JPanel();
        hintLeft.setOpaque(false);
        hintLeft.setLayout(new BoxLayout(hintLeft, BoxLayout.Y_AXIS));
        hintLeft.add(t);
        hintLeft.add(Box.createVerticalStrut(2));
        hintLeft.add(s);

        hint.add(hintLeft, BorderLayout.WEST);

        inner.add(hint, BorderLayout.NORTH);
        inner.add(mini2DPanel, BorderLayout.CENTER);

        return buildPreviewStack(inner);
    }

    // ✅ REAL 3D preview (Mini3DPreviewPanel)
    private JComponent build3DPreview() {
        RoundedPanel inner = new RoundedPanel(16, new Color(0x0B1220));
        inner.setBorderPaint(BORDER);
        inner.setLayout(new BorderLayout());
        inner.setBorder(new EmptyBorder(10, 10, 10, 10));

        mini3DPanel = new Mini3DPreviewPanel();
        mini3DPanel.setPreferredSize(new Dimension(860, 320));

        JPanel hint = new JPanel(new BorderLayout());
        hint.setOpaque(false);
        hint.setBorder(new EmptyBorder(0, 2, 8, 2));

        JLabel t = new JLabel("3D Preview");
        t.setForeground(new Color(255, 255, 255, 230));
        t.setFont(UiKit.scaled(t, Font.BOLD, 0.95f));

        JLabel s = new JLabel("Read-only isometric preview. Use “View in 3D” for tools.");
        s.setForeground(new Color(255, 255, 255, 150));
        s.setFont(UiKit.scaled(s, Font.PLAIN, 0.90f));

        JPanel hintLeft = new JPanel();
        hintLeft.setOpaque(false);
        hintLeft.setLayout(new BoxLayout(hintLeft, BoxLayout.Y_AXIS));
        hintLeft.add(t);
        hintLeft.add(Box.createVerticalStrut(2));
        hintLeft.add(s);

        hint.add(hintLeft, BorderLayout.WEST);

        inner.add(hint, BorderLayout.NORTH);
        inner.add(mini3DPanel, BorderLayout.CENTER);

        return buildPreviewStack(inner);
    }

    private JComponent getActivePreviewComponent() {
        if ("3D".equals(previewMode)) return (mini3DPanel != null) ? mini3DPanel : previewCardHost;
        return (mini2DPanel != null) ? mini2DPanel : previewCardHost;
    }

    /* ===================== Fullscreen + Export ===================== */

    private void openPreviewFullscreen() {
        if (appState == null || appState.getCurrentDesignId() == null) return;

        Design d = appState.getRepo().getById(appState.getCurrentDesignId());
        if (d == null) return;

        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(this), "Preview", Dialog.ModalityType.MODELESS);
        dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        dialog.setLayout(new BorderLayout());
        dialog.getContentPane().setBackground(UiKit.WHITE);

        JPanel content = new JPanel(new BorderLayout());
        content.setOpaque(true);
        content.setBackground(UiKit.WHITE);
        content.setBorder(new EmptyBorder(16, 16, 16, 16));

        JLabel title = new JLabel(safe(d.getDesignName(), "Design Preview") + "  •  " + previewMode + " View");
        title.setForeground(TEXT);
        title.setFont(UiKit.scaled(title, Font.BOLD, 1.05f));

        JButton close = ghostButton("Close");
        close.addActionListener(e -> dialog.dispose());

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(true);
        top.setBackground(UiKit.WHITE);
        top.setBorder(new CompoundBorder(new LineBorder(BORDER, 1), new EmptyBorder(10, 12, 10, 12)));
        top.add(title, BorderLayout.WEST);
        top.add(close, BorderLayout.EAST);

        JComponent preview;
        if ("3D".equals(previewMode)) {
            Mini3DPreviewPanel p = new Mini3DPreviewPanel();
            p.setDesign(d);
            preview = p;
            content.setBackground(new Color(0x0B1220));
            p.setBorder(new EmptyBorder(10, 10, 10, 10));
        } else {
            Mini2DPreviewPanel p = new Mini2DPreviewPanel();
            p.setDesign(d);

            RoundedPanel wrap = new RoundedPanel(16, new Color(0xF9FAFB));
            wrap.setBorderPaint(BORDER);
            wrap.setLayout(new BorderLayout());
            wrap.setBorder(new EmptyBorder(12, 12, 12, 12));
            wrap.add(p, BorderLayout.CENTER);

            preview = wrap;
        }

        content.add(preview, BorderLayout.CENTER);

        dialog.add(top, BorderLayout.NORTH);
        dialog.add(content, BorderLayout.CENTER);

        dialog.setSize(980, 620);
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private void exportPreviewPng() {
        JComponent target = getActivePreviewComponent();
        if (target == null) return;

        BufferedImage img = renderComponentToImage(target);

        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Save Preview as PNG");
        chooser.setSelectedFile(new File("roomviz-preview-" + previewMode.toLowerCase(Locale.ENGLISH) + ".png"));

        int res = chooser.showSaveDialog(this);
        if (res != JFileChooser.APPROVE_OPTION) return;

        File f = chooser.getSelectedFile();
        if (f == null) return;

        String name = f.getName().toLowerCase(Locale.ENGLISH);
        if (!name.endsWith(".png")) {
            f = new File(f.getParentFile(), f.getName() + ".png");
        }

        try {
            ImageIO.write(img, "png", f);
            JOptionPane.showMessageDialog(this, "Saved ✅\n" + f.getAbsolutePath());
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Save failed: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private BufferedImage renderComponentToImage(JComponent c) {
        int w = Math.max(1, c.getWidth());
        int h = Math.max(1, c.getHeight());

        if (w <= 1 || h <= 1) {
            Dimension pref = c.getPreferredSize();
            if (pref != null) {
                w = Math.max(w, pref.width);
                h = Math.max(h, pref.height);
            }
            w = Math.max(w, 900);
            h = Math.max(h, 520);
        }

        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        if ("3D".equals(previewMode)) g2.setColor(new Color(0x0B1220));
        else g2.setColor(WHITE);

        g2.fillRect(0, 0, w, h);

        c.paint(g2);
        g2.dispose();
        return img;
    }

    /* ===================== Notes / Info / Tags ===================== */

    private JComponent buildNotesCard() {
        RoundedPanel card = cardPanel();
        card.setLayout(new BorderLayout());
        card.setBorder(new EmptyBorder(14, 14, 14, 14));

        card.add(sectionTitle("Designer Notes"), BorderLayout.NORTH);

        JPanel body = new JPanel();
        body.setOpaque(false);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBorder(new EmptyBorder(10, 0, 0, 0));

        notesArea = new JTextArea("No notes added yet.");
        notesArea.setWrapStyleWord(true);
        notesArea.setLineWrap(true);
        notesArea.setEditable(false);
        notesArea.setOpaque(false);
        notesArea.setForeground(MUTED);
        notesArea.setFont(UiKit.scaled(notesArea, Font.PLAIN, 0.98f));
        notesArea.setBorder(null);

        body.add(notesArea);
        body.add(Box.createVerticalStrut(10));

        JPanel warn = new JPanel(new BorderLayout());
        warn.setOpaque(true);
        warn.setBackground(WARNING_BG);
        warn.setBorder(new CompoundBorder(
                new LineBorder(WARNING_BORDER, 1, true),
                new EmptyBorder(10, 12, 10, 12)
        ));

        JLabel warnText = new JLabel("⚠  Tip: Add lighting notes to improve the final render mood.");
        warnText.setForeground(WARNING_TEXT);
        warnText.setFont(UiKit.scaled(warnText, Font.PLAIN, 0.90f));
        warn.add(warnText, BorderLayout.CENTER);

        body.add(warn);

        card.add(body, BorderLayout.CENTER);
        return card;
    }

    private JComponent buildInfoCard() {
        RoundedPanel card = cardPanel();
        card.setLayout(new BorderLayout());
        card.setBorder(new EmptyBorder(14, 14, 14, 14));

        card.add(sectionTitle("Design Information"), BorderLayout.NORTH);

        JPanel body = new JPanel();
        body.setOpaque(false);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBorder(new EmptyBorder(10, 0, 0, 0));

        JPanel cust = new JPanel(new BorderLayout());
        cust.setOpaque(false);
        cust.add(keyLabel("Customer"), BorderLayout.NORTH);

        JPanel custRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 6));
        custRow.setOpaque(false);

        avatarCircle = new AvatarCircle("D");
        customerName = new JLabel("—");
        customerName.setForeground(TEXT);
        customerName.setFont(UiKit.scaled(customerName, Font.BOLD, 0.98f));

        customerEmail = new JLabel("—");
        customerEmail.setForeground(MUTED);
        customerEmail.setFont(UiKit.scaled(customerEmail, Font.PLAIN, 0.90f));

        JPanel nm = new JPanel();
        nm.setOpaque(false);
        nm.setLayout(new BoxLayout(nm, BoxLayout.Y_AXIS));
        nm.add(customerName);
        nm.add(customerEmail);

        custRow.add(avatarCircle);
        custRow.add(nm);
        cust.add(custRow, BorderLayout.CENTER);

        body.add(cust);
        body.add(divider());

        roomTypeVal = new JLabel("—");
        roomSizeVal = new JLabel("—");
        roomShapeVal = new JLabel("—");

        body.add(kv("Room Type", roomTypeVal));
        body.add(divider());
        body.add(kv("Room Size", roomSizeVal));
        body.add(divider());
        body.add(kv("Room Shape", roomShapeVal));
        body.add(divider());

        JPanel theme = new JPanel(new BorderLayout());
        theme.setOpaque(false);
        theme.add(keyLabel("Colour Theme"), BorderLayout.NORTH);

        JPanel sw = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        sw.setOpaque(false);
        sw.add(colorDot(new Color(0xE5E7EB)));
        sw.add(colorDot(new Color(0x111827)));
        sw.add(colorDot(new Color(0xFDE68A)));

        themeText = new JLabel("—");
        themeText.setForeground(MUTED);
        themeText.setFont(UiKit.scaled(themeText, Font.PLAIN, 0.90f));
        sw.add(themeText);

        theme.add(sw, BorderLayout.CENTER);

        body.add(theme);
        body.add(divider());

        createdVal = new JLabel("—");
        modifiedVal = new JLabel("—");

        body.add(kv("Created", createdVal));
        body.add(divider());
        body.add(kv("Last Modified", modifiedVal));

        card.add(body, BorderLayout.CENTER);
        return card;
    }

    private JComponent kv(String k, JLabel valueLabel) {
        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(false);

        JLabel key = keyLabel(k);
        valueLabel.setForeground(TEXT);
        valueLabel.setFont(UiKit.scaled(valueLabel, Font.PLAIN, 0.96f));

        row.add(key, BorderLayout.NORTH);
        row.add(valueLabel, BorderLayout.CENTER);
        row.setBorder(new EmptyBorder(2, 0, 2, 0));
        return row;
    }

    private JComponent buildTagsCard() {
        RoundedPanel card = cardPanel();
        card.setLayout(new BorderLayout());
        card.setBorder(new EmptyBorder(14, 14, 14, 14));

        card.add(sectionTitle("Tags"), BorderLayout.NORTH);

        JPanel body = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 10));
        body.setOpaque(false);

        body.add(UiKit.chipPrimary("Modern"));
        body.add(UiKit.chip("Minimalist"));
        body.add(UiKit.chip("Neutral"));

        JLabel add = new JLabel("+ Add Tag");
        add.setForeground(PRIMARY);
        add.setFont(UiKit.scaled(add, Font.BOLD, 0.90f));
        add.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        add.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                JOptionPane.showMessageDialog(DesignDetailsPage.this, "Tags can be persisted in a later step.");
            }
        });
        body.add(add);

        card.add(body, BorderLayout.CENTER);
        return card;
    }

    /* ===================== Empty state ===================== */

    private JComponent emptyState(String title, String subtitle) {
        RoundedPanel card = cardPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(new EmptyBorder(22, 22, 22, 22));

        JLabel t = new JLabel(title);
        t.setForeground(TEXT);
        t.setFont(UiKit.scaled(t, Font.BOLD, 1.05f));

        JLabel s = new JLabel(subtitle);
        s.setForeground(MUTED);
        s.setFont(UiKit.scaled(s, Font.PLAIN, 0.98f));
        s.setBorder(new EmptyBorder(8, 0, 0, 0));

        card.add(t);
        card.add(s);
        return card;
    }

    /* ===================== UI helpers ===================== */

    private RoundedPanel cardPanel() {
        RoundedPanel p = new RoundedPanel(16, WHITE);
        p.setBorderPaint(BORDER);
        p.setOpaque(false);
        return p;
    }

    private JLabel sectionTitle(String text) {
        JLabel l = new JLabel(text);
        l.setForeground(TEXT);
        l.setFont(UiKit.scaled(l, Font.BOLD, 1.00f));
        return l;
    }

    private JLabel keyLabel(String text) {
        JLabel l = new JLabel(text);
        l.setForeground(MUTED);
        l.setFont(UiKit.scaled(l, Font.PLAIN, 0.90f));
        return l;
    }

    private JComponent divider() {
        JPanel d = new JPanel();
        d.setOpaque(true);
        d.setBackground(BORDER);
        d.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        d.setPreferredSize(new Dimension(0, 1));
        d.setBorder(new EmptyBorder(8, 0, 8, 0));
        return d;
    }

    private JToggleButton toggleChip(String text, boolean selected) {
        JToggleButton b = new JToggleButton(text, selected);
        b.setFocusPainted(false);
        b.setBorderPainted(false);
        b.setContentAreaFilled(true);
        b.setOpaque(true);
        b.setFont(UiKit.scaled(b, Font.BOLD, 0.88f));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        styleToggle(b);
        b.addChangeListener(e -> styleToggle(b));
        return b;
    }

    private void styleToggle(AbstractButton b) {
        boolean sel = b.isSelected();
        b.setBackground(sel ? PRIMARY : new Color(0xF3F4F6));
        b.setForeground(sel ? Color.WHITE : TEXT);
        b.setBorder(new EmptyBorder(6, 10, 6, 10));
    }

    private JButton ghostButton(String text) {
        JButton b = new JButton(text);
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setBackground(WHITE);
        b.setForeground(TEXT);
        b.setFont(UiKit.scaled(b, Font.BOLD, 0.90f));
        b.setBorder(new CompoundBorder(
                new LineBorder(BORDER, 1, true),
                new EmptyBorder(8, 12, 8, 12)
        ));
        return b;
    }

    private JButton dangerOutlineButton(String text) {
        JButton b = new JButton(text);
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setBackground(WHITE);
        b.setForeground(DANGER);
        b.setFont(UiKit.scaled(b, Font.BOLD, 0.90f));
        b.setBorder(new CompoundBorder(
                new LineBorder(new Color(0xFCA5A5), 1, true),
                new EmptyBorder(8, 12, 8, 12)
        ));
        return b;
    }

    private JButton primaryPill(String text) {
        JButton b = new JButton(text);
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setBackground(PRIMARY);
        b.setForeground(Color.WHITE);
        b.setFont(UiKit.scaled(b, Font.BOLD, 0.92f));
        b.setBorder(new EmptyBorder(10, 14, 10, 14));
        return b;
    }

    private JButton primaryGradientPill(String text) {
        JButton b = new JButton(text);
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setBackground(PRIMARY_DARK);
        b.setForeground(Color.WHITE);
        b.setFont(UiKit.scaled(b, Font.BOLD, 0.92f));
        b.setBorder(new EmptyBorder(10, 14, 10, 14));
        return b;
    }

    private JButton iconPill(String text) {
        JButton b = UiKit.iconButton(text);
        b.setBackground(new Color(0xF9FAFB));
        b.setBorder(new CompoundBorder(
                new LineBorder(BORDER, 1, true),
                new EmptyBorder(6, 8, 6, 8)
        ));
        return b;
    }

    private JComponent colorDot(Color c) {
        JPanel p = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(c);
                g2.fillOval(0, 0, getWidth() - 1, getHeight() - 1);
                g2.setColor(BORDER);
                g2.drawOval(0, 0, getWidth() - 1, getHeight() - 1);
                g2.dispose();
            }
        };
        p.setOpaque(false);
        p.setPreferredSize(new Dimension(18, 18));
        return p;
    }

    /* ===================== Utilities ===================== */

    private static String safe(String s, String fallback) {
        if (s == null) return fallback;
        s = s.trim();
        return s.isEmpty() ? fallback : s;
    }

    private static String firstLetter(String name) {
        if (name == null) return "D";
        String t = name.trim();
        if (t.isEmpty()) return "D";
        return ("" + Character.toUpperCase(t.charAt(0)));
    }

    private static String generateEmailHint(String name) {
        if (name == null) return "—";
        String n = name.trim().toLowerCase(Locale.ENGLISH).replaceAll("[^a-z0-9]+", ".");
        if (n.isEmpty()) return "—";
        return n + "@example.com";
    }

    private static String formatNumber(double v) {
        if (Math.abs(v - Math.round(v)) < 1e-9) return String.valueOf((long) Math.round(v));
        return String.format(Locale.ENGLISH, "%.2f", v);
    }

    private static String dateLabel(long epochMs) {
        if (epochMs <= 0) return "—";
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("d MMM yyyy", Locale.ENGLISH);
            return sdf.format(new Date(epochMs));
        } catch (Exception e) {
            return "—";
        }
    }

    private static String timeAgoLabel(long epochMs) {
        if (epochMs <= 0) return "—";
        long diff = System.currentTimeMillis() - epochMs;
        if (diff < 60_000) return "just now";
        long mins = diff / 60_000;
        if (mins < 60) return mins + " min ago";
        long hrs = mins / 60;
        if (hrs < 24) return hrs + " hr ago";
        long days = hrs / 24;
        if (days < 14) return days + " days ago";
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("d MMM yyyy", Locale.ENGLISH);
            return sdf.format(new Date(epochMs));
        } catch (Exception e) {
            return "earlier";
        }
    }

    /* ===================== Small custom components ===================== */

    private static class AvatarCircle extends JPanel {
        private String letter;

        AvatarCircle(String letter) {
            this.letter = letter;
            setPreferredSize(new Dimension(34, 34));
            setOpaque(false);
        }

        public void setLetter(String l) {
            this.letter = (l == null || l.trim().isEmpty()) ? "D" : l.trim().substring(0, 1).toUpperCase(Locale.ENGLISH);
            repaint();
        }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth();
            int h = getHeight();
            g2.setColor(new Color(0xE5E7EB));
            g2.fillOval(0, 0, w - 1, h - 1);
            g2.setColor(new Color(0x9CA3AF));
            g2.drawOval(0, 0, w - 1, h - 1);

            g2.setColor(TEXT);
            Font f = getFont().deriveFont(Font.BOLD, 12.5f);
            g2.setFont(f);
            FontMetrics fm = g2.getFontMetrics();
            String draw = (letter == null ? "D" : letter);
            int tx = (w - fm.stringWidth(draw)) / 2;
            int ty = (h + fm.getAscent() - fm.getDescent()) / 2;
            g2.drawString(draw, tx, ty);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    private static class RoundedPanel extends JPanel {
        private final int radius;
        private final Color fill;
        private Color border;

        RoundedPanel(int radius, Color fill) {
            this.radius = radius;
            this.fill = fill;
            setOpaque(false);
        }

        public void setBorderPaint(Color c) { this.border = c; }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth();
            int h = getHeight();
            g2.setColor(fill != null ? fill : getBackground());
            g2.fillRoundRect(0, 0, w, h, radius, radius);
            if (border != null) {
                g2.setColor(border);
                g2.drawRoundRect(0, 0, w - 1, h - 1, radius, radius);
            }
            g2.dispose();
            super.paintComponent(g);
        }
    }
}
