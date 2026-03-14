// (FULL FILE) — paste this entire file exactly as-is:
package com.roomviz.screens;

import com.roomviz.app.AppFrame;
import com.roomviz.app.Router;
import com.roomviz.app.ScreenKeys;
import com.roomviz.data.AppState;
import com.roomviz.data.SettingsRepository;
import com.roomviz.model.Design;
import com.roomviz.model.DesignStatus;
import com.roomviz.model.FurnitureItem;
import com.roomviz.model.FurnitureKind;
import com.roomviz.model.FurnitureTemplate;
import com.roomviz.ui.RoomCanvas;
import com.roomviz.ui.UiKit;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.*;

/**
 * 2D Planner page (Swing UI)
 *
 * ✅ Fixes included:
 * - canvas is created BEFORE buildFurnitureLibrary() / buildPropertiesPanel() (prevents NPE on double-click add)
 * - safe reload on navigation back to PLANNER_2D
 * - keeps undo/redo + autosave + selection wiring exactly as your logic expects
 *
 * ✅ NEW (Wiring Fix - Step 1):
 * - NO MORE silent auto-create designs.
 * - If no design is selected -> show a proper empty state:
 *   "Select or create a design first" + buttons to go Library / New Design.
 *
 * ✅ NEW (Step 1 Wiring - Furniture Library filter):
 * - Search text filters furniture templates by name
 * - Chips filter by category: All / Seating / Tables
 *
 * ✅ FIX (compile):
 * - Removes self-referencing local Runnable for chips rebuild (prevents "might not have been initialized")
 * - Uses a safe helper method instead
 */
public class Planner2DPage extends JPanel {

    private final Router router;
    private final AppState appState;
    private final SettingsRepository settingsRepo;

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

    // Debounced autosave timer
    private final javax.swing.Timer autosaveTimer;

    /* ========================= Undo/Redo (history snapshots) ========================= */

    private final java.util.Deque<java.util.List<FurnitureItem>> undoStack = new java.util.ArrayDeque<>();
    private final java.util.Deque<java.util.List<FurnitureItem>> redoStack = new java.util.ArrayDeque<>();
    private static final int MAX_HISTORY = 40;
    private boolean restoringHistory = false;

    // slider drag batching
    private boolean rotationDragging = false;
    private boolean shadingDragging = false;

    /* ========================= Furniture Library filter state ========================= */

    private enum FurnitureFilterTab { ALL, SEATING, TABLES }

    private FurnitureFilterTab activeFurnitureTab = FurnitureFilterTab.ALL;
    private JPanel chipsPanelRef; // used by rebuildFurnitureChipsUI()

    public Planner2DPage(AppFrame frame, Router router, AppState appState) {
        this(frame, router, appState, null);
    }

    public Planner2DPage(AppFrame frame, Router router, AppState appState, SettingsRepository settingsRepo) {
        this.router = router;
        this.appState = appState;
        this.settingsRepo = settingsRepo;

        setLayout(new BorderLayout());
        setBackground(UiKit.BG);
        setBorder(new EmptyBorder(16, 16, 16, 16));

        // ✅ IMPORTANT: Create canvas FIRST (buildFurnitureLibrary uses canvas)
        canvas = new RoomCanvas();

        // ===== Autosave timer =====
        autosaveTimer = new javax.swing.Timer(700, e -> {
            ((javax.swing.Timer) e.getSource()).stop();
            saveDesign("Auto-saved");
        });
        autosaveTimer.setRepeats(false);

        // ✅ Build UI + load if possible (or empty state)
        rebuildFullUI();

        // One-time listener setup to prevent cumulative listeners/crashes
        setupListeners();

        // RELOAD on navigation (e.g. coming from New Design Wizard)
        if (router != null) {
            router.addListener(key -> {
                if (ScreenKeys.PLANNER_2D.equals(key)) {
                    rebuildFullUI();
                }
            });
        }
    }

