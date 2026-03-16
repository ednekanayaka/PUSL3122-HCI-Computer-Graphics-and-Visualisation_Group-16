package com.roomviz.screens;

import com.roomviz.app.AppFrame;
import com.roomviz.app.Router;
import com.roomviz.app.ScreenKeys;
import com.roomviz.data.AppState;
import com.roomviz.data.Session;
import com.roomviz.data.SettingsRepository;
import com.roomviz.model.Design;
import com.roomviz.model.DesignStatus;
import com.roomviz.model.FurnitureItem;
import com.roomviz.model.FurnitureKind;
import com.roomviz.model.FurnitureTemplate;
import com.roomviz.model.User;
import com.roomviz.ui.RoomCanvas;
import com.roomviz.ui.FontAwesome;
import com.roomviz.ui.UiKit;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.*;

/**
 * 2D Planner page
 *
 *  (UI access control):
 * - CUSTOMER: read-only (no add/move/delete/resize/rotate/shading/layer changes)
 * - ADMIN: full access
 */
public class Planner2DPage extends JPanel {

    private final Router router;
    private final AppState appState;
    private final SettingsRepository settingsRepo;

    // role context
    private final Session session;

    private final RoomCanvas canvas;

    // Right panel controls
    private final JLabel selectedTitle = new JLabel("No selection");
    private final JTextField xField = new JTextField("0");
    private final JTextField yField = new JTextField("0");
    private final JSlider rotationSlider = new JSlider(0, 360, 0);
    private final JTextField rotField = new JTextField("0");

    private final JTextField wField = new JTextField("60");
    private final JTextField hField = new JTextField("40");
    private final JCheckBox lockAspect = new JCheckBox("Lock aspect ratio", true);

    private final JSlider shadingSlider = new JSlider(0, 100, 50);

    private final JButton deleteBtn = new JButton("Delete Item");
    private final JButton shadingToolsBtn = UiKit.ghostButton("Open Shading & Colour Tools");

    private final JLabel autosaved = new JLabel("● Not saved yet");
    private boolean programmaticUpdate = false;

    // Placed Items list UI
    private final JPanel placedItemsList = new JPanel();
    private JScrollPane placedItemsScroll;

    // Debounced autosave timer
    private final javax.swing.Timer autosaveTimer;

    // slider drag batching
    private boolean rotationDragging = false;
    private boolean shadingDragging = false;

    /* ========================= Furniture Library filter state ========================= */

    private enum FurnitureFilterTab { ALL, SEATING, TABLES }

    private FurnitureFilterTab activeFurnitureTab = FurnitureFilterTab.ALL;
    private JPanel chipsPanelRef;
    private JComponent libraryPanelRef;
    private JComponent propertiesPanelRef;
    private JSplitPane libraryCenterSplitRef;
    private JSplitPane mainSplitRef;
    private boolean libraryCollapsed = false;
    private boolean propertiesCollapsed = true;
    private JButton libraryToggleBtn;
    private JButton propertiesToggleBtn;

    public Planner2DPage(AppFrame frame, Router router, AppState appState) {
        this(frame, router, appState, null, null);
    }

    public Planner2DPage(AppFrame frame, Router router, AppState appState, SettingsRepository settingsRepo) {
        this(frame, router, appState, settingsRepo, null);
    }

    // overload used by Shell to pass role/session
    public Planner2DPage(AppFrame frame, Router router, AppState appState, SettingsRepository settingsRepo, Session session) {
        this.router = router;
        this.appState = appState;
        this.settingsRepo = settingsRepo;
        this.session = session;

        setLayout(new BorderLayout());
        setBackground(UiKit.BG);
        setBorder(new EmptyBorder(16, 16, 16, 16));

        // IMPORTANT: Create canvas FIRST
        canvas = new RoomCanvas();

        // ===== Autosave timer =====
        autosaveTimer = new javax.swing.Timer(700, e -> {
            ((javax.swing.Timer) e.getSource()).stop();
            // customer is read-only, so never autosave
            if (!isCustomer()) saveDesign("Auto-saved");
        });
        autosaveTimer.setRepeats(false);

        rebuildFullUI();

        addComponentListener(new ComponentAdapter() {
            @Override public void componentResized(ComponentEvent e) {
                applyResponsiveLayout();
            }
        });

        // RELOAD on navigation
        if (router != null) {
            router.addListener(key -> {
                if (ScreenKeys.PLANNER_2D.equals(key)) {
                    rebuildFullUI();
                }
            });
        }
    }

    private boolean isCustomer() {
        if (session == null) return false; // default admin
        User u = session.getCurrentUser();
        if (u == null) return false;
        return u.isCustomer();
    }

    /* ========================= UI REBUILD / EMPTY STATE ========================= */

    private void rebuildFullUI() {
        removeAll();
        libraryPanelRef = null;
        propertiesPanelRef = null;
        libraryCenterSplitRef = null;
        mainSplitRef = null;

        Design d = (appState == null) ? null : appState.getCurrentDesignOrNull();
        if (d == null) {
            showNoDesignState();
            revalidate();
            repaint();
            return;
        }

        JPanel left = isCustomer() ? null : buildFurnitureLibrary();
        JPanel right = buildPropertiesPanel();
        libraryPanelRef = left;
        propertiesPanelRef = right;

        JPanel center = new JPanel(new BorderLayout(12, 12));
        center.setOpaque(false);

        JPanel pageHeader = buildPageHeader();
        center.add(pageHeader, BorderLayout.NORTH);

        UiKit.RoundedPanel canvasCard = new UiKit.RoundedPanel(18, UiKit.WHITE);
        canvasCard.setBorderPaint(UiKit.BORDER);
        canvasCard.setLayout(new BorderLayout());

        // Customer read-only overlay to block add/move/delete/resize even if canvas supports drag.
        if (isCustomer()) {
            canvasCard.add(buildReadOnlyCanvasOverlay(), BorderLayout.CENTER);
        } else {
            canvasCard.add(canvas, BorderLayout.CENTER);
        }

        center.add(canvasCard, BorderLayout.CENTER);

        // For customers: no furniture library, just center + properties
        if (isCustomer()) {
            mainSplitRef = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, center, right);
            mainSplitRef.setBorder(BorderFactory.createEmptyBorder());
            mainSplitRef.setOpaque(false);
            mainSplitRef.setContinuousLayout(true);
            mainSplitRef.setOneTouchExpandable(false);
            mainSplitRef.setDividerSize(6);
            mainSplitRef.setResizeWeight(1.0);
        } else {
            libraryCenterSplitRef = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, left, center);
            libraryCenterSplitRef.setBorder(BorderFactory.createEmptyBorder());
            libraryCenterSplitRef.setOpaque(false);
            libraryCenterSplitRef.setContinuousLayout(true);
            libraryCenterSplitRef.setOneTouchExpandable(false);
            libraryCenterSplitRef.setDividerSize(6);
            libraryCenterSplitRef.setResizeWeight(0.0);

