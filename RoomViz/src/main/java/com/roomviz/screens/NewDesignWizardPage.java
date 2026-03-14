package com.roomviz.screens;

import com.roomviz.app.AppFrame;
import com.roomviz.app.Router;
import com.roomviz.app.ScreenKeys;
import com.roomviz.data.AppState;
import com.roomviz.data.SettingsRepository;
import com.roomviz.model.Design;
import com.roomviz.model.DesignStatus;
import com.roomviz.model.RoomSpec;
import com.roomviz.ui.UiKit;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.util.UUID;

/**
 * New Design Wizard page (Figma-like)
 * Step 1: now creates/saves a Design into repository and navigates to Design Details.
 */
public class NewDesignWizardPage extends JPanel {

    private static final int STEPS = 4;
    private int currentStep = 1;

    private final JLabel stepTitle = new JLabel("Design Information");
    private final JLabel stepSubtitle = new JLabel("Let's start with the basic details of your design project");

    private final JPanel formHost = new JPanel(new CardLayout());
    private final Stepper stepper = new Stepper();

    private final JButton backBtn = UiKit.ghostButton("←  Back");
    private final JButton nextBtn = UiKit.primaryButton("Next  →");

    // Step 1 fields
    private final JTextField designName = new JTextField();
    private final JTextField customerName = new JTextField();
    private final JTextArea notes = new JTextArea(5, 40);

    // Step 2 fields
    private final JTextField roomWidth = new JTextField();
    private final JTextField roomLength = new JTextField();
    private final JComboBox<String> unit = new JComboBox<>(new String[]{"ft", "cm", "m"});

    // Step 3 fields
    private final JComboBox<String> roomShape = new JComboBox<>(new String[]{"Rectangular", "Square", "L-Shape", "Custom"});

    // Step 4 fields
    private final JComboBox<String> colorScheme = new JComboBox<>(new String[]{"Neutral Tones", "Warm Tones", "Cool Tones", "Monochrome", "Pastels"});

    // ✅ Step 1 shared app state (repo + current selection)
    private final AppState appState;
    private final SettingsRepository settingsRepo;

    // Step 3 (L-Shape extra fields)
    private final JTextField lCutWidth = new JTextField();
    private final JTextField lCutLength = new JTextField();
    private final JPanel lShapeDimsWrap = new JPanel();

    // placeholders (must match what you set below)
    private static final String PH_DESIGN = "e.g., Modern Living Room Redesign";
    private static final String PH_CUSTOMER = "e.g., John Smith";
    private static final String PH_WIDTH = "e.g., 24";
    private static final String PH_LENGTH = "e.g., 18";
    private static final String PH_NOTES = "Add any additional notes or requirements for this design...";
    private static final String PH_LCUT_W = "e.g., 8";
    private static final String PH_LCUT_L = "e.g., 6";

    // Existing constructor kept (fallback/no persistence)
    public NewDesignWizardPage(AppFrame frame, Router router) {
        this(frame, router, null, null);
    }

    // New constructor used by ShellScreen
    public NewDesignWizardPage(AppFrame frame, Router router, AppState appState) {
        this(frame, router, appState, null);
    }

    public NewDesignWizardPage(AppFrame frame, Router router, AppState appState, SettingsRepository settingsRepo) {
        this.appState = appState;
        this.settingsRepo = settingsRepo;

        setLayout(new BorderLayout());
        setOpaque(false);

        JPanel page = new JPanel();
        page.setOpaque(false);
        page.setLayout(new BoxLayout(page, BoxLayout.Y_AXIS));
        page.setBorder(new EmptyBorder(18, 24, 18, 24));

        page.add(topHeader(frame, router));
        page.add(Box.createVerticalStrut(14));

        page.add(stepperWrap());
        page.add(Box.createVerticalStrut(18));

        page.add(centerCard());
        page.add(Box.createVerticalStrut(18));

        page.add(bottomNav(router));

        JScrollPane scroller = new JScrollPane(page);
        scroller.setBorder(BorderFactory.createEmptyBorder());
        scroller.getViewport().setOpaque(true);
        scroller.getViewport().setBackground(UiKit.BG);
        scroller.setOpaque(false);
        scroller.getVerticalScrollBar().setUnitIncrement(18);

        add(scroller, BorderLayout.CENTER);

        // ✅ Apply default units from Settings
        applyDefaultUnitFromSettings();

        setStep(1);
    }