    private void setupListeners() {
        installApplyOnEnterOrBlur(xField, () -> {
            if (canvas.getSelected() == null) return;
            pushUndoSnapshot();
            applyPositionFromFields();
            markDirtyAndAutosave();
        });
        installApplyOnEnterOrBlur(yField, () -> {
            if (canvas.getSelected() == null) return;
            pushUndoSnapshot();
            applyPositionFromFields();
            markDirtyAndAutosave();
        });
        installApplyOnEnterOrBlur(rotField, () -> {
            if (canvas.getSelected() == null) return;
            try {
                int v = Integer.parseInt(rotField.getText().trim());
                pushUndoSnapshot();
                canvas.setSelectedRotation(v);
                programmaticUpdate = true;
                rotationSlider.setValue(v);
                programmaticUpdate = false;
                markDirtyAndAutosave();
            } catch (Exception ignored) {}
        });
        installApplyOnEnterOrBlur(wField, () -> {
            if (canvas.getSelected() == null) return;
            pushUndoSnapshot();
            applyScaleFromFields();
            markDirtyAndAutosave();
        });
        installApplyOnEnterOrBlur(hField, () -> {
            if (canvas.getSelected() == null) return;
            pushUndoSnapshot();
            applyScaleFromFields();
            markDirtyAndAutosave();
        });

        rotationSlider.addChangeListener(e -> {
            if (programmaticUpdate) return;
            if (canvas.getSelected() == null) return;

            if (rotationSlider.getValueIsAdjusting()) {
                if (!rotationDragging) {
                    rotationDragging = true;
                    pushUndoSnapshot();
                }
            } else {
                rotationDragging = false;
            }

            canvas.setSelectedRotation(rotationSlider.getValue());
            programmaticUpdate = true;
            rotField.setText(String.valueOf(rotationSlider.getValue()));
            programmaticUpdate = false;
            markDirtyAndAutosave();
        });

        shadingSlider.addChangeListener(e -> {
            if (programmaticUpdate) return;
            if (canvas.getSelected() == null) return;

            if (shadingSlider.getValueIsAdjusting()) {
                if (!shadingDragging) {
                    shadingDragging = true;
                    pushUndoSnapshot();
                }
            } else {
                shadingDragging = false;
            }

            canvas.setSelectedShading(shadingSlider.getValue());
            markDirtyAndAutosave();
        });

        shadingToolsBtn.addActionListener(e -> {
            if (router != null) router.show(ScreenKeys.SHADING_COLOR);
        });

        deleteBtn.addActionListener(e -> {
            if (canvas.getSelected() == null) return;
            pushUndoSnapshot();
            canvas.deleteSelected();
            updatePropertiesFromSelection();
            markDirtyAndAutosave();
        });

        canvas.setOnSelectionChanged(() -> {
            updatePropertiesFromSelection();
            FurnitureItem sel = canvas.getSelected();
            appState.setSelectedItemId(sel == null ? null : sel.getId());
        });

        canvas.setOnEditStart(this::pushUndoSnapshot);
        canvas.setOnEditCommit(this::markDirtyAndAutosave);

        canvas.setOnDeleteRequested(() -> {
            if (canvas.getSelected() == null) return;
            pushUndoSnapshot();
            canvas.deleteSelected();
            updatePropertiesFromSelection();
            markDirtyAndAutosave();
        });
    }

    /* ========================= UI REBUILD / EMPTY STATE ========================= */