            mainSplitRef = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, libraryCenterSplitRef, right);
            mainSplitRef.setBorder(BorderFactory.createEmptyBorder());
            mainSplitRef.setOpaque(false);
            mainSplitRef.setContinuousLayout(true);
            mainSplitRef.setOneTouchExpandable(false);
            mainSplitRef.setDividerSize(6);
            mainSplitRef.setResizeWeight(1.0);
        }

        add(mainSplitRef, BorderLayout.CENTER);

        // Load design now that UI exists
        loadDesignIntoCanvas();

        // Wire controls after canvas has items
        wireRightPanel();
        updatePropertiesFromSelection();
        rebuildPlacedItemsList();

        // lock UI pieces for customer
        if (isCustomer()) {
            applyCustomerReadOnlyMode();
        }

        SwingUtilities.invokeLater(this::applyResponsiveLayout);
    }

    private JComponent buildReadOnlyCanvasOverlay() {
        JPanel wrap = new JPanel();
        wrap.setOpaque(false);
        wrap.setLayout(new OverlayLayout(wrap));

        // base canvas
        wrap.add(canvas);

        // blocker panel on top (transparent) to swallow edits
        JPanel blocker = new JPanel(new BorderLayout());
        blocker.setOpaque(false);
        blocker.setFocusable(true);

        // small badge
        JLabel badge = new JLabel("READ-ONLY");
        badge.setOpaque(true);
        badge.setBackground(UiKit.isDarkBlueMode() ? new Color(0x172554) : UiKit.CHIP_ACTIVE_BG);
        badge.setForeground(UiKit.isDarkBlueMode() ? new Color(0xBFDBFE) : UiKit.CHIP_ACTIVE_TEXT);
        badge.setBorder(new EmptyBorder(6, 10, 6, 10));
        badge.setFont(UiKit.scaled(badge, Font.BOLD, 0.80f));

        JPanel badgeRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        badgeRow.setOpaque(false);
        badgeRow.add(badge);

        blocker.add(badgeRow, BorderLayout.NORTH);

        // swallow mouse events
        MouseAdapter swallowMouse = new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) { blocker.requestFocusInWindow(); e.consume(); }
            @Override public void mouseReleased(MouseEvent e) { e.consume(); }
            @Override public void mouseClicked(MouseEvent e) { e.consume(); }
            @Override public void mouseDragged(MouseEvent e) { e.consume(); }
            @Override public void mouseMoved(MouseEvent e) { e.consume(); }
            @Override public void mouseWheelMoved(MouseWheelEvent e) { e.consume(); }
        };
        blocker.addMouseListener(swallowMouse);
        blocker.addMouseMotionListener(swallowMouse);
        blocker.addMouseWheelListener(swallowMouse);

        // swallow delete/backspace keys (and anything else that might trigger edits)
        blocker.addKeyListener(new KeyAdapter() {
            @Override public void keyPressed(KeyEvent e) { e.consume(); }
            @Override public void keyReleased(KeyEvent e) { e.consume(); }
            @Override public void keyTyped(KeyEvent e) { e.consume(); }
        });

        wrap.add(blocker);
        return wrap;
    }

    private void applyCustomerReadOnlyMode() {
        // Right panel must be view-only
        setRightPanelEnabled(false);

        // Buttons that imply editing
        deleteBtn.setEnabled(false);

        // shading tools are admin-only (also hidden in sidebar)
        shadingToolsBtn.setVisible(false);

        // sliders/fields view-only but still show values (disabled)
        xField.setEnabled(false);
        yField.setEnabled(false);
        rotField.setEnabled(false);
        rotationSlider.setEnabled(false);
        wField.setEnabled(false);
        hField.setEnabled(false);
        lockAspect.setEnabled(false);
        shadingSlider.setEnabled(false);
        refreshLockAspectToggleVisual();
    }

    private void showNoDesignState() {
        JPanel wrap = new JPanel(new GridBagLayout());
        wrap.setOpaque(false);

        UiKit.RoundedPanel card = new UiKit.RoundedPanel(18, UiKit.WHITE);
        card.setBorderPaint(UiKit.BORDER);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(new EmptyBorder(28, 28, 28, 28));

        JLabel title = new JLabel("Select or create a design first");
        title.setForeground(UiKit.TEXT);
        title.setFont(UiKit.scaled(title, Font.BOLD, 1.10f));
        title.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel sub = new JLabel("<html>Your planner tools work on the <b>currently selected design</b>.<br/>Go to the Design Library to pick one, or create a new design.</html>");
        sub.setForeground(UiKit.MUTED);
        sub.setFont(UiKit.scaled(sub, Font.PLAIN, 0.95f));
        sub.setBorder(new EmptyBorder(8, 0, 0, 0));
        sub.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 6));
        btnRow.setOpaque(false);
        btnRow.setBorder(new EmptyBorder(12, 0, 0, 0));
        btnRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        JButton goLibrary = UiKit.primaryButton("Go to Design Library");
        goLibrary.addActionListener(e -> {
            if (router != null) router.show(ScreenKeys.DESIGN_LIBRARY);
        });

        JButton createNew = UiKit.ghostButton("Create New Design");
        createNew.addActionListener(e -> {
            if (router != null) router.show(ScreenKeys.NEW_DESIGN);
        });

        JButton importBtn = UiKit.ghostButton("Import Design JSON");
        importBtn.addActionListener(e -> onImportDesigns());

        // Customer shouldn't see create-new here
        if (isCustomer()) {
            btnRow.add(goLibrary);
            btnRow.add(importBtn);
        } else {
            btnRow.add(goLibrary);
            btnRow.add(createNew);
            btnRow.add(importBtn);
        }

        card.add(title);
        card.add(sub);
        card.add(btnRow);

        Dimension pref = card.getPreferredSize();
        card.setMinimumSize(new Dimension(0, pref.height));
        card.setPreferredSize(new Dimension(Math.min(760, pref.width), pref.height));
        card.setMaximumSize(new Dimension(760, Integer.MAX_VALUE));

        JPanel row = new JPanel();
        row.setOpaque(false);
        row.setLayout(new BoxLayout(row, BoxLayout.X_AXIS));
        row.add(Box.createHorizontalGlue());
        row.add(card);
        row.add(Box.createHorizontalGlue());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(24, 18, 24, 18);
        wrap.add(row, gbc);
        add(wrap, BorderLayout.CENTER);
    }

    private void onImportDesigns() {
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
        if (!all.isEmpty()) {
            appState.setCurrentDesignId(all.get(0).getId());
        }

        JOptionPane.showMessageDialog(this,
                "Imported " + importedCount + " design(s) from:\n" + in.getAbsolutePath(),
                "Import Complete", JOptionPane.INFORMATION_MESSAGE);

        rebuildFullUI();
    }

    /* ========================= Responsive layout ========================= */

    private void applyResponsiveLayout() {
        if (libraryPanelRef == null || propertiesPanelRef == null || mainSplitRef == null || libraryCenterSplitRef == null) {
            return;
        }

        // Keep exactly one sidebar open at a time.
        if (libraryCollapsed == propertiesCollapsed) {
            libraryCollapsed = false;
            propertiesCollapsed = true;
        }

        int width = Math.max(getWidth(), 980);
        int libraryW = clampInt((int) Math.round(width * 0.25), 230, 340);
        int propertiesW = clampInt((int) Math.round(width * 0.28), 260, 370);

        if (width < 1260) {
            libraryW = 220;
            propertiesW = 250;
        }
        if (width < 1080) {
            libraryW = 200;
            propertiesW = 230;
        }

        if (propertiesCollapsed) {
            libraryW = clampInt((int) Math.round(width * 0.36), 300, 480);
        }
        if (libraryCollapsed) {
            propertiesW = clampInt((int) Math.round(width * 0.40), 340, 520);
        }

        if (libraryCollapsed) {
            libraryPanelRef.setMinimumSize(new Dimension(0, 0));
            libraryPanelRef.setPreferredSize(new Dimension(0, 0));
            libraryPanelRef.setMaximumSize(new Dimension(0, Integer.MAX_VALUE));
            libraryCenterSplitRef.setDividerSize(0);
            libraryCenterSplitRef.setDividerLocation(0);
        } else {
            libraryPanelRef.setMinimumSize(new Dimension(190, 0));
            libraryPanelRef.setPreferredSize(new Dimension(libraryW, 0));
            libraryPanelRef.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
            libraryCenterSplitRef.setDividerSize(6);

            int effectivePropertiesW = propertiesCollapsed ? 0 : propertiesW;
            int centerLeftWidth = Math.max(420, width - effectivePropertiesW - 34);
            int divider1 = clampInt(libraryW, 190, Math.max(230, centerLeftWidth - 270));
            libraryCenterSplitRef.setDividerLocation(divider1);
        }

        if (propertiesCollapsed) {
            propertiesPanelRef.setMinimumSize(new Dimension(0, 0));
            propertiesPanelRef.setPreferredSize(new Dimension(0, 0));
            propertiesPanelRef.setMaximumSize(new Dimension(0, Integer.MAX_VALUE));
            mainSplitRef.setDividerSize(0);
            mainSplitRef.setDividerLocation(width);
        } else {
            propertiesPanelRef.setMinimumSize(new Dimension(220, 0));
            propertiesPanelRef.setPreferredSize(new Dimension(propertiesW, 0));
            propertiesPanelRef.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
            mainSplitRef.setDividerSize(6);

            int divider2 = clampInt(width - propertiesW - 14, 440, Math.max(520, width - 220));
            mainSplitRef.setDividerLocation(divider2);
        }

        updatePanelToggleButtons();
    }

    private void toggleLibraryPanel() {
        if (libraryCollapsed) {
            libraryCollapsed = false;
            propertiesCollapsed = true;
        } else {
            libraryCollapsed = true;
            propertiesCollapsed = false;
        }
        applyResponsiveLayout();
    }

    private void togglePropertiesPanel() {
        if (propertiesCollapsed) {
            propertiesCollapsed = false;
            libraryCollapsed = true;
        } else {
            propertiesCollapsed = true;
            libraryCollapsed = false;
        }
        applyResponsiveLayout();
    }

    private void updatePanelToggleButtons() {
        if (libraryToggleBtn != null) {
            libraryToggleBtn.setText(libraryCollapsed ? "▶ Library" : "◀ Library");
            libraryToggleBtn.setForeground(libraryCollapsed ? UiKit.TEXT : UiKit.PRIMARY_DARK);
        }
        if (propertiesToggleBtn != null) {
            propertiesToggleBtn.setText(propertiesCollapsed ? "Properties ◀" : "Properties ▶");
            propertiesToggleBtn.setForeground(propertiesCollapsed ? UiKit.TEXT : UiKit.PRIMARY_DARK);
        }
    }

    /* ========================= DESIGN LOAD / SAVE ========================= */

    private void loadDesignIntoCanvas() {
        Design d = appState.getCurrentDesignOrNull();
        if (d == null) return;

        // Use saved layout bounds as reference frame for pixel->rel conversion
        Rectangle legacy = null;
        try {
            Integer lx = d.getLayoutX();
            Integer ly = d.getLayoutY();
            Integer lw = d.getLayoutWidth();
            Integer lh = d.getLayoutHeight();
            if (lx != null && ly != null && lw != null && lh != null && lw > 0 && lh > 0) {
                legacy = new Rectangle(lx, ly, lw, lh);
            }
        } catch (Throwable ignored) { }

        canvas.setLegacyLayoutBounds(legacy);

        // Always load a deep copy into the canvas
        java.util.List<FurnitureItem> items = deepCopyItems(d.getItems());

        canvas.setRoomSpec(d.getRoomSpec());
        canvas.setItems(items);

        // Sync cross-screen undo/redo to this design + current items
        if (appState != null) {
            appState.syncHistoryForDesign(d.getId(), items);
        }

        autosaved.setForeground(UiKit.MUTED);
        autosaved.setText(isCustomer() ? "● View-only" : "● Loaded");

        rebuildPlacedItemsList();
    }

    private boolean isAutosaveEnabled() {
        if (settingsRepo == null) return true;
        try {
            return settingsRepo.get().isAutosaveEnabled();
        } catch (Exception e) {
            return true;
        }
    }

    private void markDirtyAndAutosave() {
        // Customer view-only: never mark dirty / autosave
        if (isCustomer()) return;

        if (appState != null && appState.isRestoringHistory()) return;
        if (appState.getCurrentDesignOrNull() == null) return;

        if (isAutosaveEnabled()) {
            if (autosaveTimer.isRunning()) autosaveTimer.restart();
            else autosaveTimer.start();
        }

        autosaved.setForeground(UiKit.PILL_WARN_FG);
        autosaved.setText("● Unsaved changes");
    }

    private void saveDesign(String reasonLabel) {
        // Customer view-only: never save from 2D
        if (isCustomer()) return;

        Design d = appState.getCurrentDesignOrNull();
        if (d == null) {
            JOptionPane.showMessageDialog(
                    this,
                    "Select or create a design first.",
                    "No design selected",
                    JOptionPane.INFORMATION_MESSAGE
            );
            return;
        }

        d.setItems(deepCopyItems(canvas.getItems()));

        Rectangle bounds = canvas.getRoomBounds();
        if (bounds != null) {
            d.setLayoutX(bounds.x);
            d.setLayoutY(bounds.y);
            d.setLayoutWidth(bounds.width);
            d.setLayoutHeight(bounds.height);
        }

        if (d.getItems() != null && !d.getItems().isEmpty()) {
            if (d.getStatus() == DesignStatus.DRAFT) {
                d.setStatus(DesignStatus.IN_PROGRESS);
            }
        }

        d.touchUpdatedAtNow();
        appState.getRepo().upsert(d);

        autosaved.setForeground(UiKit.PILL_SUCCESS_FG);
        autosaved.setText("● " + reasonLabel);
    }

    private void onExportData() {
        if (appState == null || appState.getRepo() == null) {
            JOptionPane.showMessageDialog(this, "Design repository not available.");
            return;
        }

        // Only admins do a save-before-export
        if (!isCustomer() && appState.getCurrentDesignOrNull() != null) {
            saveDesign("Saved before export");
        }

        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Export RoomViz Designs");
        chooser.setSelectedFile(new java.io.File("roomviz-designs-export.json"));

        int result = chooser.showSaveDialog(this);
        if (result != JFileChooser.APPROVE_OPTION) return;

        java.io.File out = chooser.getSelectedFile();
        appState.getRepo().exportTo(out);

        JOptionPane.showMessageDialog(
                this,
                "Exported designs to:\n" + out.getAbsolutePath(),
                "Export Complete",
                JOptionPane.INFORMATION_MESSAGE
        );
    }

    /* ========================= Undo/Redo via AppState ========================= */

    private void pushBeforeChange() {
        // Customer view-only
        if (isCustomer()) return;

        Design d = (appState == null) ? null : appState.getCurrentDesignOrNull();
        if (d == null) return;
        appState.pushBeforeChange(d.getId(), canvas.getItems());
    }

    private void recordAfterChange() {
        // Customer view-only
        if (isCustomer()) return;

        Design d = (appState == null) ? null : appState.getCurrentDesignOrNull();
        if (d == null) return;
        appState.recordAfterChange(d.getId(), canvas.getItems());
    }

    private void restoreSnapshot(java.util.List<FurnitureItem> snapshot) {
        String prevSelId = (appState == null) ? null : appState.getSelectedItemId();

        if (appState != null) appState.beginHistoryRestore();
        try {
            canvas.setItems(deepCopyItems(snapshot));

            // try re-select previous selected item
            if (prevSelId != null) {
                for (FurnitureItem it : canvas.getItems()) {
                    if (prevSelId.equals(it.getId())) {
                        canvas.setSelected(it);
                        break;
                    }
                }
            }

            updatePropertiesFromSelection();
            FurnitureItem sel = canvas.getSelected();
            if (appState != null) appState.setSelectedItemId(sel == null ? null : sel.getId());
            rebuildPlacedItemsList();
        } finally {
            if (appState != null) appState.endHistoryRestore();
        }
    }

    private void doUndo() {
        if (isCustomer()) return;

        Design d = (appState == null) ? null : appState.getCurrentDesignOrNull();
        if (d == null) return;

        java.util.List<FurnitureItem> prev = appState.undo(d.getId(), canvas.getItems());
        if (prev == null) return;

        restoreSnapshot(prev);
        markDirtyAndAutosave();
    }

    private void doRedo() {
        if (isCustomer()) return;

        Design d = (appState == null) ? null : appState.getCurrentDesignOrNull();
        if (d == null) return;

        java.util.List<FurnitureItem> next = appState.redo(d.getId(), canvas.getItems());
        if (next == null) return;

        restoreSnapshot(next);
        markDirtyAndAutosave();
    }

    /* ========================= Deep copy helper ========================= */

    private java.util.List<FurnitureItem> deepCopyItems(java.util.List<FurnitureItem> src) {
        java.util.List<FurnitureItem> out = new java.util.ArrayList<>();
        if (src == null) return out;

        for (FurnitureItem it : src) {
            if (it == null) continue;

            FurnitureItem c = new FurnitureItem(
                    it.getId(),
                    it.getName(),
                    it.getKind(),
                    it.getX(),
                    it.getY(),
                    it.getW(),
                    it.getH()
            );

            try { c.setRotation(it.getRotation()); } catch (Exception ignored) {}
            try { c.setColorHex(it.getColorHex()); } catch (Exception ignored) {}
            try { c.setShadingPercent(it.getShadingPercent()); } catch (Exception ignored) {}
            try { c.setMaterial(it.getMaterial()); } catch (Exception ignored) {}
            try { c.setLighting(it.getLighting()); } catch (Exception ignored) {}

            out.add(c);
        }
        return out;
    }

    /* ========================= LEFT: Furniture Library ========================= */

    private static void setChipClick(JComponent chip, Runnable onClick) {
        chip.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        chip.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { onClick.run(); }
        });
    }

    private void rebuildFurnitureChipsUI(Runnable applyFilter) {
        if (chipsPanelRef == null) return;

        chipsPanelRef.removeAll();

        JComponent allChip = (activeFurnitureTab == FurnitureFilterTab.ALL)
                ? UiKit.chipPrimary("All") : UiKit.chip("All");
        JComponent seatingChip = (activeFurnitureTab == FurnitureFilterTab.SEATING)
                ? UiKit.chipPrimary("Seating") : UiKit.chip("Seating");
        JComponent tablesChip = (activeFurnitureTab == FurnitureFilterTab.TABLES)
                ? UiKit.chipPrimary("Tables") : UiKit.chip("Tables");

        setChipClick(allChip, () -> {
            activeFurnitureTab = FurnitureFilterTab.ALL;
            rebuildFurnitureChipsUI(applyFilter);
            applyFilter.run();
        });
        setChipClick(seatingChip, () -> {
            activeFurnitureTab = FurnitureFilterTab.SEATING;
            rebuildFurnitureChipsUI(applyFilter);
            applyFilter.run();
        });
        setChipClick(tablesChip, () -> {
            activeFurnitureTab = FurnitureFilterTab.TABLES;
            rebuildFurnitureChipsUI(applyFilter);
            applyFilter.run();
        });

        chipsPanelRef.add(allChip);
        chipsPanelRef.add(seatingChip);
        chipsPanelRef.add(tablesChip);

        chipsPanelRef.revalidate();
        chipsPanelRef.repaint();
    }

    private JPanel buildFurnitureLibrary() {
        UiKit.RoundedPanel card = new UiKit.RoundedPanel(18, UiKit.WHITE);
        card.setBorderPaint(UiKit.BORDER);
        card.setMinimumSize(new Dimension(190, 0));
        card.setPreferredSize(new Dimension(300, 0));
        card.setLayout(new BorderLayout());

        // Header section
        JPanel header = new JPanel();
        header.setOpaque(false);
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setBorder(new EmptyBorder(18, 16, 0, 16));

        JLabel title = new JLabel("Furniture Library");
        title.setForeground(UiKit.TEXT);
        title.setFont(UiKit.scaled(title, Font.BOLD, 1.05f));
        title.setAlignmentX(0.0f);

        JLabel subtitle = new JLabel(isCustomer() ? "View-only (customers can't add items)" : "Double-click to add to canvas");
        subtitle.setForeground(UiKit.MUTED);
        subtitle.setFont(UiKit.scaled(subtitle, Font.PLAIN, 0.82f));
        subtitle.setBorder(new EmptyBorder(2, 0, 0, 0));
        subtitle.setAlignmentX(0.0f);

        header.add(title);
        header.add(subtitle);
        header.add(Box.createVerticalStrut(12));

        final String searchPlaceholder = "Search furniture...";
        JTextField search = UiKit.searchField(searchPlaceholder);
        search.setPreferredSize(new Dimension(0, 36));
        search.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        search.setAlignmentX(0.0f);
        header.add(search);
        header.add(Box.createVerticalStrut(10));

        final java.util.List<FurnitureTemplate> allTemplates = new java.util.ArrayList<>();
        allTemplates.add(new FurnitureTemplate("Accent Chair", "32\" × 34\"", FurnitureKind.CHAIR, 64, 48));
        allTemplates.add(new FurnitureTemplate("Dining Chair", "18\" × 22\"", FurnitureKind.CHAIR, 44, 36));
        allTemplates.add(new FurnitureTemplate("Lounge Chair", "36\" × 38\"", FurnitureKind.CHAIR, 70, 56));
        allTemplates.add(new FurnitureTemplate("Rectangular Table", "72\" × 36\"", FurnitureKind.TABLE_RECT, 120, 70));
        allTemplates.add(new FurnitureTemplate("Round Table", "48\" Ø", FurnitureKind.TABLE_ROUND, 90, 90));
        allTemplates.add(new FurnitureTemplate("Coffee Table", "48\" × 24\"", FurnitureKind.TABLE_RECT, 90, 50));
        allTemplates.add(new FurnitureTemplate("End Table", "20\" × 20\"", FurnitureKind.TABLE_RECT, 44, 44));
        allTemplates.add(new FurnitureTemplate("Console Table", "48\" × 16\"", FurnitureKind.TABLE_RECT, 90, 38));

        DefaultListModel<FurnitureTemplate> model = new DefaultListModel<>();
        for (FurnitureTemplate t : allTemplates) model.addElement(t);

        JList<FurnitureTemplate> list = new JList<>(model);
        list.setCellRenderer(new FurnitureCell());
        list.setFixedCellHeight(60);
        list.setBorder(new EmptyBorder(4, 0, 4, 0));
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setBackground(UiKit.WHITE);

        list.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                // customer cannot add items
                if (isCustomer()) return;

                if (e.getClickCount() == 2) {
                    FurnitureTemplate t = list.getSelectedValue();
                    if (t != null) {
                        pushBeforeChange();
                        canvas.addItemFromTemplate(t);
                        recordAfterChange();

                        markDirtyAndAutosave();
                        rebuildPlacedItemsList();
                        canvas.requestFocusInWindow();
                    }
                }
            }
        });

        JScrollPane sc = new JScrollPane(list);
        sc.setBorder(BorderFactory.createEmptyBorder());
        sc.getVerticalScrollBar().setUnitIncrement(14);

        JPanel chips = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        chips.setOpaque(false);
        chips.setAlignmentX(0.0f);
        chipsPanelRef = chips;

        Runnable applyFilter = () -> {
            String raw = (search.getText() == null) ? "" : search.getText().trim();
            String q = raw.toLowerCase();
            if (q.equals(searchPlaceholder.toLowerCase())) q = "";

            model.clear();

            for (FurnitureTemplate t : allTemplates) {
                boolean okTab = true;
                if (activeFurnitureTab == FurnitureFilterTab.SEATING) {
                    okTab = (t.kind == FurnitureKind.CHAIR);
                } else if (activeFurnitureTab == FurnitureFilterTab.TABLES) {
                    okTab = (t.kind == FurnitureKind.TABLE_RECT || t.kind == FurnitureKind.TABLE_ROUND);
                }
                if (!okTab) continue;

                boolean okSearch = q.isEmpty() || (t.name != null && t.name.toLowerCase().contains(q));
                if (!okSearch) continue;

                model.addElement(t);
            }

            if (!model.isEmpty()) list.setSelectedIndex(0);
        };

        rebuildFurnitureChipsUI(applyFilter);

        search.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { applyFilter.run(); }
            @Override public void removeUpdate(DocumentEvent e) { applyFilter.run(); }
            @Override public void changedUpdate(DocumentEvent e) { applyFilter.run(); }
        });

        header.add(chips);
        header.add(Box.createVerticalStrut(8));

        // Thin separator
        JPanel sep = new JPanel();
        sep.setBackground(UiKit.BORDER);
        sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        sep.setPreferredSize(new Dimension(0, 1));
        sep.setAlignmentX(0.0f);
        header.add(sep);

        card.add(header, BorderLayout.NORTH);
        card.add(sc, BorderLayout.CENTER);
        return card;
    }

    private static class FurnitureCell extends JPanel implements ListCellRenderer<FurnitureTemplate> {
        private final JLabel name = new JLabel();
        private final JLabel size = new JLabel();
        private final JPanel iconCircle = new JPanel(new GridBagLayout());
        private final JLabel icon = new JLabel();

        FurnitureCell() {
            setLayout(new BorderLayout(12, 0));
            setOpaque(true);
            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 1, 0, UiKit.BORDER),
                    new EmptyBorder(10, 16, 10, 16)
            ));

            // Circular icon badge
            iconCircle.setOpaque(true);
            iconCircle.setPreferredSize(new Dimension(36, 36));
            iconCircle.setMinimumSize(new Dimension(36, 36));
            iconCircle.setMaximumSize(new Dimension(36, 36));

            icon.setHorizontalAlignment(SwingConstants.CENTER);
            icon.setFont(FontAwesome.solid(13f));
            iconCircle.add(icon);

            JPanel text = new JPanel();
            text.setOpaque(false);
            text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));

            name.setFont(UiKit.scaled(name, Font.BOLD, 0.92f));
            size.setFont(UiKit.scaled(size, Font.PLAIN, 0.80f));

            text.add(name);
            text.add(Box.createVerticalStrut(2));
            text.add(size);

            add(iconCircle, BorderLayout.WEST);
            add(text, BorderLayout.CENTER);
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends FurnitureTemplate> list, FurnitureTemplate value,
                                                      int index, boolean isSelected, boolean cellHasFocus) {
            name.setText(value.name);
            size.setText(value.displaySize);
            icon.setText(value.kind.iconText);
            setToolTipText(value.name + " (" + value.displaySize + ")");

            if (isSelected) {
                setBackground(UiKit.CHIP_ACTIVE_BG);
                name.setForeground(UiKit.isDarkBlueMode() ? new Color(0xE0E7FF) : UiKit.TEXT);
                size.setForeground(UiKit.isDarkBlueMode() ? new Color(0x93C5FD) : UiKit.MUTED);
                iconCircle.setBackground(UiKit.isDarkBlueMode() ? new Color(0x1E40AF) : new Color(0xC7D2FE));
                icon.setForeground(UiKit.isDarkBlueMode() ? new Color(0xBFDBFE) : UiKit.PRIMARY);
                setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(0, 3, 1, 0, UiKit.PRIMARY),
                        new EmptyBorder(10, 13, 10, 16)
                ));
            } else {
                setBackground(UiKit.WHITE);
                name.setForeground(UiKit.TEXT);
                size.setForeground(UiKit.MUTED);
                iconCircle.setBackground(UiKit.ICON_BG);
                icon.setForeground(UiKit.PRIMARY);
                setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(0, 0, 1, 0, UiKit.BORDER),
                        new EmptyBorder(10, 16, 10, 16)
                ));
            }
            return this;
        }
    }

    /* ========================= CENTER: Page header controls ========================= */

    private JPanel buildPageHeader() {
        UiKit.RoundedPanel header = new UiKit.RoundedPanel(18, UiKit.WHITE);
        header.setBorderPaint(UiKit.BORDER);
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setBorder(new EmptyBorder(8, 10, 8, 10));

        JPanel top = new JPanel(new BorderLayout(10, 0));
        top.setOpaque(false);

        JPanel identity = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        identity.setOpaque(false);

        JLabel appName = new JLabel("RoomPlan");
        appName.setFont(UiKit.scaled(appName, Font.BOLD, 0.92f));
        appName.setForeground(UiKit.TEXT);

        JLabel divider = new JLabel("|");
        divider.setForeground(UiKit.BORDER);

        Design d = appState.getCurrentDesignOrNull();
        String name = (d == null) ? "No design selected" : d.getName();

        JLabel designName = new JLabel(name);
        designName.setFont(UiKit.scaled(designName, Font.PLAIN, 0.90f));
        designName.setForeground(UiKit.META_PILL_FG);

        JLabel workspaceTag = new JLabel(isCustomer() ? "2D View (Read-only)" : "2D Planner Workspace");
        workspaceTag.setFont(UiKit.scaled(workspaceTag, Font.PLAIN, 0.80f));
        workspaceTag.setForeground(UiKit.MUTED);

        identity.add(appName);
        identity.add(divider);
        identity.add(designName);

        top.add(identity, BorderLayout.WEST);
        top.add(workspaceTag, BorderLayout.EAST);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        actions.setOpaque(false);

        JButton undo = UiKit.iconButton(FontAwesome.ROTATE_LEFT);
        JButton redo = UiKit.iconButton(FontAwesome.ROTATE_RIGHT);
        styleCompactIcon(undo);
        styleCompactIcon(redo);
        undo.addActionListener(e -> doUndo());
        redo.addActionListener(e -> doRedo());

        JButton save = UiKit.ghostButton("Save");
        styleCompactGhost(save);
        save.addActionListener(e -> saveDesign("Saved"));

        // Customer read-only: disable edit buttons
        if (isCustomer()) {
            undo.setEnabled(false);
            redo.setEnabled(false);
            save.setEnabled(false);
        }

        autosaved.setForeground(UiKit.MUTED);
        autosaved.setFont(UiKit.scaled(autosaved, Font.PLAIN, 0.78f));
        if (isCustomer()) autosaved.setText("● View-only");

        JToggleButton toggle2d = new JToggleButton("2D");
        JToggleButton toggle3d = new JToggleButton("3D");
        ButtonGroup g = new ButtonGroup();
        g.add(toggle2d);
        g.add(toggle3d);
        toggle2d.setSelected(true);

        styleMiniToggle(toggle2d, true);
        styleMiniToggle(toggle3d, false);

        toggle2d.addActionListener(e -> {
            styleMiniToggle(toggle2d, true);
            styleMiniToggle(toggle3d, false);
        });
        toggle3d.addActionListener(e -> {
            if (appState.getCurrentDesignOrNull() == null) {
                JOptionPane.showMessageDialog(
                        this,
                        "Select or create a design first.",
                        "No design selected",
                        JOptionPane.INFORMATION_MESSAGE
                );
                toggle2d.setSelected(true);
                styleMiniToggle(toggle2d, true);
                styleMiniToggle(toggle3d, false);
                return;
            }

            // Only admins save before 3D (customers view-only)
            if (!isCustomer()) saveDesign("Saved before 3D");

            styleMiniToggle(toggle2d, false);
            styleMiniToggle(toggle3d, true);
            if (router != null) router.show(ScreenKeys.VIEW_3D);
        });

        JButton export = UiKit.ghostButton("Export");
        styleCompactGhost(export);
        export.addActionListener(e -> onExportData());

        libraryToggleBtn = UiKit.ghostButton("");
        styleCompactGhost(libraryToggleBtn);
        libraryToggleBtn.addActionListener(e -> toggleLibraryPanel());
        libraryToggleBtn.setToolTipText("Show/Hide Furniture Library");

        propertiesToggleBtn = UiKit.ghostButton("");
        styleCompactGhost(propertiesToggleBtn);
        propertiesToggleBtn.addActionListener(e -> togglePropertiesPanel());
        propertiesToggleBtn.setToolTipText("Show/Hide Properties Panel");

        JPanel viewToggle = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        viewToggle.setOpaque(false);
        viewToggle.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(UiKit.BORDER, 1, true),
                new EmptyBorder(0, 0, 0, 0)
        ));
        viewToggle.add(toggle2d);
        viewToggle.add(toggle3d);

        // Customer read-only: hide editing buttons entirely
        if (isCustomer()) {
            // Only show 2D/3D toggle and view-only label
            actions.add(viewToggle);
            actions.add(autosaved);
        } else {
            updatePanelToggleButtons();
            actions.add(libraryToggleBtn);
            actions.add(propertiesToggleBtn);
            actions.add(undo);
            actions.add(redo);
            actions.add(save);
            actions.add(export);
            actions.add(viewToggle);
            actions.add(autosaved);
        }

        header.add(top);
        header.add(Box.createVerticalStrut(6));
        header.add(actions);
        return header;
    }

    private void styleMiniToggle(AbstractButton b, boolean active) {
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setFont(UiKit.scaled(b, Font.BOLD, 0.80f));
        b.setPreferredSize(new Dimension(44, 30));
        b.setMinimumSize(new Dimension(44, 30));

        if (active) {
            if (UiKit.isDarkBlueMode()) {
                b.setBackground(new Color(0x1D4ED8));
                b.setForeground(Color.WHITE);
                b.setBorder(BorderFactory.createCompoundBorder(
                        new LineBorder(new Color(0x3B82F6), 1, true),
                        new EmptyBorder(3, 10, 3, 10)
                ));
            } else {
                b.setBackground(UiKit.CHIP_ACTIVE_BG);
                b.setForeground(UiKit.CHIP_ACTIVE_TEXT);
                b.setBorder(BorderFactory.createCompoundBorder(
                        new LineBorder(new Color(0xC7D2FE), 1, true),
                        new EmptyBorder(5, 8, 5, 8)
                ));
            }
        } else {
            b.setBackground(UiKit.TIP_BG);
            b.setForeground(UiKit.META_PILL_FG);
            b.setBorder(BorderFactory.createCompoundBorder(
                    new LineBorder(UiKit.TIP_BORDER, 1, true),
                    new EmptyBorder(5, 8, 5, 8)
            ));
        }
    }

    private void styleCompactGhost(AbstractButton b) {
        b.setFocusPainted(false);
        b.setFont(UiKit.scaled((JComponent) b, Font.BOLD, 0.86f));
        b.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(UiKit.BORDER, 1, true),
                new EmptyBorder(6, 11, 6, 11)
        ));
    }

    private void styleCompactIcon(AbstractButton b) {
        b.setFocusPainted(false);
        b.setFont(UiKit.scaled((JComponent) b, Font.BOLD, 0.88f));
        b.setPreferredSize(new Dimension(34, 30));
        b.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(UiKit.BORDER, 1, true),
                new EmptyBorder(4, 8, 4, 8)
        ));
    }

    /* ========================= RIGHT: Properties ========================= */

    private JPanel buildPropertiesPanel() {
        UiKit.RoundedPanel card = new UiKit.RoundedPanel(18, UiKit.WHITE);
        card.setBorderPaint(UiKit.BORDER);
        card.setMinimumSize(new Dimension(220, 0));
        card.setPreferredSize(new Dimension(340, 0));
        card.setLayout(new BorderLayout());

        JPanel content = new JPanel();
        content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBorder(new EmptyBorder(18, 16, 18, 16));

        // Header
        JLabel title = new JLabel("Properties");
        title.setFont(UiKit.scaled(title, Font.BOLD, 1.08f));
        title.setForeground(UiKit.TEXT);
        title.setAlignmentX(0.0f);

        selectedTitle.setFont(UiKit.scaled(selectedTitle, Font.PLAIN, 0.88f));
        selectedTitle.setForeground(UiKit.PRIMARY);
        selectedTitle.setBorder(new EmptyBorder(3, 0, 0, 0));
        selectedTitle.setAlignmentX(0.0f);

        content.add(title);
        content.add(selectedTitle);
        content.add(Box.createVerticalStrut(16));

        // Separator
        content.add(thinDivider());
        content.add(Box.createVerticalStrut(14));

        // For customers: show a polished overview instead of raw "Placed Items"
        if (isCustomer()) {
            JLabel overviewLabel = new JLabel("DESIGN OVERVIEW");
            overviewLabel.setFont(UiKit.scaled(overviewLabel, Font.BOLD, 0.76f));
            overviewLabel.setForeground(UiKit.MUTED);
            overviewLabel.setAlignmentX(0.0f);
            content.add(overviewLabel);
            content.add(Box.createVerticalStrut(4));

            JLabel hint = new JLabel("Items placed in this design:");
            hint.setFont(UiKit.scaled(hint, Font.PLAIN, 0.86f));
            hint.setForeground(UiKit.MUTED);
            hint.setAlignmentX(0.0f);
            content.add(hint);
            content.add(Box.createVerticalStrut(10));
        } 
        // Placed Items
        placedItemsList.setOpaque(false);
        placedItemsList.setLayout(new BoxLayout(placedItemsList, BoxLayout.Y_AXIS));

        placedItemsScroll = new JScrollPane(placedItemsList);
        placedItemsScroll.setBorder(BorderFactory.createEmptyBorder());
        placedItemsScroll.setOpaque(false);
        placedItemsScroll.getViewport().setOpaque(false);
        placedItemsScroll.getVerticalScrollBar().setUnitIncrement(14);
        placedItemsScroll.setPreferredSize(new Dimension(0, 180));

        content.add(propertyGroup("Placed Items", placedItemsScroll));
        content.add(Box.createVerticalStrut(14));

        // Position, Rotation, Scale, Shading — admin only
        if (!isCustomer()) {
            // Position
            content.add(propertyGroup("Position", twoFieldRow("X Position", xField, "Y Position", yField)));
            content.add(Box.createVerticalStrut(14));

            // Rotation
            content.add(propertyGroup("Rotation", rotationRow()));
            content.add(Box.createVerticalStrut(14));

            // Scale
            JPanel scaleBody = new JPanel();
            scaleBody.setOpaque(false);
            scaleBody.setLayout(new BoxLayout(scaleBody, BoxLayout.Y_AXIS));
            scaleBody.add(twoFieldRow("Width", wField, "Height", hField));
            scaleBody.add(Box.createVerticalStrut(10));

            lockAspect.setOpaque(false);
            lockAspect.setForeground(UiKit.MUTED);
            lockAspect.setFont(UiKit.scaled(lockAspect, Font.PLAIN, 0.86f));
            lockAspect.setBorder(new EmptyBorder(4, 2, 0, 0));
            styleLockAspectToggle();
            scaleBody.add(lockAspect);

            content.add(propertyGroup("Scale", scaleBody));
            content.add(Box.createVerticalStrut(14));

            // Shading
            content.add(propertyGroup("Shading Intensity", shadingRow()));
            content.add(Box.createVerticalStrut(12));
        }

        // Shading tools button — admin only
        shadingToolsBtn.setFont(UiKit.scaled(shadingToolsBtn, Font.BOLD, 0.84f));
        shadingToolsBtn.setForeground(UiKit.PRIMARY);
        shadingToolsBtn.setBackground(UiKit.CHIP_ACTIVE_BG);
        shadingToolsBtn.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(UiKit.isDarkBlueMode() ? new Color(0x3B82F6) : new Color(0xC7D2FE), 1, true),
                new EmptyBorder(8, 14, 8, 14)
        ));
        shadingToolsBtn.setOpaque(true);
        shadingToolsBtn.addActionListener(e -> {
            if (router != null) router.show(ScreenKeys.SHADING_COLOR);
        });
        shadingToolsBtn.setAlignmentX(0.0f);

        if (!isCustomer()) {
            content.add(shadingToolsBtn);
            content.add(Box.createVerticalStrut(14));
            // Layer Order (admin only)
            content.add(propertyGroup("Layer Order", layerRow()));
            content.add(Box.createVerticalStrut(18));
        }

        // Delete button (admin only)
        deleteBtn.setForeground(Color.WHITE);
        deleteBtn.setBackground(UiKit.DANGER);
        deleteBtn.setOpaque(true);
        deleteBtn.setFocusPainted(false);
        deleteBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        deleteBtn.setFont(UiKit.scaled(deleteBtn, Font.BOLD, 0.86f));
        deleteBtn.setBorder(new EmptyBorder(9, 14, 9, 14));
        deleteBtn.setAlignmentX(0.0f);

        if (!isCustomer()) {
            content.add(deleteBtn);
        }

        JScrollPane sc = new JScrollPane(content);
        sc.setBorder(BorderFactory.createEmptyBorder());
        sc.setOpaque(false);
        sc.getViewport().setOpaque(false);
        sc.getVerticalScrollBar().setUnitIncrement(14);

        card.add(sc, BorderLayout.CENTER);
        return card;
    }

    private void rebuildPlacedItemsList() {
        placedItemsList.removeAll();

        java.util.List<FurnitureItem> items = canvas.getItems();
        if (items == null || items.isEmpty()) {
            JLabel empty = new JLabel("No items placed yet");
            empty.setForeground(UiKit.MUTED);
            empty.setFont(UiKit.scaled(empty, Font.ITALIC, 0.88f));
            empty.setBorder(new EmptyBorder(10, 8, 10, 8));
            placedItemsList.add(empty);
            placedItemsList.revalidate();
            placedItemsList.repaint();
            return;
        }

        for (FurnitureItem it : items) {
            JPanel row = new JPanel(new BorderLayout(8, 0));
            row.setOpaque(true);
            row.setBackground(UiKit.WHITE);
            row.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 1, 0, UiKit.BORDER),
                    new EmptyBorder(8, 10, 8, 10)
            ));

            JLabel name = new JLabel(it.getName());
            name.setForeground(UiKit.TEXT);
            name.setFont(UiKit.scaled(name, Font.PLAIN, 0.88f));

            JButton remove = new JButton(isCustomer() ? "View" : "Remove");
            remove.setFocusPainted(false);
            remove.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            remove.setFont(UiKit.scaled(remove, Font.BOLD, 0.78f));
            remove.setForeground(isCustomer() ? UiKit.PRIMARY : UiKit.DANGER);
            remove.setBackground(UiKit.isDarkBlueMode()
                    ? (isCustomer() ? new Color(0x0B1B3A) : new Color(0x3B1111))
                    : (isCustomer() ? new Color(0xEEF2FF) : new Color(0xFEF2F2)));
            remove.setOpaque(true);
            remove.setBorder(new EmptyBorder(4, 8, 4, 8));

            name.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            name.addMouseListener(new MouseAdapter() {
                @Override public void mouseClicked(MouseEvent e) {
                    canvas.setSelected(it);
                    canvas.requestFocusInWindow();
                }
            });

            remove.addActionListener(e -> {
                // Customer: selecting only
                if (isCustomer()) {
                    canvas.setSelected(it);
                    canvas.requestFocusInWindow();
                    return;
                }

                pushBeforeChange();
                canvas.deleteItem(it);
                recordAfterChange();

                updatePropertiesFromSelection();
                rebuildPlacedItemsList();
                markDirtyAndAutosave();
            });

            row.add(name, BorderLayout.CENTER);
            row.add(remove, BorderLayout.EAST);

            placedItemsList.add(row);
        }

        placedItemsList.revalidate();
        placedItemsList.repaint();
    }

    private JComponent propertyGroup(String title, JComponent body) {
        JPanel group = new JPanel();
        group.setOpaque(false);
        group.setLayout(new BoxLayout(group, BoxLayout.Y_AXIS));
        group.setAlignmentX(0.0f);

        JLabel t = new JLabel(title.toUpperCase());
        t.setFont(UiKit.scaled(t, Font.BOLD, 0.76f));
        t.setForeground(UiKit.MUTED);
        t.setAlignmentX(0.0f);
        t.setBorder(new EmptyBorder(0, 2, 0, 0));

        body.setAlignmentX(0.0f);

        group.add(t);
        group.add(Box.createVerticalStrut(8));
        group.add(body);
        return group;
    }

    /** Thin horizontal divider line. */
    private JComponent thinDivider() {
        JPanel line = new JPanel();
        line.setBackground(UiKit.BORDER);
        line.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        line.setPreferredSize(new Dimension(0, 1));
        return line;
    }

    private void styleLockAspectToggle() {
        if (!Boolean.TRUE.equals(lockAspect.getClientProperty("lockStyled"))) {
            lockAspect.putClientProperty("lockStyled", true);
            lockAspect.addActionListener(e -> refreshLockAspectToggleVisual());
            lockAspect.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            lockAspect.setBorderPainted(true);
            lockAspect.setFocusPainted(false);
            lockAspect.setOpaque(true);
        }
        refreshLockAspectToggleVisual();
    }

    private void refreshLockAspectToggleVisual() {
        boolean enabled = lockAspect.isEnabled();
        if (lockAspect.isSelected()) {
            lockAspect.setText("🔒 Aspect Locked");
            lockAspect.setForeground(enabled ? UiKit.CHIP_ACTIVE_TEXT : UiKit.MUTED);
            lockAspect.setBackground(UiKit.CHIP_ACTIVE_BG);
            lockAspect.setOpaque(true);
            lockAspect.setBorder(new EmptyBorder(5, 9, 5, 9));
        } else {
            lockAspect.setText("🔓 Aspect Unlocked");
            lockAspect.setForeground(UiKit.MUTED);
            lockAspect.setBackground(UiKit.WHITE);
            lockAspect.setOpaque(false);
            lockAspect.setBorder(new EmptyBorder(5, 9, 5, 9));
        }
    }

    private JComponent twoFieldRow(String l1, JTextField f1, String l2, JTextField f2) {
        JPanel row = new JPanel();
        row.setOpaque(false);
        JComponent left = labeledField(l1, f1);
        JComponent right = labeledField(l2, f2);

        final boolean[] stacked = {false};
        Runnable relayout = () -> {
            int width = row.getWidth();
            boolean shouldStack = (width > 0) && (width < 260);
            if (row.getComponentCount() > 0 && stacked[0] == shouldStack) return;

            stacked[0] = shouldStack;
            row.removeAll();
            if (shouldStack) {
                row.setLayout(new BoxLayout(row, BoxLayout.Y_AXIS));
                row.add(left);
                row.add(Box.createVerticalStrut(8));
                row.add(right);
            } else {
                row.setLayout(new GridLayout(1, 2, 10, 0));
                row.add(left);
                row.add(right);
            }
            row.revalidate();
            row.repaint();
        };

        row.addComponentListener(new ComponentAdapter() {
            @Override public void componentResized(ComponentEvent e) { relayout.run(); }
            @Override public void componentShown(ComponentEvent e) { relayout.run(); }
        });
        SwingUtilities.invokeLater(relayout);
        return row;
    }

    private JComponent labeledField(String label, JTextField field) {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));

        JLabel l = new JLabel(label);
        l.setFont(UiKit.scaled(l, Font.PLAIN, 0.84f));
        l.setForeground(UiKit.MUTED);

        field.setFont(UiKit.scaled(field, Font.PLAIN, 0.92f));
        field.setForeground(UiKit.TEXT);
        field.setBackground(UiKit.isDarkBlueMode() ? new Color(0x0F172A) : new Color(0xF9FAFB));
        field.setCaretColor(UiKit.TEXT);
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        field.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(UiKit.BORDER, 1, true),
                new EmptyBorder(7, 10, 7, 10)
        ));

        p.add(l);
        p.add(Box.createVerticalStrut(4));
        p.add(field);
        return p;
    }

    private JComponent rotationRow() {
        JPanel row = new JPanel(new BorderLayout(10, 0));
        row.setOpaque(false);

        rotationSlider.setOpaque(false);

        rotField.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(UiKit.BORDER, 1, true),
                new EmptyBorder(8, 10, 8, 10)
        ));
        rotField.setForeground(UiKit.TEXT);
        rotField.setBackground(UiKit.WHITE);
        rotField.setPreferredSize(new Dimension(68, 36));

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        btns.setOpaque(false);

        JButton left90 = UiKit.ghostButton("↺ 90°");
        JButton right90 = UiKit.ghostButton("↻ 90°");
        styleCompactGhost(left90);
        styleCompactGhost(right90);

        left90.addActionListener(e -> {
            if (isCustomer()) return;
            if (canvas.getSelected() == null) return;
            pushBeforeChange();
            canvas.nudgeRotation(-90);
            recordAfterChange();
            markDirtyAndAutosave();
        });
        right90.addActionListener(e -> {
            if (isCustomer()) return;
            if (canvas.getSelected() == null) return;
            pushBeforeChange();
            canvas.nudgeRotation(90);
            recordAfterChange();
            markDirtyAndAutosave();
        });

        // customer: disable nudge buttons
        if (isCustomer()) {
            left90.setEnabled(false);
            right90.setEnabled(false);
        }

        btns.add(left90);
        btns.add(right90);

        JPanel bottom = new JPanel(new BorderLayout(10, 8));
        bottom.setOpaque(false);
        bottom.add(btns, BorderLayout.WEST);

        row.add(rotationSlider, BorderLayout.CENTER);
        row.add(rotField, BorderLayout.EAST);

        JPanel wrap = new JPanel();
        wrap.setOpaque(false);
        wrap.setLayout(new BoxLayout(wrap, BoxLayout.Y_AXIS));
        wrap.add(row);
        wrap.add(Box.createVerticalStrut(8));
        wrap.add(bottom);

        return wrap;
    }

    private JComponent shadingRow() {
        JPanel row = new JPanel(new BorderLayout(10, 0));
        row.setOpaque(false);

        JLabel dark = new JLabel("Darker");
        dark.setForeground(UiKit.MUTED);
        dark.setFont(UiKit.scaled(dark, Font.PLAIN, 0.88f));

        JLabel light = new JLabel("Lighter");
        light.setForeground(UiKit.MUTED);
        light.setFont(UiKit.scaled(light, Font.PLAIN, 0.88f));

        shadingSlider.setOpaque(false);

        row.add(dark, BorderLayout.WEST);
        row.add(shadingSlider, BorderLayout.CENTER);
        row.add(light, BorderLayout.EAST);

        return row;
    }

    private JComponent layerRow() {
        JPanel row = new JPanel(new GridLayout(2, 2, 10, 10));
        row.setOpaque(false);

        JButton forward = UiKit.ghostButton("Forward");
        JButton backward = UiKit.ghostButton("Backward");
        JButton toFront = UiKit.ghostButton("To Front");
        JButton toBack = UiKit.ghostButton("To Back");
        styleCompactGhost(forward);
        styleCompactGhost(backward);
        styleCompactGhost(toFront);
        styleCompactGhost(toBack);

        forward.addActionListener(e -> {
            if (isCustomer()) return;
            if (canvas.getSelected() == null) return;
            pushBeforeChange();
            canvas.layerForward();
            recordAfterChange();
            markDirtyAndAutosave();
            rebuildPlacedItemsList();
        });
        backward.addActionListener(e -> {
            if (isCustomer()) return;
            if (canvas.getSelected() == null) return;
            pushBeforeChange();
            canvas.layerBackward();
            recordAfterChange();
            markDirtyAndAutosave();
            rebuildPlacedItemsList();
        });
        toFront.addActionListener(e -> {
            if (isCustomer()) return;
            if (canvas.getSelected() == null) return;
            pushBeforeChange();
            canvas.layerToFront();
            recordAfterChange();
            markDirtyAndAutosave();
            rebuildPlacedItemsList();
        });
        toBack.addActionListener(e -> {
            if (isCustomer()) return;
            if (canvas.getSelected() == null) return;
            pushBeforeChange();
            canvas.layerToBack();
            recordAfterChange();
            markDirtyAndAutosave();
            rebuildPlacedItemsList();
        });

        row.add(forward);
        row.add(backward);
        row.add(toFront);
        row.add(toBack);

        return row;
    }

    /* ========================= Wiring ========================= */

    private void wireRightPanel() {
        installApplyOnEnterOrBlur(xField, () -> {
            if (isCustomer()) return;
            if (canvas.getSelected() == null) return;
            pushBeforeChange();
            applyPositionFromFields();
            recordAfterChange();
            markDirtyAndAutosave();
        });
        installApplyOnEnterOrBlur(yField, () -> {
            if (isCustomer()) return;
            if (canvas.getSelected() == null) return;
            pushBeforeChange();
            applyPositionFromFields();
            recordAfterChange();
            markDirtyAndAutosave();
        });
        installApplyOnEnterOrBlur(wField, () -> {
            if (isCustomer()) return;
            if (canvas.getSelected() == null) return;
            pushBeforeChange();
            applyScaleFromFields();
            recordAfterChange();
            markDirtyAndAutosave();
        });
        installApplyOnEnterOrBlur(hField, () -> {
            if (isCustomer()) return;
            if (canvas.getSelected() == null) return;
            pushBeforeChange();
            applyScaleFromFields();
            recordAfterChange();
            markDirtyAndAutosave();
        });

        rotationSlider.addChangeListener(e -> {
            if (isCustomer()) return;
            if (programmaticUpdate) return;
            if (canvas.getSelected() == null) return;

            if (rotationSlider.getValueIsAdjusting()) {
                if (!rotationDragging) {
                    rotationDragging = true;
                    pushBeforeChange();
                }
            } else {
                if (rotationDragging) {
                    rotationDragging = false;
                    recordAfterChange();
                }
            }

            canvas.setSelectedRotation(rotationSlider.getValue());
            rotField.setText(String.valueOf(rotationSlider.getValue()));
            markDirtyAndAutosave();
        });

        installApplyOnEnterOrBlur(rotField, () -> {
            if (isCustomer()) return;
            if (canvas.getSelected() == null) return;
            try {
                int v = Integer.parseInt(rotField.getText().trim());
                v = Math.max(0, Math.min(360, v));

                pushBeforeChange();
                canvas.setSelectedRotation(v);

                programmaticUpdate = true;
                rotationSlider.setValue(v);
                programmaticUpdate = false;

                recordAfterChange();
                markDirtyAndAutosave();
            } catch (Exception ignored) {}
        });

        shadingSlider.addChangeListener(e -> {
            if (isCustomer()) return;
            if (programmaticUpdate) return;
            if (canvas.getSelected() == null) return;

            if (shadingSlider.getValueIsAdjusting()) {
                if (!shadingDragging) {
                    shadingDragging = true;
                    pushBeforeChange();
                }
            } else {
                if (shadingDragging) {
                    shadingDragging = false;
                    recordAfterChange();
                }
            }

            canvas.setSelectedShading(shadingSlider.getValue());
            markDirtyAndAutosave();
        });

        deleteBtn.addActionListener(e -> {
            if (isCustomer()) return;
            if (canvas.getSelected() == null) return;
            pushBeforeChange();
            canvas.deleteSelected();
            recordAfterChange();

            updatePropertiesFromSelection();
            rebuildPlacedItemsList();
            markDirtyAndAutosave();
        });

        canvas.setOnSelectionChanged(() -> {
            updatePropertiesFromSelection();
            rebuildPlacedItemsList();
            FurnitureItem sel = canvas.getSelected();
            if (appState != null) appState.setSelectedItemId(sel == null ? null : sel.getId());
        });

        // Drag/resize start/end from canvas (admin only)
        canvas.setOnEditStart(() -> {
            if (isCustomer()) return;
            pushBeforeChange();
        });
        canvas.setOnEditCommit(() -> {
            if (isCustomer()) return;
            recordAfterChange();
            markDirtyAndAutosave();
            rebuildPlacedItemsList();
        });

        canvas.setOnDeleteRequested(() -> {
            if (isCustomer()) return;
            if (canvas.getSelected() == null) return;
            pushBeforeChange();
            canvas.deleteSelected();
            recordAfterChange();

            updatePropertiesFromSelection();
            rebuildPlacedItemsList();
            markDirtyAndAutosave();
        });
    }

    private void applyPositionFromFields() {
        if (programmaticUpdate) return;
        try {
            int x = Integer.parseInt(xField.getText().trim());
            int y = Integer.parseInt(yField.getText().trim());
            canvas.setSelectedPosition(x, y);
        } catch (Exception ignored) {}
    }

    private void applyScaleFromFields() {
        if (programmaticUpdate) return;

        FurnitureItem sel = canvas.getSelected();
        if (sel == null) return;

        try {
            int w = Integer.parseInt(wField.getText().trim());
            int h = Integer.parseInt(hField.getText().trim());

            w = Math.max(10, Math.min(1000, w));
            h = Math.max(10, Math.min(1000, h));

            if (lockAspect.isSelected()) {
                float aspect = sel.getH() == 0 ? 1f : (sel.getW() / (float) sel.getH());
                int computedH = Math.max(10, Math.round(w / aspect));
                h = computedH;
                programmaticUpdate = true;
                hField.setText(String.valueOf(h));
                programmaticUpdate = false;
            }

            canvas.setSelectedSize(w, h);
        } catch (Exception ignored) {}
    }

    private void updatePropertiesFromSelection() {
        FurnitureItem sel = canvas.getSelected();
        programmaticUpdate = true;
        try {
            if (sel == null) {
                selectedTitle.setText("No item selected");
                xField.setText("0");
                yField.setText("0");
                rotField.setText("0");
                rotationSlider.setValue(0);
                wField.setText("0");
                hField.setText("0");
                shadingSlider.setValue(50);

                deleteBtn.setEnabled(false);

                // customer shouldn't have shading tools here
                shadingToolsBtn.setEnabled(!isCustomer());

                setRightPanelEnabled(false);
                return;
            }

            selectedTitle.setText(sel.getName() + " selected");
            xField.setText(String.valueOf(sel.getX()));
            yField.setText(String.valueOf(sel.getY()));
            rotField.setText(String.valueOf(sel.getRotation()));
            rotationSlider.setValue(sel.getRotation());

            wField.setText(String.valueOf(sel.getW()));
            hField.setText(String.valueOf(sel.getH()));

            shadingSlider.setValue(sel.getShadingPercent());

            if (isCustomer()) {
                deleteBtn.setEnabled(false);
                setRightPanelEnabled(false);
            } else {
                deleteBtn.setEnabled(true);
                setRightPanelEnabled(true);
            }
        } finally {
            programmaticUpdate = false;
        }
    }

    private void setRightPanelEnabled(boolean enabled) {
        xField.setEnabled(enabled);
        yField.setEnabled(enabled);
        rotField.setEnabled(enabled);
        rotationSlider.setEnabled(enabled);
        wField.setEnabled(enabled);
        hField.setEnabled(enabled);
        lockAspect.setEnabled(enabled);
        shadingSlider.setEnabled(enabled);
        refreshLockAspectToggleVisual();
    }

    private void installApplyOnEnterOrBlur(JTextField tf, Runnable apply) {
        tf.addActionListener(e -> apply.run());
        tf.addFocusListener(new FocusAdapter() {
            @Override public void focusLost(FocusEvent e) { apply.run(); }
        });
    }

    private static int clampInt(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}