    private boolean isHighContrast() {
        return UiKit.TEXT.equals(Color.BLACK) && UiKit.BORDER.equals(Color.BLACK);
    }

    private Color placeholderColor() {
        return isHighContrast() ? UiKit.TEXT : new Color(0x9CA3AF);
    }

    private Color subMuted() {
        // sometimes UiKit.MUTED is enough; but this keeps strong readability in HC
        return isHighContrast() ? UiKit.TEXT : UiKit.MUTED;
    }

    /* ========================= Header ========================= */

    private JComponent topHeader(AppFrame frame, Router router) {
        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(false);

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        left.setOpaque(false);

        UiKit.RoundedPanel icon = new UiKit.RoundedPanel(10, isHighContrast() ? UiKit.WHITE : new Color(0xEEF2FF));
        icon.setBorderPaint(isHighContrast() ? UiKit.BORDER : new Color(0xC7D2FE));
        icon.setPreferredSize(new Dimension(32, 32));
        icon.setLayout(new GridBagLayout());

        JLabel i = new JLabel("✦");
        i.setForeground(isHighContrast() ? UiKit.TEXT : UiKit.PRIMARY_DARK);
        i.setFont(UiKit.scaled(i, Font.BOLD, 1.05f));
        icon.add(i);

        JPanel text = new JPanel();
        text.setOpaque(false);
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("New Design Wizard");
        title.setForeground(UiKit.TEXT);
        title.setFont(UiKit.scaled(title, Font.BOLD, 1.10f));

        JLabel sub = new JLabel("Create a custom room design");
        sub.setForeground(subMuted());
        sub.setFont(UiKit.scaled(sub, Font.PLAIN, 0.95f));
        sub.setBorder(new EmptyBorder(2, 0, 0, 0));

        text.add(title);
        text.add(sub);

        left.add(icon);
        left.add(text);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        right.setOpaque(false);

        JButton saveDraft = UiKit.ghostButton("💾  Save as Draft");
        saveDraft.setFont(UiKit.scaled(saveDraft, Font.PLAIN, 1.00f));
        saveDraft.addActionListener(e -> onSaveDraft(router));

        JButton close = UiKit.iconButton("✕");
        close.setToolTipText("Close wizard");
        close.addActionListener(e -> router.show(ScreenKeys.DESIGN_LIBRARY));

        right.add(saveDraft);
        right.add(close);

        row.add(left, BorderLayout.WEST);
        row.add(right, BorderLayout.EAST);
        return row;
    }

    /* ========================= Stepper wrapper ========================= */

    private JComponent stepperWrap() {
        UiKit.RoundedPanel wrap = new UiKit.RoundedPanel(14, UiKit.WHITE);
        wrap.setBorderPaint(UiKit.BORDER);
        wrap.setLayout(new BorderLayout());
        wrap.setBorder(new EmptyBorder(14, 14, 14, 14));
        wrap.add(stepper, BorderLayout.CENTER);
        return wrap;
    }

    /* ========================= Center Card ========================= */