    private void rebuildFullUI() {
        removeAll();

        // If no design selected -> show empty state and stop.
        Design d = appState.getCurrentDesign();
        if (d == null) {
            showNoDesignState();
            revalidate();
            repaint();
            return;
        }

        // ===== Main layout: left | center | right =====
        JPanel left = buildFurnitureLibrary();
        
        // Wrap properties in a scroll pane for small screen accessibility
        JPanel rightPanel = buildPropertiesPanel();
        JScrollPane rightScroll = new JScrollPane(rightPanel);
        rightScroll.setBorder(BorderFactory.createEmptyBorder());
        rightScroll.setOpaque(false);
        rightScroll.getViewport().setOpaque(false);
        rightScroll.getVerticalScrollBar().setUnitIncrement(14);
        rightScroll.setPreferredSize(new Dimension(280, 0));

        JPanel center = new JPanel(new BorderLayout(12, 12));
        center.setOpaque(false);

        JPanel pageHeader = buildPageHeader();
        center.add(pageHeader, BorderLayout.NORTH);

        UiKit.RoundedPanel canvasCard = new UiKit.RoundedPanel(18, UiKit.WHITE);
        canvasCard.setBorderPaint(UiKit.BORDER);
        canvasCard.setLayout(new BorderLayout());
        canvasCard.setBorder(new EmptyBorder(12, 12, 12, 12));
        canvasCard.add(canvas, BorderLayout.CENTER);
        center.add(canvasCard, BorderLayout.CENTER);

        add(left, BorderLayout.WEST);
        add(center, BorderLayout.CENTER);
        add(rightScroll, BorderLayout.EAST);

        // ✅ Load design now that UI exists
        loadDesignIntoCanvas();

        // Wire controls after canvas has items
        wireRightPanel();
        updatePropertiesFromSelection();
    }

    private void showNoDesignState() {
        JPanel wrap = new JPanel(new GridBagLayout());
        wrap.setOpaque(false);

        UiKit.RoundedPanel card = new UiKit.RoundedPanel(18, UiKit.WHITE);
        card.setBorderPaint(UiKit.BORDER);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(new EmptyBorder(18, 18, 18, 18));
        card.setPreferredSize(new Dimension(520, 220));

        JLabel title = new JLabel("Select or create a design first");
        title.setForeground(UiKit.TEXT);
        title.setFont(UiKit.scaled(title, Font.BOLD, 1.10f));
        title.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel sub = new JLabel("<html>Your planner tools work on the <b>currently selected design</b>.<br/>Go to the Design Library to pick one, or create a new design.</html>");
        sub.setForeground(UiKit.MUTED);
        sub.setFont(UiKit.scaled(sub, Font.PLAIN, 0.95f));
        sub.setBorder(new EmptyBorder(8, 0, 0, 0));
        sub.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        btnRow.setOpaque(false);
        btnRow.setBorder(new EmptyBorder(16, 0, 0, 0));
        btnRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        JButton goLibrary = UiKit.primaryButton("Go to Design Library");
        goLibrary.addActionListener(e -> {
            if (router != null) router.show(ScreenKeys.DESIGN_LIBRARY);
        });

        JButton createNew = UiKit.ghostButton("Create New Design");
        createNew.addActionListener(e -> {
            if (router != null) router.show(ScreenKeys.NEW_DESIGN);
        });

        btnRow.add(goLibrary);
        btnRow.add(createNew);

        card.add(title);
        card.add(sub);
        card.add(btnRow);

        wrap.add(card);
        add(wrap, BorderLayout.CENTER);
    }

    /* ========================= DESIGN LOAD / SAVE ========================= */

    private void loadDesignIntoCanvas() {
        Design d = appState.getCurrentDesign();
        if (d == null) return; // empty state already shown

        java.util.List<FurnitureItem> items = d.getItems();
        canvas.setItems(items);
        canvas.setRoomSpec(d.getRoomSpec());

        // Initial snapshot so first change can always be undone back to loaded state
        undoStack.clear();
        redoStack.clear();
        undoStack.push(deepCopyItems(canvas.getItems()));

        autosaved.setForeground(UiKit.MUTED);
        autosaved.setText("● Loaded");
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
        if (restoringHistory) return;

        // If no design selected, don't autosave
        if (appState.getCurrentDesign() == null) return;

        if (isAutosaveEnabled()) {
            if (autosaveTimer.isRunning()) autosaveTimer.restart();
            else autosaveTimer.start();
        }

        autosaved.setForeground(new Color(0xF59E0B));
        autosaved.setText("● Unsaved changes");
    }

