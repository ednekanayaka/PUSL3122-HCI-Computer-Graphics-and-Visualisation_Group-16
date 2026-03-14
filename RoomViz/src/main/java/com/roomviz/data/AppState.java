package com.roomviz.data;

import com.roomviz.model.Design;

public class AppState {

    private final DesignRepository repo;
    private String currentDesignId;

    /**
     * Cross-screen selection for "Selected Items" tools (e.g., shading/color).
     * Planner2D updates this when the user selects an item.
     */
    private String selectedItemId;

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

    public Design getCurrentDesignOrNull() {
        if (currentDesignId == null || currentDesignId.isBlank()) return null;
        return repo.getById(currentDesignId);
    }

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
}