    private JComponent centerCard() {
        UiKit.RoundedPanel card = new UiKit.RoundedPanel(14, UiKit.WHITE);
        card.setBorderPaint(UiKit.BORDER);
        card.setLayout(new BorderLayout());
        card.setBorder(new EmptyBorder(18, 18, 18, 18));

        JPanel header = new JPanel();
        header.setOpaque(false);
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));

        stepTitle.setForeground(UiKit.TEXT);
        stepTitle.setFont(UiKit.scaled(stepTitle, Font.BOLD, 1.20f));

        stepSubtitle.setForeground(subMuted());
        stepSubtitle.setFont(UiKit.scaled(stepSubtitle, Font.PLAIN, 0.98f));
        stepSubtitle.setBorder(new EmptyBorder(6, 0, 0, 0));

        header.add(stepTitle);
        header.add(stepSubtitle);

        formHost.setOpaque(false);
        formHost.add(step1Panel(), "1");
        formHost.add(step2Panel(), "2");
        formHost.add(step3Panel(), "3");
        formHost.add(step4Panel(), "4");

        JPanel body = new JPanel(new BorderLayout());
        body.setOpaque(false);
        body.setBorder(new EmptyBorder(14, 0, 0, 0));
        body.add(formHost, BorderLayout.CENTER);

        card.add(header, BorderLayout.NORTH);
        card.add(body, BorderLayout.CENTER);
        return card;
    }

    /* ========================= Bottom Nav ========================= */

    private JComponent bottomNav(Router router) {
        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(false);

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        left.setOpaque(false);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        right.setOpaque(false);

        styleNavButton(backBtn);
        styleNavButton(nextBtn);

        backBtn.addActionListener(e -> setStep(currentStep - 1));
        nextBtn.addActionListener(e -> {
            if (currentStep < STEPS) {
                if (validateStep(currentStep)) setStep(currentStep + 1);
            } else {
                onFinish(router);
            }
        });

        left.add(backBtn);
        right.add(nextBtn);

        row.add(left, BorderLayout.WEST);
        row.add(right, BorderLayout.EAST);
        return row;
    }

    /* ========================= Steps ========================= */

    private JComponent step1Panel() {
        JPanel p = formStack();

        p.add(fieldBlock("Design Name *", hint(PH_DESIGN, designName)));
        p.add(Box.createVerticalStrut(12));

        p.add(fieldBlock("Customer Name *", hint(PH_CUSTOMER, customerName)));
        p.add(Box.createVerticalStrut(12));

        p.add(textAreaBlock("Project Notes (Optional)", PH_NOTES, notes));

        return p;
    }

    private JComponent step2Panel() {
        JPanel p = formStack();

        JPanel row = new JPanel(new GridLayout(1, 3, 12, 0));
        row.setOpaque(false);

        row.add(fieldBlock("Room Width *", hint(PH_WIDTH, roomWidth)));
        row.add(fieldBlock("Room Length *", hint(PH_LENGTH, roomLength)));

        JPanel unitWrap = new JPanel();
        unitWrap.setOpaque(false);
        unitWrap.setLayout(new BoxLayout(unitWrap, BoxLayout.Y_AXIS));
        unitWrap.add(label("Units *"));
        unitWrap.add(Box.createVerticalStrut(6));
        UiKit.styleDropdown(unit);
        unitWrap.add(unit);

        row.add(unitWrap);

        p.add(row);
        p.add(Box.createVerticalStrut(10));

        JLabel info = new JLabel("Tip: Use the same units you will use in the 2D Planner.");
        info.setForeground(subMuted());
        info.setFont(UiKit.scaled(info, Font.PLAIN, 0.90f));
        p.add(info);

        return p;
    }

    private JComponent step3Panel() {
        JPanel p = formStack();

        UiKit.styleDropdown(roomShape);
        p.add(dropdownBlock("Room Shape *", roomShape));
        p.add(Box.createVerticalStrut(12));

        // --- L-shape dimensions block (shown only if L-Shape selected) ---
        lShapeDimsWrap.setOpaque(false);
        lShapeDimsWrap.setLayout(new BoxLayout(lShapeDimsWrap, BoxLayout.Y_AXIS));

        JLabel help = new JLabel("If you selected L-Shape, enter the cut-out size (top-right corner).");
        help.setForeground(subMuted());
        help.setFont(UiKit.scaled(help, Font.PLAIN, 0.90f));
        lShapeDimsWrap.add(help);
        lShapeDimsWrap.add(Box.createVerticalStrut(10));

        JPanel row = new JPanel(new GridLayout(1, 2, 12, 0));
        row.setOpaque(false);

        row.add(fieldBlock("Cut-out Width *", hint(PH_LCUT_W, lCutWidth)));
        row.add(fieldBlock("Cut-out Length *", hint(PH_LCUT_L, lCutLength)));

        lShapeDimsWrap.add(row);

        JLabel tip = new JLabel("Example: Outer = 24×18, Cut-out = 8×6 → L-Shape floor.");
        tip.setForeground(subMuted());
        tip.setFont(UiKit.scaled(tip, Font.PLAIN, 0.88f));
        tip.setBorder(new EmptyBorder(8, 0, 0, 0));
        lShapeDimsWrap.add(tip);

        p.add(lShapeDimsWrap);

        // Default visibility
        updateLShapeVisibility();

        // When user changes shape, show/hide inputs
        roomShape.addActionListener(e -> updateLShapeVisibility());

        return p;
    }

    private JComponent step4Panel() {
        JPanel p = formStack();

        UiKit.styleDropdown(colorScheme);
        p.add(dropdownBlock("Color Scheme *", colorScheme));
        p.add(Box.createVerticalStrut(12));

        JPanel palettes = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        palettes.setOpaque(false);

        // These are decorative; keep colors but make borders HC-friendly
        palettes.add(colorDot(new Color(0x111827)));
        palettes.add(colorDot(new Color(0xF5F3FF)));
        palettes.add(colorDot(new Color(0xEDE9FE)));
        palettes.add(colorDot(new Color(0xF3F4F6)));
        palettes.add(colorDot(new Color(0xD1D5DB)));

        JLabel prev = new JLabel("Preview");
        prev.setForeground(UiKit.TEXT);
        prev.setFont(UiKit.scaled(prev, Font.BOLD, 1.00f));

        p.add(prev);
        p.add(Box.createVerticalStrut(8));
        p.add(palettes);

        return p;
    }

    /* ========================= Step handling ========================= */

    private void setStep(int step) {
        if (step < 1) step = 1;
        if (step > STEPS) step = STEPS;

        currentStep = step;
        stepper.setActive(step);

        switch (step) {
            case 1 -> {
                stepTitle.setText("Design Information");
                stepSubtitle.setText("Let's start with the basic details of your design project");
                nextBtn.setText("Next  →");
            }
            case 2 -> {
                stepTitle.setText("Dimensions");
                stepSubtitle.setText("Enter the room measurements to set up your design");
                nextBtn.setText("Next  →");
            }
            case 3 -> {
                stepTitle.setText("Room Shape");
                stepSubtitle.setText("Choose a room shape that matches the customer space");
                nextBtn.setText("Next  →");
            }
            case 4 -> {
                stepTitle.setText("Color Scheme");
                stepSubtitle.setText("Pick a starting color theme for your design");
                nextBtn.setText("Finish  ✓");
            }
        }

        CardLayout cl = (CardLayout) formHost.getLayout();
        cl.show(formHost, String.valueOf(step));

        backBtn.setEnabled(step != 1);

        revalidate();
        repaint();
    }

    private void updateLShapeVisibility() {
        String shape = (String) roomShape.getSelectedItem();
        boolean isL = "L-Shape".equalsIgnoreCase(shape);

        lShapeDimsWrap.setVisible(isL);

        if (!isL) {
            lCutWidth.setText("");
            lCutLength.setText("");
        }

        revalidate();
        repaint();
    }

    /* ========================= Step 1 actions ========================= */

    private void onSaveDraft(Router router) {
        if (appState == null) {
            JOptionPane.showMessageDialog(this,
                    "AppState not available (wire this screen with the 3-arg constructor).",
                    "Cannot save", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // allow saving drafts with only minimal info
        String dName = cleanField(designName, PH_DESIGN);
        String cName = cleanField(customerName, PH_CUSTOMER);

        if (dName.isEmpty() || cName.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Please fill Design Name and Customer Name to save a draft.",
                    "Missing fields", JOptionPane.WARNING_MESSAGE);
            return;
        }

        Design d = buildDesignObject(true);
        if (d == null) return;

        d.setStatus(DesignStatus.DRAFT);

        appState.getRepo().upsert(d);
        appState.setCurrentDesignId(d.getId());

        JOptionPane.showMessageDialog(this,
                "Draft saved ✅",
                "Saved", JOptionPane.INFORMATION_MESSAGE);

        router.show(ScreenKeys.DESIGN_DETAILS);
    }

    private void onFinish(Router router) {
        if (appState == null) {
            JOptionPane.showMessageDialog(this,
                    "AppState not available (wire this screen with the 3-arg constructor).",
                    "Cannot finish", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (!validateStep(1) || !validateStep(2) || !validateStep(3) || !validateStep(4)) {
            return;
        }

        Design d = buildDesignObject(false);
        if (d == null) return;

        d.setStatus(DesignStatus.IN_PROGRESS);

        appState.getRepo().upsert(d);
        appState.setCurrentDesignId(d.getId());

        JOptionPane.showMessageDialog(this,
                "Design created ✅",
                "Success", JOptionPane.INFORMATION_MESSAGE);

        router.show(ScreenKeys.PLANNER_2D);
    }

    private boolean validateStep(int step) {
        if (step == 1) {
            String dn = cleanField(designName, PH_DESIGN);
            String cn = cleanField(customerName, PH_CUSTOMER);
            if (dn.isEmpty() || cn.isEmpty()) {
                JOptionPane.showMessageDialog(this,
                        "Please fill Design Name and Customer Name.",
                        "Missing fields", JOptionPane.WARNING_MESSAGE);
                return false;
            }
        }
        if (step == 2) {
            String w = cleanField(roomWidth, PH_WIDTH);
            String l = cleanField(roomLength, PH_LENGTH);
            if (w.isEmpty() || l.isEmpty()) {
                JOptionPane.showMessageDialog(this,
                        "Please enter Room Width and Room Length.",
                        "Missing fields", JOptionPane.WARNING_MESSAGE);
                return false;
            }
            try {
                double wd = Double.parseDouble(w);
                double ld = Double.parseDouble(l);
                if (wd <= 0 || ld <= 0) throw new NumberFormatException();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this,
                        "Room Width/Length must be positive numbers.",
                        "Invalid numbers", JOptionPane.WARNING_MESSAGE);
                return false;
            }
        }
        if (step == 3) {
            String shape = (String) roomShape.getSelectedItem();
            if ("L-Shape".equalsIgnoreCase(shape)) {
                String cw = cleanField(lCutWidth, PH_LCUT_W);
                String cl = cleanField(lCutLength, PH_LCUT_L);

                if (cw.isEmpty() || cl.isEmpty()) {
                    JOptionPane.showMessageDialog(this,
                            "Please enter L-Shape cut-out Width and Length.",
                            "Missing fields", JOptionPane.WARNING_MESSAGE);
                    return false;
                }

                try {
                    double cutW = Double.parseDouble(cw);
                    double cutL = Double.parseDouble(cl);
                    if (cutW <= 0 || cutL <= 0) throw new NumberFormatException();

                    double outerW = Double.parseDouble(cleanField(roomWidth, PH_WIDTH));
                    double outerL = Double.parseDouble(cleanField(roomLength, PH_LENGTH));

                    if (cutW >= outerW || cutL >= outerL) {
                        JOptionPane.showMessageDialog(this,
                                "Cut-out must be smaller than the outer room dimensions.",
                                "Invalid L-Shape dimensions", JOptionPane.WARNING_MESSAGE);
                        return false;
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(this,
                            "L-Shape cut-out Width/Length must be valid positive numbers.",
                            "Invalid numbers", JOptionPane.WARNING_MESSAGE);
                    return false;
                }
            }
        }

        return true;
    }

    private Design buildDesignObject(boolean draftMode) {
        String dName = cleanField(designName, PH_DESIGN);
        String cName = cleanField(customerName, PH_CUSTOMER);
        String note = cleanArea(notes, PH_NOTES);

        // for draft, width/length can be empty (but if provided, validate)
        double w = 0;
        double l = 0;

        String wStr = cleanField(roomWidth, PH_WIDTH);
        String lStr = cleanField(roomLength, PH_LENGTH);

        if (!wStr.isEmpty() && !lStr.isEmpty()) {
            try {
                w = Double.parseDouble(wStr);
                l = Double.parseDouble(lStr);
            } catch (Exception ignored) {
                if (!draftMode) {
                    JOptionPane.showMessageDialog(this,
                            "Room Width/Length must be numbers.",
                            "Invalid numbers", JOptionPane.WARNING_MESSAGE);
                    return null;
                }
            }
        } else if (!draftMode) {
            JOptionPane.showMessageDialog(this,
                    "Please enter Room Width and Room Length.",
                    "Missing fields", JOptionPane.WARNING_MESSAGE);
            return null;
        }

        String u = (String) unit.getSelectedItem();
        String shape = (String) roomShape.getSelectedItem();
        String scheme = (String) colorScheme.getSelectedItem();

        RoomSpec spec = new RoomSpec(w, l, u, shape, scheme);

        // Save L-Shape extra values (only if L-Shape)
        if ("L-Shape".equalsIgnoreCase(shape)) {
            String cw = cleanField(lCutWidth, PH_LCUT_W);
            String cl = cleanField(lCutLength, PH_LCUT_L);

            try {
                double cutW = Double.parseDouble(cw);
                double cutL = Double.parseDouble(cl);
                spec.setLCutWidth(cutW);
                spec.setLCutLength(cutL);
                spec.setLCorner("TOP_RIGHT"); // default
            } catch (Exception ignored) {
                // draft mode might allow empty, but finish validation prevents this anyway
            }
        }

        Design d = new Design();
        d.setId(UUID.randomUUID().toString());
        d.setDesignName(dName);
        d.setCustomerName(cName);
        d.setNotes(note);
        d.setRoomSpec(spec);

        long now = System.currentTimeMillis();
        d.setCreatedAtEpochMs(now);
        d.setLastUpdatedEpochMs(now);

        return d;
    }

    private String cleanField(JTextField tf, String placeholder) {
        String v = tf.getText();
        if (v == null) return "";
        v = v.trim();
        if (v.equals(placeholder)) return "";
        return v;
    }

    private String cleanArea(JTextArea ta, String placeholder) {
        String v = ta.getText();
        if (v == null) return "";
        v = v.trim();
        if (v.equals(placeholder)) return "";
        return v;
    }

    /* ========================= Small UI helpers ========================= */

    private JPanel formStack() {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        return p;
    }

    private JLabel label(String t) {
        JLabel l = new JLabel(t);
        l.setForeground(UiKit.TEXT);
        l.setFont(UiKit.scaled(l, Font.PLAIN, 0.95f));
        return l;
    }

    private JComponent hint(String placeholder, JTextField field) {
        styleTextField(field);
        setPlaceholder(field, placeholder);
        return field;
    }

    private JComponent fieldBlock(String label, JComponent field) {
        JPanel b = new JPanel();
        b.setOpaque(false);
        b.setLayout(new BoxLayout(b, BoxLayout.Y_AXIS));
        b.add(label(label));
        b.add(Box.createVerticalStrut(6));
        b.add(field);
        return b;
    }

    private JComponent dropdownBlock(String label, JComponent dropdown) {
        JPanel b = new JPanel();
        b.setOpaque(false);
        b.setLayout(new BoxLayout(b, BoxLayout.Y_AXIS));
        b.add(label(label));
        b.add(Box.createVerticalStrut(6));
        b.add(dropdown);
        return b;
    }

    private JComponent textAreaBlock(String label, String placeholder, JTextArea area) {
        JPanel b = new JPanel();
        b.setOpaque(false);
        b.setLayout(new BoxLayout(b, BoxLayout.Y_AXIS));
        b.add(label(label));
        b.add(Box.createVerticalStrut(6));

        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setFont(UiKit.scaled(area, Font.PLAIN, 1.00f));
        area.setBorder(new EmptyBorder(10, 12, 10, 12));
        area.setForeground(UiKit.TEXT);
        area.setBackground(UiKit.WHITE);
        area.setCaretColor(UiKit.TEXT);

        JScrollPane sp = new JScrollPane(area);
        sp.setBorder(new LineBorder(UiKit.BORDER, 1, true));
        sp.setPreferredSize(new Dimension(0, 120));

        area.setText(placeholder);
        area.setForeground(placeholderColor());
        area.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override public void focusGained(java.awt.event.FocusEvent e) {
                if (placeholder.equals(area.getText())) {
                    area.setText("");
                    area.setForeground(UiKit.TEXT);
                }
            }
            @Override public void focusLost(java.awt.event.FocusEvent e) {
                if (area.getText().trim().isEmpty()) {
                    area.setText(placeholder);
                    area.setForeground(placeholderColor());
                }
            }
        });

        b.add(sp);

        JLabel helper = new JLabel("Optional: Add special requirements, preferences, or important details");
        helper.setForeground(subMuted());
        helper.setFont(UiKit.scaled(helper, Font.PLAIN, 0.88f));
        helper.setBorder(new EmptyBorder(6, 0, 0, 0));
        b.add(helper);

        return b;
    }

    private void styleTextField(JTextField tf) {
        tf.setFont(UiKit.scaled(tf, Font.PLAIN, 1.00f));
        tf.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(UiKit.BORDER, 1, true),
                new EmptyBorder(10, 12, 10, 12)
        ));
        tf.setBackground(UiKit.WHITE);
        tf.setForeground(UiKit.TEXT);
        tf.setCaretColor(UiKit.TEXT);
        tf.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
    }

    private void setPlaceholder(JTextField tf, String placeholder) {
        tf.setText(placeholder);
        tf.setForeground(placeholderColor());

        tf.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override public void focusGained(java.awt.event.FocusEvent e) {
                if (placeholder.equals(tf.getText())) {
                    tf.setText("");
                    tf.setForeground(UiKit.TEXT);
                }
            }
            @Override public void focusLost(java.awt.event.FocusEvent e) {
                if (tf.getText().trim().isEmpty()) {
                    tf.setText(placeholder);
                    tf.setForeground(placeholderColor());
                }
            }
        });
    }

    private void styleNavButton(JButton b) {
        b.setFont(UiKit.scaled(b, Font.BOLD, 1.00f));
        b.setBorder(new EmptyBorder(10, 14, 10, 14));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    private JComponent colorDot(Color c) {
        UiKit.RoundedPanel dot = new UiKit.RoundedPanel(999, c);
        dot.setPreferredSize(new Dimension(18, 18));
        dot.setBorderPaint(isHighContrast() ? UiKit.BORDER : new Color(0xE5E7EB));
        return dot;
    }

    /* ========================= Stepper ========================= */

    private class Stepper extends JPanel {
        private int active = 1;

        Stepper() {
            setOpaque(false);
            setLayout(new GridLayout(1, STEPS, 12, 0));
            rebuild();
        }

        void setActive(int active) {
            this.active = active;
            rebuild();
        }

        private void rebuild() {
            removeAll();
            add(stepItem(1, "Design Info"));
            add(stepItem(2, "Dimensions"));
            add(stepItem(3, "Room Shape"));
            add(stepItem(4, "Color Scheme"));
            revalidate();
            repaint();
        }

        private JComponent stepItem(int number, String label) {
            JPanel p = new JPanel(new BorderLayout(10, 0));
            p.setOpaque(false);

            boolean done = number < active;
            boolean isActive = number == active;

            Color inactiveFill = isHighContrast() ? UiKit.WHITE : new Color(0xE5E7EB);
            Color circleFill = isActive ? UiKit.PRIMARY : inactiveFill;

            UiKit.RoundedPanel circle = new UiKit.RoundedPanel(999, circleFill);
            circle.setBorderPaint(isHighContrast() ? UiKit.BORDER : null);
            circle.setPreferredSize(new Dimension(28, 28));
            circle.setLayout(new GridBagLayout());

            JLabel n = new JLabel(done ? "✓" : String.valueOf(number));
            n.setForeground(isActive ? Color.WHITE : (isHighContrast() ? UiKit.TEXT : new Color(0x6B7280)));
            n.setFont(UiKit.scaled(n, Font.BOLD, 0.95f));
            circle.add(n);

            JLabel t = new JLabel(label);
            t.setForeground(isActive ? UiKit.TEXT : subMuted());
            t.setFont(UiKit.scaled(t, isActive ? Font.BOLD : Font.PLAIN, 0.95f));

            JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
            left.setOpaque(false);
            left.add(circle);

            p.add(left, BorderLayout.WEST);
            p.add(t, BorderLayout.CENTER);

            return p;
        }
    }

    private void applyDefaultUnitFromSettings() {
        if (settingsRepo == null) return;

        var s = settingsRepo.get();
        String code = (s == null) ? "cm" : s.getDefaultUnit();
        if (code == null) code = "cm";

        if ("ft".equalsIgnoreCase(code)) unit.setSelectedItem("ft");
        else if ("m".equalsIgnoreCase(code)) unit.setSelectedItem("m");
        else unit.setSelectedItem("cm");
    }
}