    private void saveDesign(String reasonLabel) {
        Design d = appState.getCurrentDesign();
        if (d == null) {
            JOptionPane.showMessageDialog(
                    this,
                    "Select or create a design first.",
                    "No design selected",
                    JOptionPane.INFORMATION_MESSAGE
            );
            return;
        }

        d.setItems(canvas.getItems());

        Rectangle bounds = canvas.getRoomBounds();
        if (bounds != null) {
            d.setLayoutX(bounds.x);
            d.setLayoutY(bounds.y);
            d.setLayoutWidth(bounds.width);
            d.setLayoutHeight(bounds.height);
        }

        // if user has started placing/editing items, move Draft -> In Progress automatically
        if (d.getItems() != null && !d.getItems().isEmpty()) {
            if (d.getStatus() == DesignStatus.DRAFT) {
                d.setStatus(DesignStatus.IN_PROGRESS);
            }
        }

        d.touchUpdatedAtNow();
        appState.getRepo().upsert(d);

        autosaved.setForeground(new Color(0x10B981));
        autosaved.setText("● " + reasonLabel);
    }

    /* ========================= Undo/Redo helpers ========================= */

    private java.util.List<FurnitureItem> deepCopyItems(java.util.List<FurnitureItem> src) {
        java.util.List<FurnitureItem> out = new java.util.ArrayList<>();
        if (src == null) return out;

        for (FurnitureItem it : src) {
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

    private void pushUndoSnapshot() {
        if (restoringHistory) return;

        undoStack.push(deepCopyItems(canvas.getItems()));
        while (undoStack.size() > MAX_HISTORY) undoStack.removeLast();

        redoStack.clear();
    }

    private void restoreSnapshot(java.util.List<FurnitureItem> snapshot) {
        restoringHistory = true;
        try {
            canvas.setItems(deepCopyItems(snapshot));
            updatePropertiesFromSelection();
            FurnitureItem sel = canvas.getSelected();
            appState.setSelectedItemId(sel == null ? null : sel.getId());
        } finally {
            restoringHistory = false;
        }
    }

    private void doUndo() {
        if (undoStack.size() <= 1) return;

        redoStack.push(deepCopyItems(canvas.getItems()));
        undoStack.pop();
        java.util.List<FurnitureItem> prev = undoStack.peek();
        if (prev != null) restoreSnapshot(prev);

        markDirtyAndAutosave();
    }

    private void doRedo() {
        if (redoStack.isEmpty()) return;

        java.util.List<FurnitureItem> next = redoStack.pop();
        if (next == null) return;

        undoStack.push(deepCopyItems(next));
        restoreSnapshot(next);

        markDirtyAndAutosave();
    }

    /* ========================= LEFT: Furniture Library ========================= */

    // UiKit chips return JComponent, so we attach clicks using MouseListener
    private static void setChipClick(JComponent chip, Runnable onClick) {
        chip.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        chip.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { onClick.run(); }
        });
    }

    // ✅ SAFE: normal method (no self-referencing local runnable)
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
        card.setPreferredSize(new Dimension(285, 0));
        card.setLayout(new BorderLayout());
        card.setBorder(new EmptyBorder(14, 14, 14, 14));

        JLabel title = new JLabel("Furniture Library");
        title.setForeground(UiKit.TEXT);
        title.setFont(UiKit.scaled(title, Font.BOLD, 1.03f));

        JTextField search = UiKit.searchField("Search furniture...");
        search.setPreferredSize(new Dimension(0, 40));

