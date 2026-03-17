package com.roomviz.screens;

import com.roomviz.app.AppFrame;
import com.roomviz.app.Router;
import com.roomviz.app.ScreenKeys;
import com.roomviz.data.AppState;
import com.roomviz.data.Session;
import com.roomviz.data.SettingsRepository;
import com.roomviz.data.UserRepository;
import com.roomviz.model.Design;
import com.roomviz.model.DesignStatus;
import com.roomviz.model.RoomSpec;
import com.roomviz.model.User;
import com.roomviz.ui.FontAwesome;
import com.roomviz.ui.UiKit;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.util.UUID;

/**
 * Step-by-step wizard for creating new designs.
 * Admin role allows design creation for customers.
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
    private final JTextField customerName = new JTextField(); // used as "Customer Email" for admin
    private final JTextArea notes = new JTextArea(5, 40);

    // Step 2 fields
    private final JTextField roomWidth = new JTextField();
    private final JTextField roomLength = new JTextField();
    private final JComboBox<String> unit = new JComboBox<>(new String[]{"ft", "m"});

    // Step 3 fields
    private final JComboBox<String> roomShape = new JComboBox<>(new String[]{"Rectangular", "Square", "L-Shape", "Custom"});

    // Step 4 fields
    private final JComboBox<String> colorScheme = new JComboBox<>(new String[]{"Neutral Tones", "Warm Tones", "Cool Tones", "Monochrome", "Pastels"});

    private final AppState appState;
    private final SettingsRepository settingsRepo;
    private final UserRepository userRepo;
    private final Session session;

    // Step 3 (L-Shape extra fields)
    private final JTextField lCutWidth = new JTextField();
    private final JTextField lCutLength = new JTextField();
    private final JPanel lShapeDimsWrap = new JPanel();

    // placeholders
    private static final String PH_DESIGN = "e.g., Modern Living Room Redesign";
    private static final String PH_CUSTOMER_EMAIL = "e.g., customer@email.com";
    private static final String PH_WIDTH = "e.g., 24";
    private static final String PH_LENGTH = "e.g., 18";
    private static final String PH_NOTES = "Add any additional notes or requirements for this design...";
    private static final String PH_LCUT_W = "e.g., 8";
    private static final String PH_LCUT_L = "e.g., 6";

    // Existing constructor kept (fallback/no persistence)
    public NewDesignWizardPage(AppFrame frame, Router router) {
        this(frame, router, null, null, null, null);
    }

    public NewDesignWizardPage(AppFrame frame, Router router, AppState appState) {
        this(frame, router, appState, null, null, null);
    }

    public NewDesignWizardPage(AppFrame frame, Router router, AppState appState, SettingsRepository settingsRepo) {
        this(frame, router, appState, settingsRepo, null, null);
    }

    // (admin ownership support)
    public NewDesignWizardPage(AppFrame frame, Router router, AppState appState,
                               SettingsRepository settingsRepo, UserRepository userRepo, Session session) {
        this.appState = appState;
        this.settingsRepo = settingsRepo;
        this.userRepo = userRepo;
        this.session = session;

        setLayout(new BorderLayout());
        setOpaque(false);

        // Block customers from creating designs
        if (session != null && session.isLoggedIn()) {
            User u = session.getCurrentUser();
            if (u != null && !u.isAdmin()) {
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(
                            this,
                            "Customers cannot create new designs.\nYou can only view designs assigned to your account.",
                            "View-only access",
                            JOptionPane.INFORMATION_MESSAGE
                    );
                    if (router != null) router.show(ScreenKeys.DESIGN_LIBRARY);
                });
            }
        }

        JPanel formCenterWrap = new JPanel(new GridBagLayout());
        formCenterWrap.setOpaque(false);

        JPanel page = new JPanel();
        page.setOpaque(false);
        page.setLayout(new BoxLayout(page, BoxLayout.Y_AXIS));
        page.setBorder(new EmptyBorder(18, 24, 18, 24));
        page.setPreferredSize(new Dimension(800, 700));
        page.setMaximumSize(new Dimension(800, Integer.MAX_VALUE));

        page.add(topHeader(frame, router));
        page.add(Box.createVerticalStrut(14));

        page.add(stepperWrap());
        page.add(Box.createVerticalStrut(18));

        page.add(centerCard());
        page.add(Box.createVerticalStrut(18));

        page.add(bottomNav(router));

        formCenterWrap.add(page, new GridBagConstraints());

        JScrollPane scroller = new JScrollPane(formCenterWrap);
        scroller.setBorder(BorderFactory.createEmptyBorder());
        scroller.getViewport().setOpaque(false);
        scroller.getViewport().setBackground(UiKit.BG);
        scroller.setOpaque(false);
        scroller.getVerticalScrollBar().setUnitIncrement(18);

        add(scroller, BorderLayout.CENTER);

        applyDefaultUnitFromSettings();
        setStep(1);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        if (!UiKit.isHighContrastMode() && !UiKit.isDarkBlueMode()) {
            int w = getWidth();
            int h = getHeight();
            
            // Base Gradient: Soft purple to soft cyan
            LinearGradientPaint lgp = new LinearGradientPaint(
                    0, 0, w, h,
                    new float[]{ 0.0f, 0.5f, 1.0f },
                    new Color[]{ new Color(223, 172, 255), new Color(210, 190, 250), new Color(130, 240, 240) }
            );
            g2.setPaint(lgp);
            g2.fillRect(0, 0, w, h);
            
            // Abstract wave 1
            g2.setPaint(new Color(255, 255, 255, 60));
            java.awt.geom.Path2D wave = new java.awt.geom.Path2D.Double();
            wave.moveTo(0, h * 0.4);
            wave.curveTo(w * 0.3, h * 0.6, w * 0.6, h * 0.2, w, h * 0.5);
            wave.lineTo(w, h);
            wave.lineTo(0, h);
            wave.closePath();
            g2.fill(wave);
            
            // Abstract wave 2
            g2.setPaint(new Color(255, 255, 255, 30));
            java.awt.geom.Path2D wave2 = new java.awt.geom.Path2D.Double();
            wave2.moveTo(0, h * 0.6);
            wave2.curveTo(w * 0.4, h * 0.8, w * 0.8, h * 0.3, w, h * 0.7);
            wave2.lineTo(w, h);
            wave2.lineTo(0, h);
            wave2.closePath();
            g2.fill(wave2);

        } else {
            g2.setColor(UiKit.BG);
            g2.fillRect(0, 0, getWidth(), getHeight());
        }
        g2.dispose();
    }

    private boolean isHighContrast() { return UiKit.isHighContrastMode(); }
    private Color placeholderColor() { return isHighContrast() ? UiKit.TEXT : UiKit.MUTED; }
    private Color subMuted() { return isHighContrast() ? UiKit.TEXT : UiKit.MUTED; }

    private boolean isAdmin() {
        return session != null && session.isLoggedIn() && session.getCurrentUser() != null && session.getCurrentUser().isAdmin();
    }

    // --- Header ---

    private JComponent topHeader(AppFrame frame, Router router) {
        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(false);

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        left.setOpaque(false);

        UiKit.RoundedPanel icon = new UiKit.RoundedPanel(10, UiKit.ICON_BG);
        icon.setBorderPaint(isHighContrast() ? UiKit.BORDER : new Color(0xC7D2FE));
        icon.setPreferredSize(new Dimension(32, 32));
        icon.setLayout(new GridBagLayout());

        JLabel i = new JLabel(FontAwesome.STAR);
        i.setForeground(isHighContrast() ? UiKit.TEXT : UiKit.PRIMARY_DARK);
        i.setFont(FontAwesome.solid(13f));
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

        // Admin-only controls
        if (isAdmin()) {
            JButton saveDraft = UiKit.ghostButton(FontAwesome.FLOPPY_DISK + "  Save as Draft");
            saveDraft.setFont(FontAwesome.solid(13f));
            saveDraft.addActionListener(e -> onSaveDraft(router));

            JButton importDesigns = UiKit.ghostButton("Import");
            importDesigns.setFont(UiKit.scaled(importDesigns, Font.PLAIN, 1.00f));
            importDesigns.setToolTipText("Import RoomViz export JSON");
            importDesigns.addActionListener(e -> onImportData(router));

            right.add(saveDraft);
            right.add(importDesigns);
        }

        row.add(left, BorderLayout.WEST);
        row.add(right, BorderLayout.EAST);
        return row;
    }

    private JComponent stepperWrap() {
        UiKit.RoundedPanel wrap = new UiKit.RoundedPanel(14, UiKit.WHITE);
        wrap.setBorderPaint(UiKit.BORDER);
        wrap.setLayout(new BorderLayout());
        wrap.setBorder(new EmptyBorder(14, 14, 14, 14));
        wrap.add(stepper, BorderLayout.CENTER);
        return wrap;
    }

    private JComponent centerCard() {
        UiKit.RoundedPanel card = new UiKit.RoundedPanel(14, UiKit.WHITE);
        card.setBorderPaint(UiKit.BORDER);
        card.setLayout(new BorderLayout());
        card.setBorder(new EmptyBorder(18, 18, 18, 18));

        JPanel header = new JPanel();
        header.setOpaque(false);
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));

        stepTitle.setForeground(UiKit.TEXT);
        stepTitle.setFont(UiKit.scaled(stepTitle, Font.BOLD, 1.30f));

        stepSubtitle.setForeground(subMuted());
        stepSubtitle.setFont(UiKit.scaled(stepSubtitle, Font.PLAIN, 1.00f));
        stepSubtitle.setBorder(new EmptyBorder(8, 0, 0, 0));

        header.add(stepTitle);
        header.add(stepSubtitle);

        formHost.setOpaque(false);
        formHost.add(step1Panel(), "1");
        formHost.add(step2Panel(), "2");
        formHost.add(step3Panel(), "3");
        formHost.add(step4Panel(), "4");

        JPanel body = new JPanel(new BorderLayout());
        body.setOpaque(false);
        body.setBorder(new EmptyBorder(24, 0, 0, 0));
        body.add(formHost, BorderLayout.CENTER);

        card.add(header, BorderLayout.NORTH);
        card.add(body, BorderLayout.CENTER);
        return card;
    }

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

    // --- Steps ---

    private JComponent step1Panel() {
        JPanel p = formStack();

        p.add(fieldBlock("Design Name *", hint(PH_DESIGN, designName)));
        p.add(Box.createVerticalStrut(12));

        // Admin must assign to a real customer account (by email)
        if (isAdmin()) {
            p.add(fieldBlock("Customer Email *", hint(PH_CUSTOMER_EMAIL, customerName)));
            p.add(Box.createVerticalStrut(12));

            JLabel help = new JLabel("This customer will be able to view this design when they log in.");
            help.setForeground(subMuted());
            help.setFont(UiKit.scaled(help, Font.PLAIN, 0.90f));
            p.add(help);
            p.add(Box.createVerticalStrut(12));
        }

        p.add(textAreaBlock("Project Notes", PH_NOTES, notes));
        return p;
    }

    private JComponent step2Panel() {
        JPanel p = formStack();

        p.add(fieldBlock("Room Width *", hint(PH_WIDTH, roomWidth)));
        p.add(Box.createVerticalStrut(12));

        p.add(fieldBlock("Room Length *", hint(PH_LENGTH, roomLength)));
        p.add(Box.createVerticalStrut(12));

        p.add(dropdownBlock("Units *", unit));
        p.add(Box.createVerticalStrut(18));

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
        p.add(Box.createVerticalStrut(20));

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

        updateLShapeVisibility();
        roomShape.addActionListener(e -> updateLShapeVisibility());

        return p;
    }

    private JComponent step4Panel() {
        JPanel p = formStack();

        UiKit.styleDropdown(colorScheme);
        p.add(dropdownBlock("Color Scheme *", colorScheme));
        p.add(Box.createVerticalStrut(20));

        return p;
    }

    // --- Step handling ---

    private void setStep(int step) {
        if (step < 1) step = 1;
        if (step > STEPS) step = STEPS;

        currentStep = step;
        stepper.setActive(step);

        if (nextBtn instanceof UiKit.RoundButton rb) {
            if (step == 4) rb.setGradient(new Color(0x6366F1), new Color(0x4338CA));
            else rb.setGradient(null, null);
        }

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

    // --- Actions ---

    private void onImportData(Router router) {
        if (!isAdmin()) {
            JOptionPane.showMessageDialog(this,
                    "Import is admin-only.",
                    "Access restricted", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (appState == null || appState.getRepo() == null) {
            JOptionPane.showMessageDialog(this,
                    "Design repository not available.",
                    "Cannot import", JOptionPane.WARNING_MESSAGE);
            return;
        }

        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Import RoomViz Designs");
        chooser.setFileFilter(new FileNameExtensionFilter("JSON files (*.json)", "json"));

        int result = chooser.showOpenDialog(this);
        if (result != JFileChooser.APPROVE_OPTION) return;

        java.io.File in = chooser.getSelectedFile();
        int importedCount = appState.getRepo().importFrom(in);

        if (importedCount <= 0) {
            JOptionPane.showMessageDialog(this,
                    "Could not import designs.\nPlease select a valid RoomViz export JSON file.",
                    "Import Failed", JOptionPane.WARNING_MESSAGE);
            return;
        }

        java.util.List<Design> all = appState.getRepo().getAllSortedByLastUpdatedDesc();
        if (!all.isEmpty()) appState.setCurrentDesignId(all.get(0).getId());

        JOptionPane.showMessageDialog(this,
                "Imported " + importedCount + " design(s) from:\n" + in.getAbsolutePath(),
                "Import Complete", JOptionPane.INFORMATION_MESSAGE);

        if (router != null) router.show(ScreenKeys.DESIGN_LIBRARY);
    }

    private void onSaveDraft(Router router) {
        if (!isAdmin()) {
            JOptionPane.showMessageDialog(this,
                    "Save Draft is admin-only.",
                    "Access restricted", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (appState == null) {
            JOptionPane.showMessageDialog(this,
                    "AppState not available (wire this screen with the correct constructor).",
                    "Cannot save", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int ownerId = resolveOwnerCustomerId();
        if (ownerId <= 0) return;

        String dName = cleanField(designName, PH_DESIGN);
        if (dName.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Please fill Design Name to save a draft.",
                    "Missing fields", JOptionPane.WARNING_MESSAGE);
            return;
        }

        Design d = buildDesignObject(true);
        if (d == null) return;

        d.setStatus(DesignStatus.DRAFT);

        boolean ok = appState.getRepo().upsertForOwner(d, ownerId);

        if (!ok) {
            JOptionPane.showMessageDialog(this, "Failed to save draft.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        appState.setCurrentDesignId(d.getId());

        JOptionPane.showMessageDialog(this,
                "Draft saved",
                "Saved", JOptionPane.INFORMATION_MESSAGE);

        resetForm();
        router.show(ScreenKeys.DESIGN_DETAILS);
    }

    private void onFinish(Router router) {
        if (appState == null) {
            JOptionPane.showMessageDialog(this,
                    "AppState not available (wire this screen with the correct constructor).",
                    "Cannot finish", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (!validateStep(1) || !validateStep(2) || !validateStep(3) || !validateStep(4)) return;

        Design d = buildDesignObject(false);
        if (d == null) return;

        d.setStatus(DesignStatus.IN_PROGRESS);

        boolean ok;
        if (isAdmin()) {
            int ownerId = resolveOwnerCustomerId();
            if (ownerId <= 0) return;
            ok = appState.getRepo().upsertForOwner(d, ownerId);
        } else {
            // customers should never get here, but keep safe fallback
            ok = appState.getRepo().upsert(d);
        }

        if (!ok) {
            JOptionPane.showMessageDialog(this, "Failed to create design.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        appState.setCurrentDesignId(d.getId());

        JOptionPane.showMessageDialog(this,
                "Design created",
                "Success", JOptionPane.INFORMATION_MESSAGE);

        resetForm();
        router.show(ScreenKeys.PLANNER_2D);
    }

    private void resetForm() {
        // Step 1 fields
        designName.setText(PH_DESIGN);
        designName.setForeground(placeholderColor());
        customerName.setText(PH_CUSTOMER_EMAIL);
        customerName.setForeground(placeholderColor());
        notes.setText(PH_NOTES);
        notes.setForeground(placeholderColor());

        // Step 2 fields
        roomWidth.setText(PH_WIDTH);
        roomWidth.setForeground(placeholderColor());
        roomLength.setText(PH_LENGTH);
        roomLength.setForeground(placeholderColor());
        unit.setSelectedIndex(0);

        // Step 3 fields
        roomShape.setSelectedIndex(0);
        lCutWidth.setText("");
        lCutLength.setText("");
        updateLShapeVisibility();

        // Step 4 fields
        colorScheme.setSelectedIndex(0);

        // Reset to step 1
        setStep(1);
    }

    /** Admin must assign to an existing customer account (email). */
    private int resolveOwnerCustomerId() {
        if (!isAdmin()) return -1;

        if (userRepo == null) {
            JOptionPane.showMessageDialog(this,
                    "User repository not wired to this screen.",
                    "Error", JOptionPane.ERROR_MESSAGE);
            return -1;
        }

        String email = cleanField(customerName, PH_CUSTOMER_EMAIL).toLowerCase().trim();
        if (email.isEmpty() || !email.contains("@")) {
            JOptionPane.showMessageDialog(this,
                    "Please enter a valid Customer Email.\n(Example: customer@email.com)",
                    "Missing customer", JOptionPane.WARNING_MESSAGE);
            return -1;
        }

        User customer = userRepo.findByEmail(email);
        if (customer == null) {
            JOptionPane.showMessageDialog(this,
                    "No customer account found for:\n" + email + "\n\nAsk the customer to register first.",
                    "Customer not found", JOptionPane.WARNING_MESSAGE);
            return -1;
        }

        if (customer.isAdmin()) {
            JOptionPane.showMessageDialog(this,
                    "You entered an admin email.\nPlease enter a CUSTOMER email.",
                    "Invalid customer", JOptionPane.WARNING_MESSAGE);
            return -1;
        }

        return customer.getId();
    }

    private boolean validateStep(int step) {
        if (step == 1) {
            String dn = cleanField(designName, PH_DESIGN);
            if (dn.isEmpty()) {
                JOptionPane.showMessageDialog(this,
                        "Please fill Design Name.",
                        "Missing fields", JOptionPane.WARNING_MESSAGE);
                return false;
            }

            if (isAdmin()) {
                String ce = cleanField(customerName, PH_CUSTOMER_EMAIL);
                if (ce.isEmpty() || !ce.contains("@")) {
                    JOptionPane.showMessageDialog(this,
                            "Please enter a valid Customer Email.",
                            "Missing customer", JOptionPane.WARNING_MESSAGE);
                    return false;
                }
                if (resolveOwnerCustomerId() <= 0) return false;
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

            // Square validation (must be equal sides)
            if ("Square".equalsIgnoreCase(shape)) {
                try {
                    double outerW = Double.parseDouble(cleanField(roomWidth, PH_WIDTH));
                    double outerL = Double.parseDouble(cleanField(roomLength, PH_LENGTH));
                    if (Math.abs(outerW - outerL) > 1e-6) {
                        JOptionPane.showMessageDialog(this,
                                "Square rooms require Width and Length to be equal.\nPlease adjust the dimensions (Step 2).",
                                "Invalid Square dimensions", JOptionPane.WARNING_MESSAGE);
                        return false;
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(this,
                            "Please enter valid Width and Length first (Step 2).",
                            "Missing dimensions", JOptionPane.WARNING_MESSAGE);
                    return false;
                }
            }

            // L-Shape validation
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
        String note = cleanArea(notes, PH_NOTES);

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

        // Safety: if Square selected and both dims exist, force equal sides
        if ("Square".equalsIgnoreCase(shape) && w > 0 && l > 0) {
            if (Math.abs(w - l) > 1e-6) {
                if (!draftMode) {
                    JOptionPane.showMessageDialog(this,
                            "Square rooms require Width and Length to be equal.\nPlease adjust dimensions.",
                            "Invalid Square dimensions", JOptionPane.WARNING_MESSAGE);
                    return null;
                } else {
                    l = w;
                }
            }
        }

        RoomSpec spec = new RoomSpec(w, l, u, shape, scheme);

        if ("L-Shape".equalsIgnoreCase(shape)) {
            String cw = cleanField(lCutWidth, PH_LCUT_W);
            String cl = cleanField(lCutLength, PH_LCUT_L);

            try {
                double cutW = Double.parseDouble(cw);
                double cutL = Double.parseDouble(cl);
                spec.setLCutWidth(cutW);
                spec.setLCutLength(cutL);
                spec.setLCorner("TOP_RIGHT");
            } catch (Exception ignored) {
                // keep defaults for draft/incomplete
            }
        } else {
            spec.setLCutWidth(0);
            spec.setLCutLength(0);
            spec.setLCorner("TOP_RIGHT");
        }

        Design d = new Design();
        d.setId(UUID.randomUUID().toString());

        // Use setName() for 3D page compatibility
        d.setName(dName);

        // admin stores customer email; customer stores their name (but customers are blocked anyway)
        if (isAdmin()) {
            d.setCustomerName(cleanField(customerName, PH_CUSTOMER_EMAIL));
        } else if (session != null && session.getCurrentUser() != null) {
            d.setCustomerName(session.getCurrentUser().getFullName());
        } else {
            d.setCustomerName("");
        }

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

    private JComponent fieldBlock(String labelStr, JComponent field) {
        JPanel b = new JPanel(new GridBagLayout());
        b.setOpaque(false);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(0, 0, 0, 16);

        JLabel l = label(labelStr);
        l.setPreferredSize(new Dimension(160, 20));
        l.setMinimumSize(new Dimension(160, 20));

        gbc.weightx = 0;
        b.add(l, gbc);

        gbc.weightx = 1.0;
        gbc.insets = new Insets(0, 0, 0, 0);
        b.add(field, gbc);

        return b;
    }

    private JComponent dropdownBlock(String labelStr, JComboBox<?> dropdown) {
        UiKit.styleDropdown(dropdown);
        return fieldBlock(labelStr, dropdown);
    }

    private JComponent textAreaBlock(String labelStr, String placeholder, JTextArea area) {
        JPanel b = new JPanel(new GridBagLayout());
        b.setOpaque(false);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.insets = new Insets(8, 0, 0, 16);

        JLabel l = label(labelStr);
        l.setPreferredSize(new Dimension(160, 20));
        l.setMinimumSize(new Dimension(160, 20));

        gbc.gridx = 0;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        b.add(l, gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(0, 0, 0, 0);

        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setFont(UiKit.scaled(area, Font.PLAIN, 1.00f));
        area.setBorder(new EmptyBorder(10, 12, 10, 12));
        area.setForeground(UiKit.TEXT);
        area.setBackground(UiKit.WHITE);
        area.setCaretColor(UiKit.TEXT);
        area.setOpaque(true);

        JScrollPane sp = new JScrollPane(area);
        sp.setBorder(new LineBorder(UiKit.BORDER, 1, true));
        sp.setPreferredSize(new Dimension(200, 100));
        sp.setMinimumSize(new Dimension(200, 100));
        sp.setOpaque(true);
        sp.setBackground(UiKit.WHITE);

        b.add(sp, gbc);

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

    /* ========================= Stepper ========================= */

    private class Stepper extends JPanel {
        private int active = 1;

        Stepper() {
            setOpaque(false);
            // Render steps and horizontal lines between them
            setLayout(new GridLayout(1, (STEPS * 2) - 1, 12, 0));
            rebuild();
        }

        void setActive(int active) {
            this.active = active;
            rebuild();
        }

        private void rebuild() {
            removeAll();
            add(stepItem(1, "Design Info"));
            add(line(1));
            add(stepItem(2, "Dimensions"));
            add(line(2));
            add(stepItem(3, "Room Shape"));
            add(line(3));
            add(stepItem(4, "Color Scheme"));
            revalidate();
            repaint();
        }

        private JComponent line(int afterStep) {
            JPanel p = new JPanel(new GridBagLayout());
            p.setOpaque(false);

            boolean isPassed = afterStep < this.active;
            Color c = isPassed ? UiKit.PRIMARY : UiKit.BORDER;

            JPanel line = new JPanel();
            line.setBackground(c);
            line.setPreferredSize(new Dimension(40, 2));

            p.add(line);
            return p;
        }

        private JComponent stepItem(int number, String label) {
            JPanel p = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 0));
            p.setOpaque(false);

            boolean done = number < active;
            boolean isActive = number == active;

            Color circleFill = isActive ? UiKit.PRIMARY : (done ? UiKit.PRIMARY_DARK : UiKit.STEPPER_INACTIVE);

            UiKit.RoundedPanel circle = new UiKit.RoundedPanel(999, circleFill);
            circle.setBorderPaint(isHighContrast() ? UiKit.BORDER : (isActive ? UiKit.PRIMARY : null));
            circle.setPreferredSize(new Dimension(30, 30));
            circle.setLayout(new GridBagLayout());

            JLabel n = new JLabel(done ? FontAwesome.CHECK : String.valueOf(number));
            n.setForeground((isActive || done) ? Color.WHITE : (isHighContrast() ? UiKit.TEXT : UiKit.MUTED));
            n.setFont(done ? FontAwesome.solid(11f) : UiKit.scaled(n, Font.BOLD, 0.95f));
            circle.add(n);

            JLabel t = new JLabel(label);
            t.setForeground(isActive ? UiKit.TEXT : (done ? UiKit.PRIMARY_DARK : subMuted()));
            t.setFont(UiKit.scaled(t, isActive ? Font.BOLD : Font.PLAIN, 0.95f));

            p.add(circle);
            p.add(t);

            return p;
        }
    }

    private void applyDefaultUnitFromSettings() {
        if (settingsRepo == null) return;

        var s = settingsRepo.get();
        String code = (s == null) ? "ft" : s.getDefaultUnit();
        if (code == null) code = "ft";

        if ("ft".equalsIgnoreCase(code)) unit.setSelectedItem("ft");
        else if ("m".equalsIgnoreCase(code)) unit.setSelectedItem("m");
        else unit.setSelectedItem("ft");
    }
}
