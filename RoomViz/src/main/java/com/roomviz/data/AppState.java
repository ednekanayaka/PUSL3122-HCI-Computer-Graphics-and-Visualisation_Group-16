package com.roomviz.data;

import com.roomviz.model.Design;
import com.roomviz.model.FurnitureItem;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Objects;

public class AppState {

    private final DesignRepository repo;
    private String currentDesignId;

    /**
     * Cross-screen selection for "Selected Items" tools (e.g., shading/color).
     * Planner2D updates this when the user selects an item.
     */
    private String selectedItemId;

    /* ========================= Cross-screen Undo/Redo (per selected design) ========================= */

    private String historyDesignId;
    private final Deque<List<FurnitureItem>> undoStack = new ArrayDeque<>();
    private final Deque<List<FurnitureItem>> redoStack = new ArrayDeque<>();
    private static final int MAX_HISTORY = 40;

    private boolean restoringHistory = false;

    public AppState(DesignRepository repo) {
        this.repo = repo;
    }

    public DesignRepository getRepo() {
        return repo;
    }

    public String getCurrentDesignId() {
        return currentDesignId;
    }

    public void setCurrentDesignId(String currentDesignId) {
        this.currentDesignId = currentDesignId;
    }

    /**
     * Returns the currently selected design, or null if none is selected.
     * (This does NOT create a new design.)
     */
    public Design getCurrentDesignOrNull() {
        if (currentDesignId == null || currentDesignId.isBlank()) return null;
        return repo.getById(currentDesignId);
    }

    /**
     * Alias used by screens that expect appState.getCurrentDesign().
     * Keeps behaviour "non-creating" (returns null if none selected).
     */
    public Design getCurrentDesign() {
        return getCurrentDesignOrNull();
    }

    /**
     * Convenience: returns current design; if none selected, creates a new one and selects it.
     * NOTE: Use this only in flows where auto-creating is intended.
     */
    public Design getOrCreateCurrentDesign() {
        Design d = getCurrentDesignOrNull();
        if (d != null) return d;

        Design fresh = Design.createNew("Untitled Design");
        repo.upsert(fresh);
        currentDesignId = fresh.getId();
        return fresh;
    }

    public String getSelectedItemId() {
        return selectedItemId;
    }

    public void setSelectedItemId(String selectedItemId) {
        this.selectedItemId = selectedItemId;
    }

    /* ========================= Undo/Redo API ========================= */

    /**
     * Sync undo history to the current design state.
     * - If design changed: reset history with current items.
     * - If same design but history empty: initialize.
     * - If same design but current items differ from top snapshot (e.g., edited from another screen):
     *   push current and clear redo.
     */
    public void syncHistoryForDesign(String designId, List<FurnitureItem> currentItems) {
        if (designId == null) return;

        if (!Objects.equals(historyDesignId, designId)) {
            historyDesignId = designId;
            undoStack.clear();
            redoStack.clear();
            undoStack.push(deepCopyItems(currentItems));
            trimHistory();
            return;
        }

        if (undoStack.isEmpty()) {
            undoStack.push(deepCopyItems(currentItems));
            trimHistory();
            return;
        }

        List<FurnitureItem> top = undoStack.peek();
        List<FurnitureItem> now = deepCopyItems(currentItems);
        if (!snapshotsEqual(top, now)) {
            undoStack.push(now);
            trimHistory();
            redoStack.clear();
        }
    }

    /** Push a "before change" snapshot (and clear redo). */
    public void pushBeforeChange(String designId, List<FurnitureItem> currentItems) {
        if (designId == null) return;
        if (restoringHistory) return;

        syncHistoryForDesign(designId, currentItems);

        List<FurnitureItem> snapshot = deepCopyItems(currentItems);
        List<FurnitureItem> top = undoStack.peek();
        if (top != null && snapshotsEqual(top, snapshot)) return;

        undoStack.push(snapshot);
        trimHistory();
        redoStack.clear();
    }

    /** Record an "after change" snapshot (and clear redo). */
    public void recordAfterChange(String designId, List<FurnitureItem> currentItems) {
        if (designId == null) return;
        if (restoringHistory) return;

        syncHistoryForDesign(designId, currentItems);

        List<FurnitureItem> snapshot = deepCopyItems(currentItems);
        List<FurnitureItem> top = undoStack.peek();
        if (top != null && snapshotsEqual(top, snapshot)) return;

        undoStack.push(snapshot);
        trimHistory();
        redoStack.clear();
    }

    /** Returns snapshot to restore after undo, or null if nothing to undo. */
    public List<FurnitureItem> undo(String designId, List<FurnitureItem> currentItems) {
        if (designId == null) return null;

        syncHistoryForDesign(designId, currentItems);
        if (undoStack.size() <= 1) return null;

        // current -> redo
        redoStack.push(deepCopyItems(currentItems));

        // drop current
        undoStack.pop();

        return deepCopyItems(undoStack.peek());
    }

    /** Returns snapshot to restore after redo, or null if nothing to redo. */
    public List<FurnitureItem> redo(String designId, List<FurnitureItem> currentItems) {
        if (designId == null) return null;

        syncHistoryForDesign(designId, currentItems);
        if (redoStack.isEmpty()) return null;

        List<FurnitureItem> next = deepCopyItems(redoStack.pop());
        undoStack.push(deepCopyItems(next));
        trimHistory();
        return next;
    }

    public boolean canUndo() {
        return undoStack.size() > 1;
    }

    public boolean canRedo() {
        return !redoStack.isEmpty();
    }

    public void beginHistoryRestore() {
        restoringHistory = true;
    }

    public void endHistoryRestore() {
        restoringHistory = false;
    }

    public boolean isRestoringHistory() {
        return restoringHistory;
    }

    private void trimHistory() {
        while (undoStack.size() > MAX_HISTORY) {
            undoStack.removeLast(); // remove oldest
        }
    }

    private List<FurnitureItem> deepCopyItems(List<FurnitureItem> src) {
        List<FurnitureItem> out = new ArrayList<>();
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

    private boolean snapshotsEqual(List<FurnitureItem> a, List<FurnitureItem> b) {
        if (a == b) return true;
        if (a == null || b == null) return false;
        if (a.size() != b.size()) return false;

        for (int i = 0; i < a.size(); i++) {
            FurnitureItem x = a.get(i);
            FurnitureItem y = b.get(i);
            if (x == y) continue;
            if (x == null || y == null) return false;

            if (!Objects.equals(x.getId(), y.getId())) return false;
            if (!Objects.equals(x.getName(), y.getName())) return false;
            if (!Objects.equals(String.valueOf(x.getKind()), String.valueOf(y.getKind()))) return false;
            if (!Objects.equals(x.getX(), y.getX())) return false;
            if (!Objects.equals(x.getY(), y.getY())) return false;
            if (!Objects.equals(x.getW(), y.getW())) return false;
            if (!Objects.equals(x.getH(), y.getH())) return false;
            if (!Objects.equals(x.getRotation(), y.getRotation())) return false;
            if (!Objects.equals(x.getColorHex(), y.getColorHex())) return false;
            if (!Objects.equals(x.getShadingPercent(), y.getShadingPercent())) return false;
            if (!Objects.equals(x.getMaterial(), y.getMaterial())) return false;
            if (!Objects.equals(x.getLighting(), y.getLighting())) return false;
        }
        return true;
    }
}