        // ---- All templates (source of truth) - Unit: Inches ----
        final java.util.List<FurnitureTemplate> allTemplates = new java.util.ArrayList<>();
        allTemplates.add(new FurnitureTemplate("Accent Chair", "32\" × 34\"", FurnitureKind.CHAIR, 32, 34));
        allTemplates.add(new FurnitureTemplate("Dining Chair", "18\" × 22\"", FurnitureKind.CHAIR, 18, 22));
        allTemplates.add(new FurnitureTemplate("Lounge Chair", "36\" × 38\"", FurnitureKind.CHAIR, 36, 38));
        allTemplates.add(new FurnitureTemplate("Rectangular Table", "72\" × 36\"", FurnitureKind.TABLE_RECT, 72, 36));
        allTemplates.add(new FurnitureTemplate("Round Table", "48\" Ø", FurnitureKind.TABLE_ROUND, 48, 48));
        allTemplates.add(new FurnitureTemplate("Coffee Table", "48\" × 24\"", FurnitureKind.TABLE_RECT, 48, 24));
        allTemplates.add(new FurnitureTemplate("End Table", "20\" × 20\"", FurnitureKind.TABLE_RECT, 20, 20));
        allTemplates.add(new FurnitureTemplate("Console Table", "48\" × 16\"", FurnitureKind.TABLE_RECT, 48, 16));

        // ---- Visible model (filtered) ----
        DefaultListModel<FurnitureTemplate> model = new DefaultListModel<>();
        for (FurnitureTemplate t : allTemplates) model.addElement(t);

        JList<FurnitureTemplate> list = new JList<>(model);
        list.setCellRenderer(new FurnitureCell());
        list.setFixedCellHeight(56);
        list.setBorder(new EmptyBorder(10, 6, 10, 6));
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        list.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    FurnitureTemplate t = list.getSelectedValue();
                    if (t != null) {
                        pushUndoSnapshot();
                        canvas.addItemFromTemplate(t);
                        markDirtyAndAutosave();
                        canvas.requestFocusInWindow();
                    }
                }
            }
        });

        JScrollPane sc = new JScrollPane(list);
        sc.setBorder(BorderFactory.createEmptyBorder());
        sc.getVerticalScrollBar().setUnitIncrement(14);

        // ---- Chips panel ----
        JPanel chips = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        chips.setOpaque(false);

        // store ref so method can rebuild it
        chipsPanelRef = chips;

        Runnable applyFilter = () -> {
            String q = (search.getText() == null) ? "" : search.getText().trim().toLowerCase();

            model.clear();

            for (FurnitureTemplate t : allTemplates) {
                // category filter
                boolean okTab = true;
                if (activeFurnitureTab == FurnitureFilterTab.SEATING) {
                    okTab = (t.kind == FurnitureKind.CHAIR);
                } else if (activeFurnitureTab == FurnitureFilterTab.TABLES) {
                    okTab = (t.kind == FurnitureKind.TABLE_RECT || t.kind == FurnitureKind.TABLE_ROUND);
                }
                if (!okTab) continue;

                // search filter (by name)
                boolean okSearch = q.isEmpty() || (t.name != null && t.name.toLowerCase().contains(q));
                if (!okSearch) continue;

                model.addElement(t);
            }

            if (!model.isEmpty()) list.setSelectedIndex(0);
        };

        // initial render
        rebuildFurnitureChipsUI(applyFilter);

        // Live search: filter on each change
        search.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { applyFilter.run(); }
            @Override public void removeUpdate(DocumentEvent e) { applyFilter.run(); }
            @Override public void changedUpdate(DocumentEvent e) { applyFilter.run(); }
        });

        // ---- Top layout ----
        JPanel top = new JPanel();
        top.setOpaque(false);
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
        top.add(title);
        top.add(Box.createVerticalStrut(10));
        top.add(search);
        top.add(Box.createVerticalStrut(10));
        top.add(chips);

        card.add(top, BorderLayout.NORTH);
        card.add(sc, BorderLayout.CENTER);
        return card;
    }

    private static class FurnitureCell extends JPanel implements ListCellRenderer<FurnitureTemplate> {
        private final JLabel name = new JLabel();
        private final JLabel size = new JLabel();
        private final JLabel icon = new JLabel();

        FurnitureCell() {
            setLayout(new BorderLayout(10, 0));
            setOpaque(true);
            setBorder(new EmptyBorder(10, 10, 10, 10));

            icon.setPreferredSize(new Dimension(28, 28));
            icon.setHorizontalAlignment(SwingConstants.CENTER);
            icon.setFont(UiKit.scaled(icon, Font.BOLD, 1.00f));
            icon.setForeground(UiKit.PRIMARY);

            JPanel text = new JPanel();
            text.setOpaque(false);
            text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));

            name.setFont(UiKit.scaled(name, Font.BOLD, 0.97f));
            name.setForeground(UiKit.TEXT);

            size.setFont(UiKit.scaled(size, Font.PLAIN, 0.88f));
            size.setForeground(UiKit.MUTED);

            text.add(name);
            text.add(Box.createVerticalStrut(3));
            text.add(size);

            add(icon, BorderLayout.WEST);
            add(text, BorderLayout.CENTER);
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends FurnitureTemplate> list, FurnitureTemplate value,
                                                      int index, boolean isSelected, boolean cellHasFocus) {
            name.setText(value.name);
            size.setText(value.displaySize);
            icon.setText(value.kind.iconText);

            if (isSelected) {
                setBackground(new Color(0xEEF2FF));
                setBorder(BorderFactory.createCompoundBorder(
                        new LineBorder(new Color(0xC7D2FE), 1, true),
                        new EmptyBorder(10, 10, 10, 10)
                ));
            } else {
                setBackground(UiKit.WHITE);
                setBorder(new EmptyBorder(10, 10, 10, 10));
            }
            return this;
        }
    }

    /* ========================= CENTER: Page header controls ========================= */

    private JPanel buildPageHeader() {
        UiKit.RoundedPanel header = new UiKit.RoundedPanel(18, UiKit.WHITE);
        header.setBorderPaint(UiKit.BORDER);
        header.setLayout(new GridBagLayout());
        header.setBorder(new EmptyBorder(8, 16, 8, 16));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.VERTICAL;

        // Group 1: Branding & Status
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        left.setOpaque(false);

        JLabel appName = new JLabel("RoomPlan");
        appName.setFont(UiKit.scaled(appName, Font.BOLD, 0.98f));
        appName.setForeground(UiKit.PRIMARY);

        Design d = appState.getCurrentDesign();
        String name = (d == null) ? "No design selected" : d.getName();
        JLabel designLabel = new JLabel(name);
        designLabel.setFont(UiKit.scaled(designLabel, Font.PLAIN, 0.95f));
        designLabel.setForeground(UiKit.MUTED);

        autosaved.setForeground(UiKit.MUTED);
        autosaved.setFont(UiKit.scaled(autosaved, Font.PLAIN, 0.82f));

        left.add(appName);
        left.add(new JLabel("•")).setForeground(UiKit.BORDER);
        left.add(designLabel);
        left.add(Box.createHorizontalStrut(8));
        left.add(autosaved);

        gbc.gridx = 0;
        gbc.weightx = 1.0;
        gbc.anchor = GridBagConstraints.WEST;
        header.add(left, gbc);

        // Group 2: History & Actions
        JPanel center = new JPanel(new FlowLayout(FlowLayout.CENTER, 4, 0));
        center.setOpaque(false);

        JButton undo = UiKit.iconButton("↶");
        JButton redo = UiKit.iconButton("↷");
        undo.setToolTipText("Undo (Ctrl+Z)");
        redo.setToolTipText("Redo (Ctrl+Y)");
        undo.addActionListener(e -> doUndo());
        redo.addActionListener(e -> doRedo());

        JButton save = UiKit.ghostButton("Save");
        save.setFont(UiKit.scaled(save, Font.BOLD, 0.90f));
        save.addActionListener(e -> saveDesign("Saved"));

        center.add(undo);
        center.add(redo);
        center.add(Box.createHorizontalStrut(4));
        center.add(save);

        gbc.gridx = 1;
        gbc.weightx = 0;
        gbc.anchor = GridBagConstraints.CENTER;
        header.add(center, gbc);

        // Group 3: View Toggles
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        right.setOpaque(false);

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
            if (appState.getCurrentDesign() == null) return;
            saveDesign("Saved before 3D");
            if (router != null) router.show(ScreenKeys.VIEW_3D);
        });

        JButton export = UiKit.iconButton("📤");
        export.setPreferredSize(new Dimension(32, 32));
        export.setToolTipText("Export Design");
        export.addActionListener(e -> JOptionPane.showMessageDialog(this, "Export available in Settings → Account ✅"));

        right.add(toggle2d);
        right.add(toggle3d);
        right.add(export);

        gbc.gridx = 2;
        gbc.weightx = 1.0;
        gbc.anchor = GridBagConstraints.EAST;
        header.add(right, gbc);

        return header;
    }

    private void styleMiniToggle(AbstractButton b, boolean active) {
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setFont(UiKit.scaled(b, Font.BOLD, 0.92f));

        if (active) {
            b.setBackground(new Color(0xEEF2FF));
            b.setForeground(new Color(0x4338CA));
            b.setBorder(BorderFactory.createCompoundBorder(
                    new LineBorder(new Color(0xC7D2FE), 1, true),
                    new EmptyBorder(8, 12, 8, 12)
            ));
        } else {
            b.setBackground(UiKit.WHITE);
            b.setForeground(new Color(0x374151));
            b.setBorder(BorderFactory.createCompoundBorder(
                    new LineBorder(UiKit.BORDER, 1, true),
                    new EmptyBorder(8, 12, 8, 12)
            ));
        }
    }

    /* ========================= RIGHT: Properties ========================= */

    private JPanel buildPropertiesPanel() {
        UiKit.RoundedPanel card = new UiKit.RoundedPanel(18, UiKit.WHITE);
        card.setBorderPaint(UiKit.BORDER);
        card.setLayout(new BorderLayout());
        card.setBorder(new EmptyBorder(14, 14, 14, 14));

        JPanel content = new JPanel();
        content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("Properties");
        title.setFont(UiKit.scaled(title, Font.BOLD, 1.03f));
        title.setForeground(UiKit.TEXT);

        selectedTitle.setFont(UiKit.scaled(selectedTitle, Font.PLAIN, 0.90f));
        selectedTitle.setForeground(UiKit.MUTED);
        selectedTitle.setBorder(new EmptyBorder(4, 0, 0, 0));

        content.add(title);
        content.add(selectedTitle);
        content.add(Box.createVerticalStrut(14));

        content.add(sectionLabel("📐 Position"));
        content.add(twoFieldRow("X Position", xField, "Y Position", yField));
        content.add(Box.createVerticalStrut(18));

        content.add(sectionLabel("🔄 Rotation"));
        content.add(rotationRow());
        content.add(Box.createVerticalStrut(18));

        content.add(sectionLabel("📏 Dimensions"));
        content.add(twoFieldRow("Width", wField, "Height", hField));
        lockAspect.setOpaque(false);
        lockAspect.setForeground(UiKit.MUTED);
        lockAspect.setFont(UiKit.scaled(lockAspect, Font.PLAIN, 0.90f));
        lockAspect.setBorder(new EmptyBorder(6, 2, 0, 0));
        content.add(lockAspect);
        content.add(Box.createVerticalStrut(18));

        content.add(sectionLabel("🌓 Shading Intensity"));
        content.add(shadingRow());
        content.add(Box.createVerticalStrut(12));

        shadingToolsBtn.setBorder(new EmptyBorder(10, 12, 10, 12));
        shadingToolsBtn.setFont(UiKit.scaled(shadingToolsBtn, Font.BOLD, 0.92f));
        content.add(shadingToolsBtn);

        content.add(Box.createVerticalStrut(18));
        content.add(sectionLabel("🥞 Layer Order"));
        content.add(layerRow());
        content.add(Box.createVerticalStrut(16));

        deleteBtn.setForeground(UiKit.DANGER);
        deleteBtn.setBackground(UiKit.WHITE);
        deleteBtn.setFocusPainted(false);
        deleteBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        deleteBtn.setFont(UiKit.scaled(deleteBtn, Font.BOLD, 0.92f));
        deleteBtn.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(UiKit.BORDER, 1, true),
                new EmptyBorder(10, 12, 10, 12)
        ));
        content.add(deleteBtn);

        JScrollPane sc = new JScrollPane(content);
        sc.setBorder(BorderFactory.createEmptyBorder());
        sc.getVerticalScrollBar().setUnitIncrement(14);

        card.add(sc, BorderLayout.CENTER);
        return card;
    }

    private JComponent sectionLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(UiKit.scaled(l, Font.BOLD, 0.97f));
        l.setForeground(new Color(0x374151));
        l.setBorder(new EmptyBorder(0, 0, 8, 0));
        return l;
    }

    private JComponent twoFieldRow(String l1, JTextField f1, String l2, JTextField f2) {
        JPanel row = new JPanel(new GridLayout(1, 2, 10, 0));
        row.setOpaque(false);

        row.add(labeledField(l1, f1));
        row.add(labeledField(l2, f2));
        return row;
    }

    private JComponent labeledField(String label, JTextField field) {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));

        JLabel l = new JLabel(label);
        l.setFont(UiKit.scaled(l, Font.PLAIN, 0.88f));
        l.setForeground(UiKit.MUTED);

        field.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(UiKit.BORDER, 1, true),
                new EmptyBorder(8, 10, 8, 10)
        ));

        p.add(l);
        p.add(Box.createVerticalStrut(6));
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
        rotField.setPreferredSize(new Dimension(64, 34));

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        btns.setOpaque(false);

        JButton left90 = UiKit.ghostButton("↺ 90°");
        JButton right90 = UiKit.ghostButton("↻ 90°");

        left90.addActionListener(e -> {
            if (canvas.getSelected() == null) return;
            pushUndoSnapshot();
            canvas.nudgeRotation(-90);
            markDirtyAndAutosave();
        });
        right90.addActionListener(e -> {
            if (canvas.getSelected() == null) return;
            pushUndoSnapshot();
            canvas.nudgeRotation(90);
            markDirtyAndAutosave();
        });

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

        forward.addActionListener(e -> {
            if (canvas.getSelected() == null) return;
            pushUndoSnapshot();
            canvas.layerForward();
            markDirtyAndAutosave();
        });
        backward.addActionListener(e -> {
            if (canvas.getSelected() == null) return;
            pushUndoSnapshot();
            canvas.layerBackward();
            markDirtyAndAutosave();
        });
        toFront.addActionListener(e -> {
            if (canvas.getSelected() == null) return;
            pushUndoSnapshot();
            canvas.layerToFront();
            markDirtyAndAutosave();
        });
        toBack.addActionListener(e -> {
            if (canvas.getSelected() == null) return;
            pushUndoSnapshot();
            canvas.layerToBack();
            markDirtyAndAutosave();
        });

        row.add(forward);
        row.add(backward);
        row.add(toFront);
        row.add(toBack);

        return row;
    }

    /* ========================= Wiring ========================= */

    private void wireRightPanel() {
        // Redundant - listeners moved to setupListeners()
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
                shadingToolsBtn.setEnabled(true);
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
            deleteBtn.setEnabled(true);
            shadingToolsBtn.setEnabled(true);
            setRightPanelEnabled(true);
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
    }

    private void installApplyOnEnterOrBlur(JTextField tf, Runnable apply) {
        tf.addActionListener(e -> apply.run());
        tf.addFocusListener(new FocusAdapter() {
            @Override public void focusLost(FocusEvent e) { apply.run(); }
        });
    }
}
